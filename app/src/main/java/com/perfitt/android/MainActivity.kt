package com.perfitt.android

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.*
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
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
            }
            webChromeClient = object : WebChromeClient() {
                override fun onPermissionRequest(request: PermissionRequest?) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        request?.grant(request.resources)
                    }
//                    super.onPermissionRequest(request)
                }


                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                    val newWebView = WebView(context)
                    val webSettings = newWebView.settings
                    webSettings.javaScriptEnabled = true
                    val dialog = Dialog(context)
                    dialog.setContentView(newWebView)
                    resultMsg?.obj
                    val params: ViewGroup.LayoutParams = dialog.window!!.attributes
                    params.width = ViewGroup.LayoutParams.MATCH_PARENT
                    params.height = ViewGroup.LayoutParams.MATCH_PARENT
                    dialog.window?.attributes = params as WindowManager.LayoutParams
                    dialog.show()

                    val transform = (resultMsg?.obj as WebView.WebViewTransport);
                    transform.webView = newWebView
                    resultMsg.sendToTarget()

                    return true
                }
            }
            // sdk code
            addJavascriptInterface(PerfittPartners.instance.getJavascriptInterface(this@MainActivity), PerfittPartners.instance.getJavascriptInterfaceName())
//            loadUrl("https://perfitt-static-files.s3.ap-northeast-2.amazonaws.com/resources/clutter/test.html")
//            loadUrl("https://s3.ap-northeast-2.amazonaws.com/dev-sdk.perfitt.io/sibule.html?apiKey=undefined&page=http://m.sgumg.cafe24.com/product/%EB%A3%A8%EC%9D%B4%EB%B8%8C-%EC%8A%A4%ED%8B%B8%EB%A0%88%ED%86%A0-%EB%AE%AC-%EC%8A%AC%EB%A6%AC%ED%8D%BC-75cm/14/category/1/display/2/&accessKey=PARTNERS_TEST_KEY")
//            loadUrl("http://m.sgumg.cafe24.com/")
//            loadUrl("http://yjperfitt.www260.freesell.co.kr/m/")
            loadUrl("https://perfitt-static-files.s3.ap-northeast-2.amazonaws.com/SOLUTION_STATICS/gf.html")
        }
        PerfittPartners.instance.initialize(this, "PARTNERS_TEST_KEY")
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
    }

    override fun onBackPressed() {
//        super.onBackPressed()
        web_view.goBack()
    }
}