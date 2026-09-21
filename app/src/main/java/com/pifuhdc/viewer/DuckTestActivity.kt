package com.pifuhdc.viewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberModelInstance
import io.github.sceneview.SceneView
import io.github.sceneview.node.ModelNode

class DuckTestActivity : ComponentActivity() {

    // Modern Activity Result API for picking files securely
    private val glbPicker = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        selectedUri = uri
    }

    private var selectedUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        
                        // 3D Viewer Area
                        Box(modifier = Modifier.weight(1f)) {
                            if (selectedUri != null) {
                                GLBViewer(uri = selectedUri!!)
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No model loaded. Tap 'OPEN GLB' to select a file.")
                                }
                            }
                        }

                        // Bottom Control Panel
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = selectedUri?.lastPathSegment ?: "Ready to load GLB",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Button(onClick = { 
                                    // Request specifically GLB/GLTF mime types
                                    glbPicker.launch(arrayOf("model/gltf-binary", "application/octet-stream")) 
                                }) {
                                    Text("OPEN GLB")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GLBViewer(uri: Uri) {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    
    // SceneView automatically handles the surface lifecycle, rendering loop, and camera
    SceneView(
        modifier = Modifier.fillMaxSize(),
        engine = engine,
        modelLoader = modelLoader
    ) {
        // SceneView's ModelLoader can resolve content:// URIs directly [[40]]
        val modelInstance = rememberModelInstance(modelLoader, uri.toString())
        
        modelInstance?.let {
            // scaleToUnits = 1.0f automatically frames the model perfectly in the camera view
            ModelNode(
                modelInstance = it,
                scaleToUnits = 1.0f,
                autoAnimate = true
            )
        }
    }
}
