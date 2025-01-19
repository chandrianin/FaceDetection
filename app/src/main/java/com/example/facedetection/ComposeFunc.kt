package com.example.facedetection

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ComposeFunc {

    /**
     * Отображает превью передней камера устройства на экране
     *
     * @param it Callback-функция, возвращает список найденных лиц и разрешение кадра
     */
    @Composable
    fun CameraPreviewScreen(it: (MutableState<List<Face>>, MutableState<Pair<Int, Int>>) -> Unit) {
        // Конфигурирование
        val lensFacing = CameraSelector.LENS_FACING_FRONT
        val imageAnalysis = ImageAnalysis.Builder().build()
        val context: Context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val preview = androidx.camera.core.Preview.Builder().build()
        val previewView = remember { PreviewView(context) }

        // Переменные для хранения списка лиц и разрешения камеры
        val facesDetected = remember { mutableStateOf<List<Face>>(emptyList()) }
        val imageResolution = remember { mutableStateOf(Pair(1, 1)) }
        imageAnalysis.setAnalyzer(
            ContextCompat.getMainExecutor(context),
            FaceAnalyzer { faces, resolution ->
                facesDetected.value = faces
                val newPair: Pair<Int, Int> = Pair(resolution.first ?: 1, resolution.second ?: 1)
                imageResolution.value = newPair
            })
        it(facesDetected, imageResolution)

        // Конфигурирование
        val cameraxSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        LaunchedEffect(lensFacing) {
            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraxSelector, preview, imageAnalysis)
            preview.setSurfaceProvider(previewView.surfaceProvider)
        }
        AndroidView(factory = { previewView }, modifier = Modifier.aspectRatio(3 / 4f))

        val previewWidth = previewView.width
        val previewHeight = previewView.height
        Log.d("PreviewResolution", "Width: $previewWidth, Height: $previewHeight")
    }


    /**
     * Отображает текст текущего состояния обнаружения лица
     *
     * @param facesPosition список обнаруженных лиц.
     */
    @Composable
    fun FacePosition(facesPosition: List<Face>) {
        val msg =
            if (facesPosition.isNotEmpty())
                "Everything is working"
            else "The face is not detected"
        Text(msg, color = MaterialTheme.colorScheme.primary)
    }

    /**
     * Отображает соединенные между собой точки контура лица при помощи [Canvas].
     * Приложение способно отображать одновременно лишь контур одного лица, так как
     * в [документации Google ML Kit](https://developers.google.com/ml-kit/vision/face-detection/android#kotlin)
     * указано следующее:
     * > Note that when contour detection is enabled, only one face is detected
     *
     * @param facesPositions список обнаруженных лиц.
     * @param imageResolution разрешение кадра
     *
     */
    @Composable
    fun FaceCanvas(facesPositions: List<Face>, imageResolution: Pair<Int, Int>) {
        Log.d("FaceCanvas", facesPositions.size.toString())
        Canvas(
            modifier = Modifier
                .aspectRatio(3 / 4f)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0f)),
        ) {
            for (face in facesPositions) {
                val points = mutableListOf<Offset>()
                for (point in face.allContours[0].points) {
                    Log.d("FaceCanvas", point.toString())

                    val imageWidth: Int
                    val imageHeight: Int
                    // Данная конструкция необходима, так как в противном случае
                    // значения ширины и высоты кадра могут быть перепутаны
                    if (size.width > size.height) {
                        imageWidth = Math.max(
                            imageResolution.first,
                            imageResolution.second
                        )
                        imageHeight = Math.min(
                            imageResolution.first,
                            imageResolution.second
                        )
                    } else {
                        imageWidth = Math.min(
                            imageResolution.first,
                            imageResolution.second
                        )
                        imageHeight = Math.max(
                            imageResolution.first,
                            imageResolution.second
                        )
                    }

                    points.add(
                        Offset(
                            size.width - point.x * size.width / imageWidth,
                            point.y * size.height / imageHeight
                        )
                    )
                }
                drawPoints(
                    points = points,
                    strokeWidth = 10f,
                    pointMode = PointMode.Polygon,
                    color = Color.Green
                )
            }
        }
    }

    /**
     * Используется для управления камерой при помощи `ProcessCameraProvider`
     */
    private suspend fun Context.getCameraProvider(): ProcessCameraProvider =
        suspendCoroutine { continuation ->
            ProcessCameraProvider.getInstance(this).also { cameraProvider ->
                cameraProvider.addListener({
                    continuation.resume(cameraProvider.get())
                }, ContextCompat.getMainExecutor(this))
            }
        }
}