package com.perfitt.android.perfitt_partners.activities

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import com.perfitt.android.perfitt_partners.utils.DialogUtil
import com.perfitt.android.perfitt_partners.utils.FileUtil
import com.perfitt.android.perfitt_partners.utils.PreferenceUtil
import kotlinx.android.synthetic.main.activity_foot_camera.*
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class FootCameraActivity : AppCompatActivity(), SensorEventListener {
    private var type = 0
    private var fileName = ""

    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foot_camera)
        onNewIntent(intent)
        supportActionBar?.setTitle(if (type == TYPE_FOOT_RIGHT) R.string.activity_foot_camera_title_right else R.string.activity_foot_camera_title_left)

        PreferenceUtil.instance(this).run {
            if (!isFirstAppTutorial) {
                startActivity(Intent(this@FootCameraActivity, TutorialWebViewActivity::class.java))
                setFirstAppTutorial(true)
            } else {
                DialogUtil.instance.showMessageDialog(
                    this@FootCameraActivity,
                    "",
                    if (type == TYPE_FOOT_RIGHT) getString(R.string.activity_foot_camera_title_right_message) else getString(R.string.activity_foot_camera_title_left_message)
                )
            }
        }

        btn_zoom_in.setOnClickListener {
            camera_preview.zoomIn()
        }

        btn_zoom_out.setOnClickListener {
            camera_preview.zoomOut()
        }

        btn_camera.setOnClickListener {
            progress_bar.visibility = View.VISIBLE
            unregisterListener()
            camera_preview.takePhoto { data, _ ->
                // 640 * 480 가로길이가 더 김.
                BitmapFactory.decodeByteArray(data, 0, data.size).let { bitmap ->
                    val rotateBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply {
                        postRotate(180f)
                    }, false)
                    saveBitmapToJpeg(rotateBitmap)
                    startActivity(Intent(applicationContext, FootCameraConfirmActivity::class.java).apply {
                        putExtra("currentZoom", camera_preview.currentZoom)
                        putExtra("type", this@FootCameraActivity.type)
                        putExtra("fileName", fileName)
                    })
                    finish()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        type = intent?.getIntExtra("type", TYPE_FOOT_RIGHT) ?: TYPE_FOOT_RIGHT
    }

    override fun onResume() {
        super.onResume()
        camera_preview.run camera@{
            holder.run {
                addCallback(this@camera)
            }
        }
        registerListener()
    }

    override fun onPause() {
        super.onPause()
        unregisterListener()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_foot_camera, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_tutorial -> {
                startActivity(Intent(this, TutorialWebViewActivity::class.java).apply {
                    putExtra("uri", "https://www.perfitt.io/")
                })
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun registerListener() {
        btn_camera.isEnabled = true
        camera_preview.startPreview()
        sensorManager.registerListener(
            this,
            //센서 매니저에 센서 등록(센서 받을 액티비티, 센서 종류, 센서 빈도)
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), // 기울기 센서
            SensorManager.SENSOR_DELAY_GAME
        )
    }

    private fun unregisterListener() {
        btn_camera.isEnabled = false
        sensorManager.unregisterListener(this)
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        // ${event.values[0]} : x축 값 : 위로 기울이면 -10~0, 아래로 기울이면 0~10
        // ${event.values[1]} : y축 값 : 왼쪽으로 기울이면 -10~0, 오른쪽으로 기울이면 0~10
        // ${event.values[2]} : z축 값 : 미사용
        event?.run {
            val x = event.values[0]
            val y = event.values[1]
            val xCoord = x * 20
            val yCoord = y * 20

            img_circle.x = btn_camera.x + xCoord
            img_circle.y = btn_camera.y + yCoord

            if (x < 1.5 && x > -1.5 && y < 1.5 && y > -1.5) {
                img_camera_disable.visibility = View.INVISIBLE
                btn_camera.visibility = View.VISIBLE
                btn_camera.isClickable = true
                layout_empty.visibility = View.INVISIBLE
            } else {
                img_camera_disable.visibility = View.VISIBLE
                btn_camera.visibility = View.INVISIBLE
                btn_camera.isClickable = false
                layout_empty.visibility = View.VISIBLE
            }
        }
    }

    private fun saveBitmapToJpeg(bitmap: Bitmap): File {
        fileName = when (type) {
            TYPE_FOOT_RIGHT -> FileUtil.FILE_NAME_FOOT_RIGHT
            TYPE_FOOT_LEFT -> FileUtil.FILE_NAME_FOOT_LEFT
            else -> ""
        }

        val storage = FileUtil.instance.getFootFilePath(this).path
        val tempFile = File(storage, fileName)
        try {
            tempFile.createNewFile()
            val out = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return tempFile
    }

    companion object {
        const val TYPE_FOOT_RIGHT = 1001
        const val TYPE_FOOT_LEFT = 1002
    }
}