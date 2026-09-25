package com.tinyai.geekbox.feature.ai

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object BrowserLauncher {

    fun openTab(context: Context, url: String) {
        val uri = runCatching { Uri.parse(url) }.getOrNull() ?: return
        val normalized = if (uri.scheme == null) Uri.parse("https://$url") else uri
        val intent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(true)
            .build()
        val ok = runCatching { intent.launchUrl(context, normalized) }.isSuccess
        if (!ok) {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, normalized).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}
