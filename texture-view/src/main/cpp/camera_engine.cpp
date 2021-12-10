/**
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

/** Description
 *   Demonstrate NDK Camera interface added to android-24
 */

#include <cstdio>
#include <cstring>
#include "camera_engine.h"
#include "utils/native_debug.h"

CameraAppEngine::CameraAppEngine(JNIEnv* env, jobject instance, jint w, jint h)
    : env_(env),
      javaInstance_(instance),
      requestWidth_(w),
      requestHeight_(h),
      surface_(nullptr),
      camera_(nullptr),
      yuvReader_(nullptr),
      jpgReader_(nullptr),
      globalClass (nullptr),
      jvm (nullptr) {
  env->GetJavaVM(&jvm);
  javaInstance_ = reinterpret_cast<jclass>(env->NewGlobalRef(instance));
  jclass localClass = env_->GetObjectClass(javaInstance_);
  globalClass = reinterpret_cast<jclass>(env->NewGlobalRef(localClass));
  memset(&compatibleCameraRes_, 0, sizeof(compatibleCameraRes_));
  camera_ = new NDKCamera();
  ASSERT(camera_, "Failed to Create CameraObject");
  camera_->MatchCaptureSizeRequest(requestWidth_, requestHeight_,
                                   &compatibleCameraRes_);
}

CameraAppEngine::~CameraAppEngine() {
  if (camera_) {
    delete camera_;
    camera_ = nullptr;
  }

  if (yuvReader_) {
    delete yuvReader_;
    yuvReader_ = nullptr;
  }

  if (jpgReader_) {
    delete jpgReader_;
    jpgReader_ = nullptr;
  }

  if (surface_) {
    env_->DeleteGlobalRef(surface_);
    surface_ = nullptr;
  }

  env_->DeleteGlobalRef(globalClass);
  env_->DeleteGlobalRef(javaInstance_);
}

/**
 * Create a capture session with given Java Surface Object
 * @param surface a {@link Surface} object.
 */
void CameraAppEngine::CreateCameraSession(jobject surface) {
  surface_ = env_->NewGlobalRef(surface);

  yuvReader_ = new ImageReader(&compatibleCameraRes_, AIMAGE_FORMAT_YUV_420_888);
//  yuvReader_->SetPresentRotation(GetCameraSensorOrientation(ACAMERA_LENS_FACING_BACK));
//  yuvReader_->SetUIWindow(ANativeWindow_fromSurface(env_, surface));
  jpgReader_ = new ImageReader(&compatibleCameraRes_, AIMAGE_FORMAT_JPEG);
  jpgReader_->SetPresentRotation(GetCameraSensorOrientation(ACAMERA_LENS_FACING_BACK));
  jpgReader_->RegisterCallback(this, [this](void* ctx, const char* str) -> void {
      reinterpret_cast<CameraAppEngine* >(ctx)->OnQRDetected(str);
  });
  camera_->CreateSession(ANativeWindow_fromSurface(env_, surface), jpgReader_->GetNativeWindow(), false, GetCameraSensorOrientation(ACAMERA_LENS_FACING_BACK));
//  camera_->CreateSession(ANativeWindow_fromSurface(env_, surface), yuvReader_->GetNativeWindow());
}

/**
 * @return cached {@link Surface} object
 */
jobject CameraAppEngine::GetSurfaceObject() { return surface_; }

/**
 *
 * @return saved camera preview resolution for this session
 */
const ImageFormat& CameraAppEngine::GetCompatibleCameraRes() const {
  return compatibleCameraRes_;
}

int CameraAppEngine::GetCameraSensorOrientation(int32_t requestFacing) {
  ASSERT(requestFacing == ACAMERA_LENS_FACING_BACK,
         "Only support rear facing camera");
  int32_t facing = 0, angle = 0;
  if (camera_->GetSensorOrientation(&facing, &angle) ||
      facing == requestFacing) {
    return angle;
  }
  ASSERT(false, "Failed for GetSensorOrientation()");
  return 0;
}

/**
 *
 * @param start is true to start preview, false to stop preview
 * @return  true if preview started, false when error happened
 */
void CameraAppEngine::StartPreview(bool start) { camera_->StartPreview(start); }

void CameraAppEngine::OnQRDetected(const char *result) {
  JNIEnv* env;

  jvm->AttachCurrentThread(&env, nullptr);
  jmethodID methodID = env->GetMethodID(globalClass, "onQRDetected", "(Ljava/lang/String;)V");
  jstring javaName = env->NewStringUTF(result);

  env->CallVoidMethod(javaInstance_, methodID, javaName);
  jvm->DetachCurrentThread();
}

void CameraAppEngine::scanPhoto(void)
{
  if (camera_) {
    camera_->TakePhoto();
  }
}