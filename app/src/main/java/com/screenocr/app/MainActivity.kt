package com.screenocr.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.graphics.Rect
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.screenocr.app.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: SharedPreferences

    private var captureService: ScreenCaptureService? = null
    private var isBound = false
    private var windowManager: WindowManager? = null
    private var overlayView: OverlayView? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as ScreenCaptureService.LocalBinder
            captureService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            captureService = null
            isBound = false
        }
    }

    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            captureService?.startProjection(result.resultCode, result.data!!)
            // Small delay to ensure projection is ready
            binding.root.postDelayed({
                showOverlay()
            }, 300)
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            requestMediaProjection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(SettingsActivity.PREFS_NAME, Context.MODE_PRIVATE)

        setupListeners()
    }

    override fun onStart() {
        super.onStart()
        // Start and bind to service
        val serviceIntent = Intent(this, ScreenCaptureService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
    }

    private fun setupListeners() {
        binding.btnStartOcr.setOnClickListener {
            startOcrFlow()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun startOcrFlow() {
        // Check API configuration
        val baseUrl = prefs.getString(SettingsActivity.KEY_API_BASE_URL, "")
        val apiKey = prefs.getString(SettingsActivity.KEY_API_KEY, "")

        if (baseUrl.isNullOrBlank() || apiKey.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_no_api_config, Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            showOverlayPermissionDialog()
            return
        }

        requestMediaProjection()
    }

    private fun showOverlayPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_overlay_title)
            .setMessage(R.string.permission_overlay_message)
            .setPositiveButton(R.string.btn_go_settings) { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                overlayPermissionLauncher.launch(intent)
            }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }

    private fun requestMediaProjection() {
        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun showOverlay() {
        // Minimize the app first
        moveTaskToBack(true)

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        )

        overlayView = OverlayView(this).apply {
            onRegionSelected = { rect ->
                removeOverlay()
                captureAndRecognize(rect)
            }
        }

        windowManager?.addView(overlayView, layoutParams)
    }

    private fun removeOverlay() {
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                // View might already be removed
            }
            overlayView = null
        }
    }

    private fun captureAndRecognize(rect: Rect) {
        // Bring app back to foreground
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)

        showLoading(true)

        // Small delay to ensure overlay is removed before capture
        binding.root.postDelayed({
            val bitmap = captureService?.captureRegion(rect)

            if (bitmap == null) {
                showLoading(false)
                Toast.makeText(this, R.string.error_capture_failed, Toast.LENGTH_SHORT).show()
                return@postDelayed
            }

            performOcr(bitmap, rect)
        }, 100)
    }

    private fun performOcr(bitmap: android.graphics.Bitmap, rect: Rect) {
        val baseUrl = prefs.getString(SettingsActivity.KEY_API_BASE_URL, "") ?: ""
        val apiKey = prefs.getString(SettingsActivity.KEY_API_KEY, "") ?: ""
        val model = prefs.getString(SettingsActivity.KEY_MODEL, SettingsActivity.DEFAULT_MODEL) ?: SettingsActivity.DEFAULT_MODEL

        val client = OcrApiClient(baseUrl, apiKey, model)

        lifecycleScope.launch {
            val result = client.recognizeText(bitmap)

            // Recycle bitmap after use
            bitmap.recycle()

            showLoading(false)

            result.fold(
                onSuccess = { text ->
                    ResultDialog(this@MainActivity, text).show()
                },
                onFailure = { error ->
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.error_ocr_failed, error.message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.tvLoading.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnStartOcr.isEnabled = !show
        binding.btnSettings.isEnabled = !show
    }
}
