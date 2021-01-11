package com.perfitt.android.perfitt_partners.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.controller.APIController
import kotlinx.android.synthetic.main.activity_web_view_tutorial.*

class TutorialWebViewActivity : AppCompatActivity() {
    private var landingType = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view_tutorial)
        onNewIntent(intent)
        btn_ok.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            landingType = it.getStringExtra(LandingActivity.LANDING_TYPE) ?: ""
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
                loadUrl(APIController.TUTORIAL_URL)
            }
        }
    }

    override fun onDestroy() {
        when (landingType) {
            LandingActivity.KIT ->{
                startActivity(Intent(this, KitDetectorActivity::class.java))
            }
            LandingActivity.A4 ->{
                startActivity(Intent(this, A4DetectorActivity::class.java))
            }
        }
        super.onDestroy()
    }
}