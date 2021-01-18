package com.perfitt.android.perfitt_partners.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.perfitt.android.perfitt_partners.activities.LandingActivity
import com.perfitt.android.perfitt_partners.listener.ConfirmListener
import com.perfitt.android.perfitt_partners.listener.NativeConfirmListener
import com.perfitt.android.perfitt_partners.utils.DialogUtil
import com.perfitt.android.perfitt_partners.utils.PerfittPartnersJSInterface

class PerfittPartners {
    var confirmListener: ConfirmListener? = null
    var nativeConfirmListener: NativeConfirmListener? = null
    var context: Context? = null

    /**
     * @param context application context
     * @param apiKey your Perfitt Partners API Key
     */
    fun initialize(context: Context, apiKey: String) {
        API_KEY = apiKey
        this.context = context
    }

    fun getJavascriptInterface(context: Context): PerfittPartnersJSInterface {
        return PerfittPartnersJSInterface(context)
    }

    fun getJavascriptInterfaceName(): String {
        return PerfittPartnersJSInterface.JAVASCRIPT_INTERFACE_NAME
    }

    fun onConfirm(listener: ConfirmListener) {
        this.confirmListener = listener
    }

    fun onNativeConfirm(listener: NativeConfirmListener) {
        this.nativeConfirmListener = listener
    }

    fun runSdk(){
        DialogUtil.instance.showSizePicker(context as Activity, "250") { size ->
            AVERAGE_SIZE = size.toInt()
            context?.startActivity(Intent(context, LandingActivity::class.java).apply {
                putExtra(LandingActivity.LANDING_TYPE, LandingActivity.KIT)
            })
        }
    }

    private object Holder {
        val INSTANCE = PerfittPartners()
    }

    companion object {
        val instance: PerfittPartners by lazy { Holder.INSTANCE }
        var API_KEY = ""
        var CUSTOMER_ID = ""
        var AVERAGE_SIZE = 0
    }
}