Deep tracker model slot

Expected asset path:
- `models/deeplab_mobile.tflite`
- `models/mediapipe_object_detector.tflite`

Purpose:
- reserves the bundled model location for the `Deep Accurate` tracker mode
- reserves the bundled model location for the `MediaPipe Detect` tracker mode
- keeps the app-side path stable while the real TFLite segmentation pipeline is implemented

Current state:
- the app can detect whether the model asset exists
- `MediaPipe Detect` expects an object detector model with MediaPipe/TFLite metadata
- `Deep Accurate` expects a compatible DeepLab-style segmentation model
