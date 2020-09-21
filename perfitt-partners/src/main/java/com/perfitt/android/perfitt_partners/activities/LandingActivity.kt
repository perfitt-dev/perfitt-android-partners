package com.perfitt.android.perfitt_partners.activities

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.controller.PerfittPartners
import com.perfitt.android.perfitt_partners.utils.PermissionUtil

class LandingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)
        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            when (it.getStringExtra(LANDING_TYPE)) {
                CAMERA -> {
                    PermissionUtil.instance
                        .setMessage(getString(R.string.msg_permission_camera))
                        .setPermissions(arrayListOf(Manifest.permission.CAMERA))
                        .onGranted {
                            startActivity(Intent(this, FootCameraActivity::class.java))
                            finish()
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
            it.getStringExtra(API_KEY)?.let { apiKey ->
                PerfittPartners.instance.apiKey = apiKey
            }
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
        const val CAMERA = "camera"
        const val API_KEY = "api_key"
    }
}