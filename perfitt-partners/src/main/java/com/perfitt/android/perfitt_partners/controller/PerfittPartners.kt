package com.perfitt.android.perfitt_partners.controller

import android.content.Context
import com.perfitt.android.perfitt_partners.listener.ConfirmListener
import com.perfitt.android.perfitt_partners.utils.PerfittPartnersJSInterface

class PerfittPartners {
    var confirmListener: ConfirmListener? = null
    var apiKey = ""
    var context: Context? = null

    /**
     * @param context application context
     * @param context your Perfitt Partners API Key
     */
    fun initialize(context: Context, apiKey: String) {
        this.apiKey = apiKey
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

    private object Holder {
        val INSTANCE = PerfittPartners()
    }

    companion object {
        val instance: PerfittPartners by lazy { Holder.INSTANCE }
    }
}