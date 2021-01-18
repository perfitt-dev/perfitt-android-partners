package com.perfitt.android.perfitt_partners.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import com.perfitt.android.perfitt_partners.activities.LandingActivity
import com.perfitt.android.perfitt_partners.controller.PerfittPartners

class PerfittPartnersJSInterface(private val context: Context) {

    @JavascriptInterface
    fun runSDK(apiKey: String) {
        context.startActivity(Intent(context, LandingActivity::class.java).apply {
            //TODO Test
//            putExtra(LandingActivity.LANDING_TYPE, LandingActivity.A4)
//            putExtra(LandingActivity.LANDING_TYPE, LandingActivity.KIT)
        })
    }

    @JavascriptInterface
    fun runSDKkit(customerId: String?) {
        customerId?.let {
            PerfittPartners.CUSTOMER_ID = it
        }
        DialogSDKUtil.INSTANCE.showSizePicker(context as Activity, "250") { size ->
            PerfittPartners.AVERAGE_SIZE = size.toInt()
            context.startActivity(Intent(context, LandingActivity::class.java).apply {
                putExtra(LandingActivity.LANDING_TYPE, LandingActivity.KIT)
            })
        }
    }

    @JavascriptInterface
    fun runSDKa4(customerId: String?) {
        customerId?.let {
            PerfittPartners.CUSTOMER_ID = it
        }
        DialogSDKUtil.INSTANCE.showSizePicker(context as Activity, "250") { size ->
            PerfittPartners.AVERAGE_SIZE = size.toInt()
            context.startActivity(Intent(context, LandingActivity::class.java).apply {
                putExtra(LandingActivity.LANDING_TYPE, LandingActivity.A4)
            })
        }
    }

    companion object {
        const val JAVASCRIPT_INTERFACE_NAME = "PERFITT_SDK"
    }
}