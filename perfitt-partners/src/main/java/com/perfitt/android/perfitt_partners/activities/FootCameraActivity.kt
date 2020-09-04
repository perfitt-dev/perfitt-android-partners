package com.perfitt.android.perfitt_partners.activities

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.perfitt.android.perfitt_partners.R
import kotlinx.android.synthetic.main.activity_foot_camera.*

class FootCameraActivity : AppCompatActivity(), SensorEventListener {
    private val sensorManager by lazy {
        getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_foot_camera)
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
}