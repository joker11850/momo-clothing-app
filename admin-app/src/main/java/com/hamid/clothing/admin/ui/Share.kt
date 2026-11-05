package com.hamid.clothing.admin.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast

fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

fun shareText(context: Context, text: String) {
    try {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, "ارسال").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(context, "اشتراک‌گذاری ممکن نشد")
    }
}

fun shareFile(context: Context, file: java.io.File, mime: String = "application/zip", title: String = "ارسال") {
    try {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(mime)
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: Exception) {
        toast(context, "ارسال فایل ممکن نشد")
    }
}
