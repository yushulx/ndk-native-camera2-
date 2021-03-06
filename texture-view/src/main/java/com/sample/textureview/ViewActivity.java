/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sample.textureview;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.util.Size;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import static android.hardware.camera2.CameraMetadata.LENS_FACING_BACK;

import java.util.Timer;
import java.util.TimerTask;

public class ViewActivity extends Activity
        implements TextureView.SurfaceTextureListener,
        ActivityCompat.OnRequestPermissionsResultCallback {
    long ndkCamera_;
    private TextureView textureView_;
    ImageButton _takePhoto;
    Surface surface_ = null;
    private Size cameraPreviewSize_;
    Timer timer;
    TextView textView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onWindowFocusChanged(true);
        setContentView(R.layout.activity_main);
        timer = new Timer();
        textView = findViewById(R.id.textView);
        _takePhoto = (ImageButton)findViewById(R.id.takePhoto);
        _takePhoto.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            scanPhoto(ndkCamera_);
                        }
                    });
        if (isCamera2Device()) {
            RequestCamera();
        } else {
            Log.e("CameraSample", "Found legacy camera device, this sample needs camera2 device");
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
    private boolean isCamera2Device() {
        CameraManager camMgr = (CameraManager)getSystemService(Context.CAMERA_SERVICE);
        boolean camera2Dev = true;
        try {
            String[] cameraIds = camMgr.getCameraIdList();
            if (cameraIds.length != 0 ) {
                for (String id : cameraIds) {
                    CameraCharacteristics characteristics = camMgr.getCameraCharacteristics(id);
                    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    int facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY &&
                            facing == LENS_FACING_BACK) {
                        camera2Dev =  false;
                    }
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            camera2Dev = false;
        }
        return camera2Dev;
    }
    private void createTextureView() {
        textureView_ = (TextureView) findViewById(R.id.texturePreview);
        textureView_.setSurfaceTextureListener(this);
        if (textureView_.isAvailable()) {
            onSurfaceTextureAvailable(textureView_.getSurfaceTexture(),
                    textureView_.getWidth(), textureView_.getHeight());
        }
    }

    public void onSurfaceTextureAvailable(SurfaceTexture surface,
                                          int width, int height) {
        createNativeCamera();

        resizeTextureView(width, height);
        surface.setDefaultBufferSize(cameraPreviewSize_.getWidth(),
                cameraPreviewSize_.getHeight());
        surface_ = new Surface(surface);
        onPreviewSurfaceCreated(ndkCamera_, surface_);
        scheduleTask();
    }

    private void resizeTextureView(int textureWidth, int textureHeight) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int newWidth = textureWidth;
        int newHeight = textureWidth * cameraPreviewSize_.getWidth() / cameraPreviewSize_.getHeight();

        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            newHeight = (textureWidth * cameraPreviewSize_.getHeight()) / cameraPreviewSize_.getWidth();
        }
        textureView_.setLayoutParams(
                new FrameLayout.LayoutParams(newWidth, newHeight, Gravity.CENTER));
        configureTransform(newWidth, newHeight);
    }

    /**
     * configureTransform()
     * Courtesy to https://github.com/google/cameraview/blob/master/library/src/main/api14/com/google/android/cameraview/TextureViewPreview.java#L108
     *
     * @param width  TextureView width
     * @param height is TextureView height
     */
    void configureTransform(int width, int height) {
        int mDisplayOrientation = getWindowManager().getDefaultDisplay().getRotation() * 90;
        Matrix matrix = new Matrix();
        if (mDisplayOrientation % 180 == 90) {
            //final int width = getWidth();
            //final int height = getHeight();
            // Rotate the camera preview when the screen is landscape.
            matrix.setPolyToPoly(
                    new float[]{
                            0.f, 0.f, // top left
                            width, 0.f, // top right
                            0.f, height, // bottom left
                            width, height, // bottom right
                    }, 0,
                    mDisplayOrientation == 90 ?
                            // Clockwise
                            new float[]{
                                    0.f, height, // top left
                                    0.f, 0.f,    // top right
                                    width, height, // bottom left
                                    width, 0.f, // bottom right
                            } : // mDisplayOrientation == 270
                            // Counter-clockwise
                            new float[]{
                                    width, 0.f, // top left
                                    width, height, // top right
                                    0.f, 0.f, // bottom left
                                    0.f, height, // bottom right
                            }, 0,
                    4);
        } else if (mDisplayOrientation == 180) {
            matrix.postRotate(180, width / 2, height / 2);
        }
        textureView_.setTransform(matrix);
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface,
                                            int width, int height) {
        int tmp = width;
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        onPreviewSurfaceDestroyed(ndkCamera_, surface_);
        deleteCamera(ndkCamera_, surface_);
        ndkCamera_ = 0;
        surface_ = null;
        return true;
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        SurfaceTexture tmp = surface;
    }

    private static final int PERMISSION_REQUEST_CODE_CAMERA = 1;

    public void scheduleTask() {
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (ndkCamera_ != 0 && surface_ != null) {
//                    scanPhoto(ndkCamera_);
//                    scanVideo(ndkCamera_, surface_);
                }
            }
        }, 0);
    }

    public void RequestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE_CAMERA);
            return;
        }
        createTextureView();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        /*
         * if any permission failed, the sample could not play
         */
        if (PERMISSION_REQUEST_CODE_CAMERA != requestCode) {
            super.onRequestPermissionsResult(requestCode,
                    permissions,
                    grantResults);
            return;
        }

        if (grantResults.length == 1 &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Thread initCamera = new Thread(new Runnable() {
                public void run() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            createTextureView();
                        }
                    });
                }
            });
            initCamera.start();
        }
    }

    private void createNativeCamera() {
        Display display = getWindowManager().getDefaultDisplay();
        int height = display.getMode().getPhysicalHeight();
        int width = display.getMode().getPhysicalWidth();

        ndkCamera_ = createCamera(width, height);

        cameraPreviewSize_ = getMinimumCompatiblePreviewSize(ndkCamera_);

    }

    public void onQRDetected(String result) {
        final String content = result;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                Toast.makeText(getApplicationContext(),
//                        content, Toast.LENGTH_SHORT).show();
                textView.setText(content);
                scheduleTask();
            }
        });
    }

    private native void scanVideo(long ndkCamera, Surface surface);
    private native void scanPhoto(long ndkCamera);

    private native long createCamera(int width, int height);

    private native Size getMinimumCompatiblePreviewSize(long ndkCamera);

    private native void onPreviewSurfaceCreated(long ndkCamera, Surface surface);

    private native void onPreviewSurfaceDestroyed(long ndkCamera, Surface surface);

    private native void deleteCamera(long ndkCamera, Surface surface);

    static {
        System.loadLibrary("camera_textureview");
    }

}
