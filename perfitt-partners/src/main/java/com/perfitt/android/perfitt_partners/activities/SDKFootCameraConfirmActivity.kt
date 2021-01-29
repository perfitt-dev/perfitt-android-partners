package com.perfitt.android.perfitt_partners.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.controller.APIController
import com.perfitt.android.perfitt_partners.controller.PerfittPartners
import com.perfitt.android.perfitt_partners.utils.DialogSDKUtil
import com.perfitt.android.perfitt_partners.utils.FileUtil
import com.perfitt.android.perfitt_partners.utils.PoolUtils
import kotlinx.android.synthetic.main.sdk_activity_foot_camera_confirm.*
import java.io.File


class SDKFootCameraConfirmActivity : AppCompatActivity() {
    var parentType = ""
    var type = 0
    var progressDialog: AlertDialog? = null
    var currentZoom: Int = 0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sdk_activity_foot_camera_confirm)
        onNewIntent(intent)
//        supportActionBar?.setTitle(if (type == A4DetectorActivity.TYPE_FOOT_RIGHT) R.string.activity_foot_camera_title_right_confirm else R.string.activity_foot_camera_title_left_confirm)
        supportActionBar?.hide()
        btn_next.setOnClickListener {
            if (type == SDKA4DetectorActivity.TYPE_FOOT_RIGHT) {
                if (parentType == LandingActivity.A4) {
                    startActivity(Intent(this, SDKA4DetectorActivity::class.java).apply {
                        putExtra("type", SDKA4DetectorActivity.TYPE_FOOT_LEFT)
                        putExtra("currentZoom", currentZoom)
                    })
                } else {
                    startActivity(Intent(this, SDKKitDetectorActivity::class.java).apply {
                        putExtra("type", SDKKitDetectorActivity.TYPE_FOOT_LEFT)
                        putExtra("currentZoom", currentZoom)
                    })
                }
                finish()
            } else {
                runOnUiThread {
                    progressDialog = DialogSDKUtil.INSTANCE.showProgressMessage(this@SDKFootCameraConfirmActivity, getString(R.string.sdk_term_progress_size))
                }
                var rightData = ""
                var leftData = ""
                FileUtil.instance.getFootFilePath(this).let {
                    it.path.let { path ->
                        File("$path/${FileUtil.FILE_NAME_FOOT_RIGHT}").let { rightFoot ->
                            Base64.encodeToString(rightFoot.readBytes(), Base64.DEFAULT).replace("\\n".toRegex(), "").let { base64 ->
                                rightData = base64
                            }
                        }
                        File("$path/${FileUtil.FILE_NAME_FOOT_LEFT}").let { leftFoot ->
                            Base64.encodeToString(leftFoot.readBytes(), Base64.DEFAULT).replace("\\n".toRegex(), "").let { base64 ->
                                leftData = base64
                            }
                        }
                    }
                    Thread(Runnable {
                        val sourceType = packageManager.getPackageInfo(packageName, 0).versionName + "_" + Build.MODEL + "_android-" + Build.VERSION.SDK_INT
                        val response = if (parentType == LandingActivity.A4) {
                            APIController.instance.requestUsersA4(PerfittPartners.API_KEY, leftData, rightData, sourceType) { errorCode, errorType, errorMessage ->
                                runOnUiThread {
                                    progressDialog?.dismiss()
                                    DialogSDKUtil.INSTANCE.showMessageDialogCustomText(this,
                                        getString(R.string.sdk_msg_dialog_foot_measure_error_title),
                                        getString(R.string.sdk_msg_dialog_foot_measure_error_message),
                                        getString(R.string.sdk_msg_dialog_foot_measure_error_button), "", {
                                            startActivity(Intent(this, SDKKitDetectorActivity::class.java).apply {
                                                putExtra("type", SDKCameraActivity.TYPE_FOOT_RIGHT)
                                                putExtra("currentZoom", currentZoom)
                                            })
                                            finish()
                                        })
                                }
                            }
                        } else {
                            APIController.instance.requestUsersKit(
                                PerfittPartners.API_KEY,
                                leftData,
                                rightData,
                                sourceType,
                                PoolUtils.instance.leftFoot,
                                PoolUtils.instance.rightFoot
                            ) { errorCode, errorType, errorMessage ->
                                runOnUiThread {
                                    progressDialog?.dismiss()
                                    DialogSDKUtil.INSTANCE.showMessageDialogCustomText(this,
                                        getString(R.string.sdk_msg_dialog_foot_measure_error_title),
                                        getString(R.string.sdk_msg_dialog_foot_measure_error_message),
                                        getString(R.string.sdk_msg_dialog_foot_measure_error_button), "", {
                                            startActivity(Intent(this, SDKKitDetectorActivity::class.java).apply {
                                                putExtra("type", SDKCameraActivity.TYPE_FOOT_RIGHT)
                                                putExtra("currentZoom", currentZoom)
                                            })
                                            finish()
                                        })
                                }
                            }
                        }
                        response?.let { response ->
                            runOnUiThread {
                                Log.d("Dony", "response:$response")
                                progressDialog?.dismiss()
                                startActivity(Intent(this, FootResultActivity::class.java).apply {
                                    putExtra("response", response)
                                    putExtra("parentType", parentType)
                                    putExtra("currentZoom", currentZoom)
                                    putExtra("type", type)
                                })
                                finish()
                            }
                        }
                    }).start()
                }
            }
        }

        btn_retake.setOnClickListener {
            if (parentType == LandingActivity.A4) {
                startActivity(Intent(this, SDKA4DetectorActivity::class.java).apply {
                    putExtra("type", this@SDKFootCameraConfirmActivity.type)
                    putExtra("currentZoom", currentZoom)
                })
            } else {
                startActivity(Intent(this, SDKKitDetectorActivity::class.java).apply {
                    putExtra("type", this@SDKFootCameraConfirmActivity.type)
                    putExtra("currentZoom", currentZoom)
                })
            }
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            it.getStringExtra("fileName").let { fileName ->
                (FileUtil.instance.getFootFilePath(this).path + "/$fileName").let { path ->
                    BitmapFactory.decodeFile(path, BitmapFactory.Options()).let { bitmap ->
                        val rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply {
//                            postRotate(-90f)
                        }, false)
                        img_preview.setImageBitmap(rotateBitmap)
                    }
                }
            }
            it.getIntExtra("currentZoom", 0).let { zoom ->
                currentZoom = zoom
            }

            type = it.getIntExtra("type", 0)
            parentType = it.getStringExtra("parentType")
        }
    }
}
