package com.akta.luraplayersampleapp.modern.dialogs

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import com.akta.luraplayersampleapp.R


class LuraAlertDialog(
    private val context: Context,
    title: String,
    message: String,
    private val positiveListener: (() -> Unit)? = null,
) : Dialog(context, true, null) {

    private val titleView: TextView
    private val messageView: TextView

    private val copyButton: TextView
    private val okButton: TextView

    init {
        setContentView(R.layout.modern_alert_dialog)

        window?.setBackgroundDrawable(null)

        titleView = findViewById(R.id.title)
        messageView = findViewById(R.id.message)

        copyButton = findViewById(R.id.copy_button)
        okButton = findViewById(R.id.ok_button)

        titleView.text = title
        messageView.text = message

        copyButton.setOnClickListener {
            val clipboard: ClipboardManager? =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clip = ClipData.newPlainText("Message", message)
            clipboard?.setPrimaryClip(clip)

            dismiss()
        }

        okButton.setOnClickListener {
            positiveListener?.invoke()
            dismiss()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val params = WindowManager.LayoutParams()
        params.copyFrom(window?.attributes)
        params.width = context.resources.displayMetrics.widthPixels - 200
        params.height = WindowManager.LayoutParams.WRAP_CONTENT

        window?.attributes = params
    }
}
