package com.screenocr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ResultDialog(
    private val context: Context,
    private val text: String,
    private val isError: Boolean = false
) {
    fun show() {
        val title = if (isError) R.string.error_title else R.string.result_title

        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton(R.string.btn_copy) { _, _ ->
                copyToClipboard()
            }
            .setNegativeButton(R.string.btn_close, null)
            .show()
    }

    private fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(if (isError) "Error" else "OCR Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }
}
