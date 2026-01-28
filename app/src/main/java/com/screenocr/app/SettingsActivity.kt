package com.screenocr.app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.screenocr.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    companion object {
        const val PREFS_NAME = "screenocr_prefs"
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val DEFAULT_MODEL = "gpt-4o-mini"
    }

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        loadSettings()
        setupListeners()
    }

    private fun loadSettings() {
        binding.etApiBaseUrl.setText(prefs.getString(KEY_API_BASE_URL, ""))
        binding.etApiKey.setText(prefs.getString(KEY_API_KEY, ""))
        binding.etModel.setText(prefs.getString(KEY_MODEL, DEFAULT_MODEL))
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun saveSettings() {
        val baseUrl = binding.etApiBaseUrl.text.toString().trim()
        val apiKey = binding.etApiKey.text.toString().trim()
        var model = binding.etModel.text.toString().trim()

        // Remove trailing slash from base URL
        val cleanBaseUrl = baseUrl.trimEnd('/')

        // Use default model if empty
        if (model.isBlank()) {
            model = DEFAULT_MODEL
        }

        prefs.edit()
            .putString(KEY_API_BASE_URL, cleanBaseUrl)
            .putString(KEY_API_KEY, apiKey)
            .putString(KEY_MODEL, model)
            .apply()

        Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
