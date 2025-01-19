package com.example.facedetection

import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.example.facedetection.ui.theme.FaceDetecktionTheme
import com.google.mlkit.vision.face.Face


class MainActivity : ComponentActivity() {

    /**
     * Запрос разрешения на использование камеры
     */
    private val cameraPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                setCameraPreview()
            } else {
                Toast.makeText(this, "Для работы необходим доступ к камере", Toast.LENGTH_LONG)
                    .show()
            }
        }

    /**
     * Метод `onCreate()` вызывается после запуска приложение.
     * Если дано разрешение использовать камеру, запускает функцию распознавания лица,
     * иначе запрашивает разрешение
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.CAMERA
            )
            -> { // если доступ к камере есть
                setCameraPreview()
            }

            else -> { // иначе запрашиваем разрешение
                cameraPermissionRequest.launch(android.Manifest.permission.CAMERA)
            }
        }

    }

    /**
     * Заполняет экран необходимыми элементами интерфейса с помощью
     * [CameraPreviewScreen][ComposeFunc.CameraPreviewScreen],
     * [FaceCanvas][ComposeFunc.FaceCanvas] и
     * [FacePosition][ComposeFunc.FacePosition]
     */
    private fun setCameraPreview() {
        setContent {
            FaceDetecktionTheme {
                var facesState = remember { mutableStateOf<List<Face>>(emptyList()) }
                var imageResolution = remember { mutableStateOf(Pair(1, 1)) }
                ComposeFunc().CameraPreviewScreen { facesDetected, resolution ->
                    facesState = facesDetected
                    imageResolution = resolution
                }
                ComposeFunc().FaceCanvas(facesState.value, imageResolution.value)
                ComposeFunc().FacePosition(facesState.value)
            }
        }
    }
}