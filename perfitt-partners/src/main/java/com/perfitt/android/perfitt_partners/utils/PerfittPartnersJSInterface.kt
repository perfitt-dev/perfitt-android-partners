package com.perfitt.android.perfitt_partners.utils

import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import com.perfitt.android.perfitt_partners.activities.LandingActivity

class PerfittPartnersJSInterface(private val context: Context) {

    @JavascriptInterface
    fun runSDK(apiKey: String) {
        context.startActivity(Intent(context, LandingActivity::class.java).apply {
            putExtra(LandingActivity.LANDING_TYPE, LandingActivity.CAMERA)
            putExtra(LandingActivity.API_KEY, apiKey)
        })
    }

    companion object {
        const val JAVASCRIPT_INTERFACE_NAME = "PERFITT_SDK"
    }
}