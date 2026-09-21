package com.pifuhdc.viewer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberEnvironmentLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.rememberModelLoader

class DuckTestActivity : ComponentActivity() {

    private var selectedUri by mutableStateOf<Uri?>(null)

    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            selectedUri = uri
            uri?.let {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ViewerScreen(
                selectedUri = selectedUri,
                onOpen = {
                    picker.launch(
                        arrayOf(
                            "model/gltf-binary",
                            "application/octet-stream"
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun ViewerScreen(
    selectedUri: Uri?,
    onOpen: () -> Unit
) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val environmentLoader = rememberEnvironmentLoader(engine)

    val modelInstance = remember(selectedUri) {
        selectedUri?.toString()
    }?.let { uri ->
        rememberModelInstance(modelLoader, uri)
    }

    var status by remember(selectedUri) {
        mutableStateOf(
            if (selectedUri == null)
                "Open a GLB model"
            else
                "Loading..."
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        SceneView(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            environmentLoader = environmentLoader
        ) {

            modelInstance?.let { instance ->

                status = "GLB loaded ✓"

                ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1f,
                    autoAnimate = true
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = status,
                color = Color.Black
            )

            Spacer(Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpen
            ) {
                Text("OPEN GLB")
            }
        }
    }
}
