# QR Scanner with NDK Camera2
A simple Android QR scanner built with NDK Camera2 API and [Dynamsoft Barcode SDK for Android](https://www.dynamsoft.com/barcode-reader/sdk-mobile/).

## Usage
1. Get a [valid license key](https://www.dynamsoft.com/customer/license/trialLicense?product=dbr) and update the code in `image_reader.cpp`:
    ```cpp
    DBR_InitLicense(barcode_reader, "LICENSE-KEY");
    ```
2. Build and run the project:
    <kbd><img src="https://www.dynamsoft.com/codepool/img/2021/12/ndk-camera-qr-scanner.jpg" width="40%">

## References
- [https://github.com/android/ndk-samples/tree/main/camera](https://github.com/android/ndk-samples/tree/main/camera)
- [https://android.googlesource.com/platform/frameworks/av/+/c360382/media/ndk/](https://android.googlesource.com/platform/frameworks/av/+/c360382/media/ndk/)
- [https://android.googlesource.com/platform/cts/+/master/tests/camera/libctscamera2jni/native-camera-jni.cpp](https://android.googlesource.com/platform/cts/+/master/tests/camera/libctscamera2jni/native-camera-jni.cpp)
    
## Blog
[How to Implement Android QR Scanner with NDK Camera2 API](https://www.dynamsoft.com/codepool/android-ndk-camera-qr-scanner.html)
