package com.perfitt.android

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.controller.PerfittPartners
import com.perfitt.android.perfitt_partners.listener.ConfirmListener
import com.perfitt.android.perfitt_partners.listener.NativeConfirmListener
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WebView.setWebContentsDebuggingEnabled(true)
        web_view.run {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    progress_bar.visibility = View.VISIBLE
                    return super.shouldOverrideUrlLoading(view, request)
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    progress_bar.visibility = View.INVISIBLE
                }
            }

            settings.run {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            webChromeClient = WebChromeClient()
            // sdk code
            addJavascriptInterface(PerfittPartners.instance.getJavascriptInterface(this@MainActivity), PerfittPartners.instance.getJavascriptInterfaceName())
//            loadUrl("https://perfitt-static-files.s3.ap-northeast-2.amazonaws.com/resources/clutter/test.html")
            loadUrl("http://m.sgumg.cafe24.com/")
        }
        PerfittPartners.instance.initialize(this, "PERFITT_TEST_KEY")
        PerfittPartners.instance.onConfirm(object : ConfirmListener {
            override fun onConfirm(url: String) {
                Log.d("Dony", "url:$url")
                web_view.loadUrl(url)
            }
        })
        PerfittPartners.instance.onNativeConfirm(object : NativeConfirmListener {
            override fun onConfirm(feetId: String) {
                Log.d("Dony", "feetId:$feetId")
            }
        })
        btn_native.setOnClickListener {
            PerfittPartners.instance.runSdk(this, "")
        }
    }
}