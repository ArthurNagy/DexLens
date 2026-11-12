# 14. Run the image model with LiteRT, bundled and on the CPU

Date: 2026-11-12

## Status

Accepted

## Context

The scanner adds a picture signal next to text recognition: a 9.5 MB MobileNetV4 encoder (`dexlens_encoder.tflite`) that
turns a 224×224 crop into a 256-number vector, matched against 28,085 bundled reference vectors. The app needs a runtime
for the `.tflite` file. Recognition must work offline from the first launch, like the bundled ML Kit text recogniser, and
the app is open source, so builds without Google Play should work too.

Alternatives considered:
- LiteRT from Google Play services (`play-services-litert`), Google's default, which keeps the runtime out of the APK.
  It ties recognition to Play services being present and up to date.
- The legacy `org.tensorflow.lite.Interpreter`, which is deprecated.
- The GPU or NPU. The shipped weight-only 8-bit file fails on the GPU delegate, and the float version was barely faster on
  the GPU (16.2 ms against 17.7 ms on the CPU, Pixel 6).

## Decision

Use LiteRT's `CompiledModel` API (`com.google.ai.edge.litert:litert`) with the runtime bundled in the APK, on the CPU
only, without `litert-gpu`. One `PictureRecognizer` thread owns the model, as LiteRT's thread-confinement rule requires.

## Consequences

- The APK grows by LiteRT's native library for each ABI. The Play bundle splits it per ABI.
- The encoder can't run under Robolectric, so it is covered by an instrumented test and device QA. The thread, timeout
  and failure handling around it is JVM-tested over a fake engine.
- A retrained model ships by replacing the two asset files, as long as it keeps the input and output contract.
