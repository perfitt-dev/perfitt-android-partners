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
import kotlinx.android.synthetic.main.sdk_activity_web_view_tutorial.*

class TutorialWebViewActivity : AppCompatActivity() {
    private var landingType = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sdk_activity_web_view_tutorial)
        onNewIntent(intent)
//        supportActionBar?.title = getString(R.string.sdk_term_measure_guide)
        supportActionBar?.hide()
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
                        url?.let {
                            if (url.contains("/app/guides/start")) {
                                finish()
                            }
                        }
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
            LandingActivity.KIT -> {
                startActivity(Intent(this, SDKKitDetectorActivity::class.java))
            }
            LandingActivity.A4 -> {
                startActivity(Intent(this, SDKA4DetectorActivity::class.java))
            }
        }
        super.onDestroy()
    }
}