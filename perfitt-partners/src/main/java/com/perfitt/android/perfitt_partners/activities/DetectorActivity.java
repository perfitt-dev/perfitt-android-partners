/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perfitt.android.perfitt_partners.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.perfitt.android.perfitt_partners.R;
import com.perfitt.android.perfitt_partners.tflite.Classifier;
import com.perfitt.android.perfitt_partners.tflite.TFLiteObjectDetectionAPIModel;
import com.perfitt.android.perfitt_partners.tracking.MultiBoxTracker;
import com.perfitt.android.perfitt_partners.utils.DialogUtil;
import com.perfitt.android.perfitt_partners.utils.ImageUtils;
import com.perfitt.android.perfitt_partners.utils.Logger;
import com.perfitt.android.perfitt_partners.utils.PreferenceUtil;
import com.perfitt.android.perfitt_partners.views.BorderedText;
import com.perfitt.android.perfitt_partners.views.OverlayView;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener, SensorEventListener {
    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 320;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "model.tflite";
    private static final String TF_OD_API_LABELS_FILE = "dict.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.95f;
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 720);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    private AppCompatImageView img_circle, img_camera_disable, guide_test;
    private ConstraintLayout layout_empty;

    private SensorManager sensorManager;
    private boolean isSensor = false, isFoot = false, isValidation = false;

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onNewIntent(getIntent());
        if (getSupportActionBar() != null) {
            int titleRes;
            if (viewType == TYPE_FOOT_RIGHT) {
                titleRes = R.string.activity_foot_camera_title_right;
            } else {
                titleRes = R.string.activity_foot_camera_title_left;
            }
            getSupportActionBar().setTitle(titleRes);
        }
        PreferenceUtil pref = PreferenceUtil.Companion.instance(this);

        if (!pref.isFirstAppTutorial()) {
            startActivity(new Intent(this, TutorialWebViewActivity.class));
            pref.setFirstAppTutorial(true);
        } else {
            String message;
            if (viewType == TYPE_FOOT_RIGHT) {
                message = getString(R.string.activity_foot_camera_title_right_message);
            } else {
                message = getString(R.string.activity_foot_camera_title_left_message);
            }

            DialogUtil.Companion.getInstance().showMessageDialog(this, "", message, null, null);
        }

        img_circle = findViewById(R.id.img_circle);
        img_camera_disable = findViewById(R.id.img_camera_disable);
        layout_empty = findViewById(R.id.layout_empty);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null) {
            viewType = intent.getIntExtra("type", TYPE_FOOT_RIGHT);
        } else {
            viewType = TYPE_FOOT_RIGHT;
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        registerListener();
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        unregisterListener();
    }

    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        runUI(() -> {
                            if (isSensor) {
                                if (isFoot) {
                                    txt_status_sensor.setVisibility(View.INVISIBLE);
                                    txt_status_foot.setVisibility(View.INVISIBLE);
                                    txt_status_a4.setVisibility(View.VISIBLE);
                                } else {
                                    txt_status_sensor.setVisibility(View.INVISIBLE);
                                    txt_status_foot.setVisibility(View.VISIBLE);
                                    txt_status_a4.setVisibility(View.INVISIBLE);
                                }
                            } else {
                                txt_status_sensor.setVisibility(View.VISIBLE);
                                txt_status_foot.setVisibility(View.INVISIBLE);
                                txt_status_a4.setVisibility(View.INVISIBLE);
                            }
                        });

                        isValidation = false;
                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                isValidation = true;
                                Log.d("Dony", "minimumConfidence");
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);

                                if (isSensor) {
                                    if (!validationFoot(result)) {
                                        validationBase(result);
                                    }
                                }
                            }
                        }
                        if (!isValidation) {
                            isFoot = false;
                        }
                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");
                                    }
                                });
                    }
                });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // ${event.values[0]} : x축 값 : 위로 기울이면 -10~0, 아래로 기울이면 0~10
        // ${event.values[1]} : y축 값 : 왼쪽으로 기울이면 -10~0, 오른쪽으로 기울이면 0~10
        // ${event.values[2]} : z축 값 : 미사용
        float x = event.values[0];
        float y = event.values[1];
        float xCoord = x * 20;
        float yCoord = y * 20;

        img_circle.setX(btn_camera.getX() + xCoord);
        img_circle.setY(btn_camera.getY() + yCoord);

        if (x < 1.5 && x > -1.5 && y < 1.5 && y > -1.5) {
            isSensor = true;
            img_camera_disable.setVisibility(View.INVISIBLE);
            btn_camera.setVisibility(View.VISIBLE);
            btn_camera.setClickable(true);
        } else {
            isSensor = false;
            img_camera_disable.setVisibility(View.VISIBLE);
            btn_camera.setVisibility(View.INVISIBLE);
            btn_camera.setClickable(false);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }


    private void registerListener() {
        btn_camera.setEnabled(true);
        sensorManager.registerListener(
                this,
                //센서 매니저에 센서 등록(센서 받을 액티비티, 센서 종류, 센서 빈도)
                sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), // 기울기 센서
                SensorManager.SENSOR_DELAY_GAME
        );
    }

    private void unregisterListener() {
        btn_camera.setEnabled(false);
        sensorManager.unregisterListener(this);
    }

    private boolean validationFoot(Classifier.Recognition result) {
        if (result.getTitle().equals("b'foot'")) {
            isFoot = true;
            Log.d("Dony", "Find Foot");
            return true;
        } else {
            isFoot = false;
            return false;
        }
    }

    private void validationBase(Classifier.Recognition result) {
        if (result.getTitle().equals("b'base'")) {
            Log.d("Dony", "Find Base");
            Log.d("Dony", "top Y: " + guide_validation_top.getY());
            Log.d("Dony", "bottom Y: " + guide_validation_bottom.getY());
            final RectF detectionScreenRect = new RectF();
            tracker.frameToCanvasMatrix.mapRect(detectionScreenRect, result.getLocation());

            float point = detectionScreenRect.top;
            Log.d("Dony", "point : " + point);
            if (guide_validation_top.getY() <= point && guide_validation_bottom.getY() >= point) {
                txt_status_sensor.setVisibility(View.INVISIBLE);
                txt_status_foot.setVisibility(View.INVISIBLE);
                txt_status_a4.setVisibility(View.INVISIBLE);
                Toast.makeText(this, "버튼을 눌러 촬영해주세요.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void runUI(Runnable runnable) {
        runOnUiThread(runnable);
    }

    public boolean chkTouchInside(View view, int x, int y) {

        int[] location = new int[2];

        view.getLocationOnScreen(location);

        if (x >= location[0]) {

            if (x <= location[0] + view.getWidth()) {

                if (y >= location[1]) {

                    if (y <= location[1] + view.getHeight()) {

                        return true;

                    }

                }

            }

        }

        return false;

    }

}
