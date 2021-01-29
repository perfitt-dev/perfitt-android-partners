package com.perfitt.android.perfitt_partners.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.utils.PermissionUtil
import com.perfitt.android.perfitt_partners.utils.PreferenceUtil

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sdk_activity_landing)
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            val pref = PreferenceUtil.instance(this)
            when (it.getStringExtra(LANDING_TYPE)) {
                A4 -> {
                    PermissionUtil.instance
                        .setMessage(getString(R.string.sdk_msg_permission_camera))
                        .setPermissions(arrayListOf(Manifest.permission.CAMERA))
                        .onGranted {
                            startActivity(Intent(this, TutorialWebViewActivity::class.java).apply {
                                putExtra(LandingActivity.LANDING_TYPE, LandingActivity.A4)
                            })
                            finish()
                            pref.setFirstAppTutorial(true)
                        }
                        .onDenied {
                            finish()
                        }
                        .onSystemPermissionSettings {
                            finish()
                        }
                        .checkToPermissions(this as AppCompatActivity)
                }
                KIT -> {
                    PermissionUtil.instance
                        .setMessage(getString(R.string.sdk_msg_permission_camera))
                        .setPermissions(arrayListOf(Manifest.permission.CAMERA))
                        .onGranted {
                            startActivity(Intent(this, TutorialWebViewActivity::class.java).apply {
                                putExtra(LandingActivity.LANDING_TYPE, LandingActivity.KIT)
                            })
                            finish()
                            pref.setFirstAppTutorial(true)
                        }
                        .onDenied {
                            finish()
                        }
                        .onSystemPermissionSettings {
                            finish()
                        }
                        .checkToPermissions(this as AppCompatActivity)
                }
            }
        } ?: run {
            Handler().postDelayed(Runnable {
                finish()
            }, 3000)
        }
    }

    /**
     * 퍼미션 설정 체크
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtil.REQUEST_CODE_PERMISSION) {
            PermissionUtil.instance.deniedPermissionSettings(this, permissions)
        }
    }

    companion object {
        const val LANDING_TYPE = "landing_type"
        const val PARAMS = "params"
        const val A4 = "a4"
        const val KIT = "kit"
    }
}