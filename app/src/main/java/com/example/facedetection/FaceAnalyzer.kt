package com.example.facedetection

import android.util.Log
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Реализация интерфейса [Analyzer][androidx.camera.core.ImageAnalysis.Analyzer]
 * для распознавания лиц при помощи Google ML Kit
 * @param onFaceDetected Callback-функция, возвращает список распознанных лиц и разрешение кадра
 */

class FaceAnalyzer(private val onFaceDetected: (List<Face>, Pair<Int?, Int?>) -> Unit) :
    ImageAnalysis.Analyzer {

    /**
     * Параметры детектора лиц [detector]
     * `.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)` определяет распознавание контуров
     * лица, включая глаза, брови, нос и рот
     */
    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()

    /**
     * Объект детектора лиц из Google ML Kit
     */
    private val detector = FaceDetection.getClient(realTimeOpts)

    /**
     * Метод преобразует кадры в [InputImage] для их обработки внутри [detector]
     */
    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val resolution = Pair(imageProxy.image?.width, imageProxy.image?.height)
            detector.process(image).addOnSuccessListener { faces ->
                onFaceDetected(faces, resolution)
            }.addOnFailureListener { e ->
                Log.e("FaceAnalyzer", "Face detection failed", e)
            }.addOnCompleteListener {
                imageProxy.close()
            }
        }
    }
}