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

import com.perfitt.android.perfitt_partners.R;
import com.perfitt.android.perfitt_partners.models.TFMappingModel;
import com.perfitt.android.perfitt_partners.tflite.Classifier;
import com.perfitt.android.perfitt_partners.tflite.TFLiteObjectDetectionAPIModel;
import com.perfitt.android.perfitt_partners.tracking.MultiBoxTracker;
import com.perfitt.android.perfitt_partners.utils.DialogSDKUtil;
import com.perfitt.android.perfitt_partners.utils.ImageUtils;
import com.perfitt.android.perfitt_partners.utils.Logger;
import com.perfitt.android.perfitt_partners.utils.PoolUtils;
import com.perfitt.android.perfitt_partners.views.BorderedText;
import com.perfitt.android.perfitt_partners.views.OverlayView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SDKKitDetectorActivity extends SDKCameraActivity implements OnImageAvailableListener, SensorEventListener {
    private static final Logger LOGGER = new Logger();

    // Configuration values for the prepackaged SSD model.
    private static final int TF_OD_API_INPUT_SIZE = 320;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "kit/model.tflite";
    private static final String TF_OD_API_LABELS_FILE = "kit/dict.txt";
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    // Minimum detection confidence to track a detection.
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.8f;
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

    private SensorManager sensorManager;
    private boolean isSensor = false;

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
        parentType = LandingActivity.KIT;

        String message;
        if (viewType == TYPE_FOOT_RIGHT) {
            message = getString(R.string.sdk_activity_foot_camera_title_right_message);
        } else {
            message = getString(R.string.sdk_activity_foot_camera_title_left_message);
        }

        DialogSDKUtil.Companion.getINSTANCE().showMessageDialog(this, "", message, null, null);

        img_circle = findViewById(R.id.img_circle);
        img_camera_disable = findViewById(R.id.img_camera_disable);
//        img_divider = findViewById(R.id.top_divider);
//        img_divider.setVisibility(View.GONE);
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
                        final ArrayList<TFMappingModel> tfMappingModels = new ArrayList<>();

                        for (int i = 0; i < results.size(); i++) {
//                        for (final Classifier.Recognition result : results) {
                            final RectF location = results.get(i).getLocation();
                            TFMappingModel model = new TFMappingModel();
                            if (location != null && results.get(i).getConfidence() >= minimumConfidence) {
                                if (results.get(i).getTitle().equals("b'foot'") && results.get(i).getConfidence() >= 0.9) {
                                    mappedRecognitions.add(results.get(i));
                                }
                                if (results.get(i).getTitle().equals("b'base'") && results.get(i).getConfidence() >= 0.6) {
                                    mappedRecognitions.add(results.get(i));
                                }
                                if (results.get(i).getTitle().equals("b'left_triangle'") && results.get(i).getConfidence() >= 0.6) {
                                    mappedRecognitions.add(results.get(i));
                                }
                                if (results.get(i).getTitle().equals("b'right_triangle'") && results.get(i).getConfidence() >= 0.6) {
                                    mappedRecognitions.add(results.get(i));
                                }

                                model.setTitle(results.get(i).getTitle());
                                model.setFloat(((TFLiteObjectDetectionAPIModel) detector).outputLocations[0][i]);
                                tfMappingModels.add(model);

                                canvas.drawRect(location, paint);
                                cropToFrameTransform.mapRect(location);
                                results.get(i).setLocation(location);
                            }
                        }

                        boolean isFoot = false, isBase = false, isTriangle = false; //isTriangleDegree = false;
                        RectF leftRect = null, rightRect = null;
                        for (int i = 0; i < mappedRecognitions.size(); i++) {
//                            for (final Classifier.Recognition result : mappedRecognitions) {
                            if (!isBase) {
                                isBase = validationBase(mappedRecognitions.get(i));

                                if (isBase) {
                                    runUI(() -> btn_guide_4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0));
                                    if (viewType == TYPE_FOOT_RIGHT) {
                                        PoolUtils.Companion.getInstance().getRightFoot().setBaseModel(tfMappingModels.get(i));
                                    } else {
                                        PoolUtils.Companion.getInstance().getLeftFoot().setBaseModel(tfMappingModels.get(i));
                                    }
                                }
                            }
                            if (!isFoot) {
                                isFoot = validationFoot(mappedRecognitions.get(i));
                                if (isFoot) {
                                    if (viewType == TYPE_FOOT_RIGHT) {
                                        PoolUtils.Companion.getInstance().getRightFoot().setFootModel(tfMappingModels.get(i));
                                    } else {
                                        PoolUtils.Companion.getInstance().getLeftFoot().setFootModel(tfMappingModels.get(i));
                                    }
                                }
                            }
                            if (leftRect == null) {
                                leftRect = validationTriangleLeft(mappedRecognitions.get(i));
                                if (leftRect != null) {
                                    if (viewType == TYPE_FOOT_RIGHT) {
                                        PoolUtils.Companion.getInstance().getRightFoot().setLeftTriModel(tfMappingModels.get(i));
                                    } else {
                                        PoolUtils.Companion.getInstance().getLeftFoot().setLeftTriModel(tfMappingModels.get(i));
                                    }
                                }
                            }

                            if (rightRect == null) {
                                rightRect = validationTriangleRight(mappedRecognitions.get(i));
                                if (rightRect != null) {
                                    if (viewType == TYPE_FOOT_RIGHT) {
                                        PoolUtils.Companion.getInstance().getRightFoot().setRightTriModel(tfMappingModels.get(i));
                                    } else {
                                        PoolUtils.Companion.getInstance().getLeftFoot().setRightTriModel(tfMappingModels.get(i));
                                    }
                                }
                            }

//                            if (!isTriangleDegree) {
//                                isTriangleDegree = validationTriangle(leftRect, rightRect);
//                            }
                            if (!isTriangle) {
                                runUI(() -> btn_guide_3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0));
                                isTriangle = leftRect != null && rightRect != null;
                            }
                        }

                        if (isSensor) {
                            runUI(() -> {
                                btn_guide_1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
//                                btn_guide_2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0);
//                                btn_guide_3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0);
//                                btn_guide_4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0);
                            });
                            if (!isBase) {
                                runUI(() -> {
                                    // 가이드
                                    cameraValidation(false);
//                                    btn_guide_4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                });
                            } else if (!isFoot) {
                                runUI(() -> {
                                    // 발
                                    cameraValidation(false);
//                                    btn_guide_2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                });
                            } else if (!isTriangle) {
                                runUI(() -> {
                                    // 조명
                                    cameraValidation(false);
//                                    btn_guide_3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                });
                            }
//                            else if (!isTriangleDegree) {
//                                runUI(() -> {
//                                    cameraValidation(false);
//                                    txt_status_kit.setVisibility(View.VISIBLE);
//                                });
//                            }
                            else {
                                runUI(() -> {
                                    cameraValidation(true);
                                    btn_guide_1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                    btn_guide_2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                    btn_guide_3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                    btn_guide_4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0);
                                });
                            }
                        } else {
                            runUI(() -> {
                                // 수평
                                btn_guide_1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0);
                                cameraValidation(false);
                            });
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

//                        runUI(() -> {
//                            showFrameInfo(previewWidth + "x" + previewHeight);
//                            showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
//                            showInference(lastProcessingTimeMs + "ms");
//                        });
                    }
                });
    }

    void cameraValidation(boolean isValidation) {
        if (isValidation) {
            img_camera_disable.setVisibility(View.INVISIBLE);
            btn_camera.setVisibility(View.VISIBLE);
            btn_camera.setClickable(true);
        } else {
            img_camera_disable.setVisibility(View.VISIBLE);
            btn_camera.setVisibility(View.INVISIBLE);
            btn_camera.setClickable(false);
        }
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
        } else {
            isSensor = false;
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
            // 발이 탐지 되었다면
            runUI(() -> btn_guide_2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_completed, 0, 0));
            return true;
        } else {
            runUI(() -> btn_guide_2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0));
            return false;
        }
    }

    private boolean validationBase(Classifier.Recognition result) {
        if (result.getTitle().equals("b'base'")) {
            // 발판이 탐지 되었다면
            return validationGuide(result);
        } else {
            runUI(() -> btn_guide_4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0));
            return false;
        }
    }

    private RectF validationTriangleLeft(Classifier.Recognition result) {
        RectF leftRect = null;
        if (result.getTitle().equals("b'left_triangle'")) {
            // 왼쪽 삼각형이 탐지 되었다면
            leftRect = new RectF();
            tracker.frameToCanvasMatrix.mapRect(leftRect, result.getLocation());
        } else {
            runUI(() -> btn_guide_3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0));
        }
        return leftRect;
    }

    private RectF validationTriangleRight(Classifier.Recognition result) {
        RectF rightRect = null;
        if (result.getTitle().equals("b'right_triangle'")) {
            // 오른쪽 삼각형이 탐지 되었다면
            rightRect = new RectF();
            tracker.frameToCanvasMatrix.mapRect(rightRect, result.getLocation());
        } else {
            runUI(() -> btn_guide_3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_incomplete, 0, 0));
        }
        return rightRect;
    }

    public boolean validationTriangle(RectF leftRect, RectF rightRect) {
        if (leftRect == null || rightRect == null) {
            return false;
        }

        double bottomSize = (leftRect.left - rightRect.right);
        double heightSize = (leftRect.top - rightRect.top);
        double angleResult = Math.atan2(heightSize, bottomSize);

        double degree = (angleResult * 180 / Math.PI);

        double adjust = 4;
        if ((degree < adjust && degree > -adjust) || (degree + 180 > -adjust && degree + 180 < adjust) || (degree < 180 && degree > 180 - adjust)) {
            return true;
        }

        if (leftRect.top < rightRect.top) {
            // 좌표의 각도가 -8 미만 인 경우
//            runUI(() -> {
//                txt_status_kit.setVisibility(View.VISIBLE);
//                txt_status_kit.setText(R.string.activity_foot_camera_kit_validation_triangle);
//            });
            return false;
        } else if (leftRect.top > rightRect.top) {
            // 좌표의 각도가 8도 초과 인 경우
//            runUI(() -> {
//                txt_status_kit.setVisibility(View.VISIBLE);
//                txt_status_kit.setText(R.string.activity_foot_camera_kit_validation_triangle);
//            });
            return false;
        }
        return false;
    }

    private boolean validationGuide(Classifier.Recognition result) {
        Log.d("Dony", "-----------------------------------");
        Log.d("Dony", "In Top Y: " + guide_validation_top_in.getY());
        Log.d("Dony", "In Left X: " + guide_validation_left_in.getX());
        Log.d("Dony", "In Right X: " + guide_validation_right_in.getX());
        Log.d("Dony", "-----------------------------------");
        Log.d("Dony", "Out Top Y: " + guide_validation_top_out.getY());
        Log.d("Dony", "Out Left X: " + guide_validation_left_out.getX());
        Log.d("Dony", "Out Right X: " + guide_validation_right_out.getX());
        final RectF detectionScreenRect = new RectF();
        tracker.frameToCanvasMatrix.mapRect(detectionScreenRect, result.getLocation());

        float pointTop = detectionScreenRect.top;
        float pointLeft = detectionScreenRect.left;
        float pointRight = detectionScreenRect.right;
        Log.d("Dony", "-----------------------------------");
        Log.d("Dony", "point Top: " + pointTop);
        Log.d("Dony", "point Left: " + pointLeft);
        Log.d("Dony", "point Right: " + pointRight);

        // TODO Debug
//        if (guide_validation_top_out.getY() <= pointTop && guide_validation_top_in.getY() >= pointTop) {
//            top_divider.setBackgroundColor(ContextCompat.getColor(this, R.color.vibrant_green));
//        } else {
//            top_divider.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary));
//        }
//        if (guide_validation_left_out.getX() <= pointLeft && guide_validation_left_in.getX() >= pointLeft) {
//            left_divider.setBackgroundColor(ContextCompat.getColor(this, R.color.vibrant_green));
//        } else {
//            left_divider.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary));
//        }
//        if (guide_validation_right_in.getX() <= pointRight && guide_validation_right_out.getX() >= pointRight) {
//            right_divider.setBackgroundColor(ContextCompat.getColor(this, R.color.vibrant_green));
//        } else {
//            right_divider.setBackgroundColor(ContextCompat.getColor(this, R.color.color_primary));
//        }

        if (guide_validation_top_out.getY() <= pointTop && guide_validation_top_in.getY() >= pointTop &&
                guide_validation_left_out.getX() <= pointLeft && guide_validation_left_in.getX() >= pointLeft &&
                guide_validation_right_in.getX() <= pointRight && guide_validation_right_out.getX() >= pointRight) {
            Log.d("Dony", "Baee Validation Success");
            return true;
        } else {
            Log.d("Dony", "Baee Validation Failed");
            return false;
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
