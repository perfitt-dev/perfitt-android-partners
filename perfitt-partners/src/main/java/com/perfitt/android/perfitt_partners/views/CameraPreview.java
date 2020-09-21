package com.perfitt.android.perfitt_partners.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private Camera mCamera;
    public List<Camera.Size> prSupportedPreviewSizes;
    private Camera.Size prPreviewSize;
    private Camera.Parameters params;

    private double touch_interval_X = 0; // X 터치 간격
    private double touch_interval_Y = 0; // Y 터치 간격
    private int zoom_in_count = 0; // 줌 인 카운트
    private int zoom_out_count = 0; // 줌 아웃 카운트
    private int touch_zoom = 0; // 줌 크기
    private int currentZoom = 0;

    public CameraPreview(Context context) {
        super(context);
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mCamera = Camera.open();
        prSupportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
    }

    public boolean takePhoto(Camera.PictureCallback handler) {
        if (mCamera != null) {
            mCamera.takePicture(null, null, handler);
            return true;
        } else {
            return false;
        }
    }

    public void startPreview() {
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Surface가 생성되었으니 프리뷰를 어디에 띄울지 지정해준다. (holder 로 받은 SurfaceHolder에 뿌려준다.
        try {
            if (mCamera == null) {
                mCamera = Camera.open();
            }
            params = mCamera.getParameters();
            if (getResources().getConfiguration().orientation != Configuration.ORIENTATION_LANDSCAPE) {
                params.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
                params.setRotation(90);
            } else {
                params.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
                params.setRotation(0);
            }
// 1024 768
            Camera.Size size = getBestPreviewSize(1280, 720);
            params.setPictureSize(size.width, size.height);
            mCamera.setParameters(params);
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
        }
        params.setZoom(currentZoom);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // 프리뷰 제거시 카메라 사용도 끝났다고 간주하여 리소스를 전부 반환한다
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }


    private Camera.Size getBestPreviewSize(int width, int height) {
        Camera.Size result = null;
        Camera.Parameters p = mCamera.getParameters();
        for (Camera.Size size : p.getSupportedPreviewSizes()) {
            if (size.width <= width && size.height <= height) {
                if (result == null) {
                    result = size;
                } else {
                    int resultArea = result.width * result.height;
                    int newArea = size.width * size.height;

                    if (newArea > resultArea) {
                        result = size;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // 프리뷰를 회전시키거나 변경시 처리를 여기서 해준다.
        // 프리뷰 변경시에는 먼저 프리뷰를 멈춘다음 변경해야한다.
        if (holder.getSurface() == null) {
            // 프리뷰가 존재하지 않을때
            return;
        }

        // 우선 멈춘다
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // 프리뷰가 존재조차 하지 않는 경우다
        }

        // 프리뷰 변경, 처리 등을 여기서 해준다.
        List<String> focusModes = params.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        params.setPreviewSize(prPreviewSize.width, prPreviewSize.height);
        mCamera.setParameters(params);
        // 새로 변경된 설정으로 프리뷰를 재생성한다
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e) {
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (prSupportedPreviewSizes != null) {
            prPreviewSize = getBestPreviewSize(1280, 720);
            setMeasuredDimension(prPreviewSize.width, prPreviewSize.height);
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mCamera == null) {
            return true;
        }
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN: // 싱글 터치
                // 오토 포커스 설정
                mCamera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean b, Camera camera) {

                    }
                });
                break;

            case MotionEvent.ACTION_MOVE: // 터치 후 이동 시
                if (event.getPointerCount() == 2) { // 터치 손가락 2개일 때
                    double now_interval_X = (double) Math.abs(event.getX(0) - event.getX(1)); // 두 손가락 X좌표 차이 절대값
                    double now_interval_Y = (double) Math.abs(event.getY(0) - event.getY(1)); // 두 손가락 Y좌표 차이 절대값
                    if (touch_interval_X < now_interval_X && touch_interval_Y < now_interval_Y) { // 이전 값과 비교
                        // 여기에 확대기능에 대한 코드를 정의 하면됩니다. (두 손가락을 벌렸을 때 분기점입니다.)
                        zoom_in_count++;
                        if (zoom_in_count > 1) { // 카운트를 세는 이유 : 너무 많은 호출을 줄이기 위해
                            zoom_in_count = 0;
                            touch_zoom += 4;

                            if (params.getMaxZoom() < touch_zoom) {
                                touch_zoom = params.getMaxZoom();
                            }
                            params.setZoom(touch_zoom);
                            mCamera.setParameters(params);
                        }
                    }

                    if (touch_interval_X > now_interval_X && touch_interval_Y > now_interval_Y) {
                        // 여기에 축소기능에 대한 코드를 정의 하면됩니다. (두 손가락 사이를 좁혔을 때 분기점입니다.)
                        zoom_out_count++;
                        if (zoom_out_count > 1) {
                            zoom_out_count = 0;
                            touch_zoom -= 4;

                            if (0 > touch_zoom) {
                                touch_zoom = 0;
                            }

                            params.setZoom(touch_zoom);
                            mCamera.setParameters(params);
                        }
                    }
                    touch_interval_X = (double) Math.abs(event.getX(0) - event.getX(1));
                    touch_interval_Y = (double) Math.abs(event.getY(0) - event.getY(1));
                }
                break;
        }
        return true;
    }

    /**
     * 줌 확대
     */
    public void zoomIn() {
        touch_zoom += 4;

        if (params.getMaxZoom() < touch_zoom) {
            touch_zoom = params.getMaxZoom();
        }
        params.setZoom(touch_zoom);
        mCamera.setParameters(params);
    }

    /**
     * 줌 축소
     */
    public void zoomOut() {
        touch_zoom -= 4;

        if (0 > touch_zoom) {
            touch_zoom = 0;
        }

        params.setZoom(touch_zoom);
        mCamera.setParameters(params);
    }

    /**
     * 최대 줌 값
     */
    public Integer getMaxZoom() {
        return params.getMaxZoom();
    }

    /**
     * 현재 줌 값
     */
    public Integer getCurrentZoom() {
        return params.getZoom();
    }

    public void setCurrentZoom(Integer zoom) {
        currentZoom = zoom;
    }
}