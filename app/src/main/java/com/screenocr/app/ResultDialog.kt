package com.screenocr.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ResultDialog(
    private val context: Context,
    private val text: String
) {
    fun show() {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.result_title)
            .setMessage(text)
            .setPositiveButton(R.string.btn_copy) { _, _ ->
                copyToClipboard()
            }
            .setNegativeButton(R.string.btn_close, null)
            .show()
    }

    private fun copyToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("OCR Result", text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show()
    }
}
