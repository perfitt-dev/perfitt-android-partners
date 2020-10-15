package com.perfitt.android.perfitt_partners.activities

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.controller.APIController
import com.perfitt.android.perfitt_partners.controller.PerfittPartners
import com.perfitt.android.perfitt_partners.utils.DialogUtil
import com.perfitt.android.perfitt_partners.utils.FileUtil
import kotlinx.android.synthetic.main.activity_foot_camera_confirm.*
import org.json.JSONObject
import java.io.File


class FootCameraConfirmActivity : AppCompatActivity() {
    var type = 0
    var progressDialog: AlertDialog? = null
    var currentZoom: Int = 0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foot_camera_confirm)
        onNewIntent(intent)
        supportActionBar?.setTitle(if (type == DetectorActivity.TYPE_FOOT_RIGHT) R.string.activity_foot_camera_title_right_confirm else R.string.activity_foot_camera_title_left_confirm)
        btn_next.setOnClickListener {
            if (type == DetectorActivity.TYPE_FOOT_RIGHT) {
                startActivity(Intent(this, DetectorActivity::class.java).apply {
                    putExtra("type", DetectorActivity.TYPE_FOOT_LEFT)
                    putExtra("currentZoom", currentZoom)
                })
                finish()
            } else {
                progressDialog = DialogUtil.instance.showProgressMessage(this@FootCameraConfirmActivity, getString(R.string.term_progress_size))
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
                    val handler = Handler()
                    Thread(Runnable {
                        val sourceType = packageManager.getPackageInfo(packageName, 0).versionName + "_" + Build.MODEL + "_android-" + Build.VERSION.SDK_INT
                        val response = APIController.instance.requestUsers(PerfittPartners.instance.apiKey, leftData, rightData, sourceType) { errorCode, errorMessage ->
                            handler.post {
                                progressDialog?.dismiss()
                                Toast.makeText(this@FootCameraConfirmActivity, "errorCode:$errorCode \n errorMessage$errorMessage", Toast.LENGTH_SHORT).show()
                            }
                        }

                        response?.let { confirm ->
                            handler.post {
                                progressDialog?.dismiss()
                                JSONObject(confirm).getString("id").let { id ->
                                    PerfittPartners.instance.confirmListener?.onConfirm("javascript:callback('$id')")
                                    finish()
                                }
                            }
                        }

                    }).start()
                }
            }
        }

        btn_retake.setOnClickListener {
            startActivity(Intent(this, DetectorActivity::class.java).apply {
                putExtra("type", this@FootCameraConfirmActivity.type)
                putExtra("currentZoom", currentZoom)
            })
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
        }
    }
}
