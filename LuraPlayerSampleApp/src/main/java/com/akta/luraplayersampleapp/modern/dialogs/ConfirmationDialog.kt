package com.akta.luraplayersampleapp.modern.dialogs

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.akta.luraplayersampleapp.R


class ConfirmationDialog(
    private val context: Context,
    title: String,
    message: String,
    private val positiveListener: (() -> Unit)? = null,
    private val negativeListener: (() -> Unit)? = null,
) : Dialog(context, true, null) {

    private val titleText: TextView

    private val messageText: TextView

    private val okButton: TextView
    private val cancelButton: TextView

    init {
        setContentView(R.layout.modern_confirmation_dialog)

        window?.setBackgroundDrawable(null)

        titleText = findViewById(R.id.title)

        messageText = findViewById(R.id.message)

        okButton = findViewById(R.id.ok_button)
        cancelButton = findViewById(R.id.cancel_button)

        okButton.setOnClickListener {
            positiveListener?.invoke()
            dismiss()
        }

        cancelButton.setOnClickListener {
            negativeListener?.invoke()
            dismiss()
        }

        titleText.text = title
        messageText.text = message
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
