package com.pifuhdc.viewer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import io.github.sceneview.SceneView
import io.github.sceneview.createDefaultCameraManipulator
import io.github.sceneview.loaders.ModelLoader
import io.github.sceneview.math.Position
import io.github.sceneview.node.ModelNode

class DuckTestActivity : Activity() {

    private lateinit var sceneView: SceneView
    private lateinit var statusText: TextView
    private lateinit var modelLoader: ModelLoader

    private var modelNode: ModelNode? = null

    companion object {
        private const val REQUEST_GLB = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createUi()

        sceneView.post {
            initializeViewer()
        }
    }

    private fun createUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        sceneView = SceneView(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        root.addView(
            sceneView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val bottomPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(24, 12, 24, 12)
        }

        statusText = TextView(this).apply {
            text = "Starting SceneView..."
            textSize = 16f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        val openButton = Button(this).apply {
            text = "OPEN GLB"

            setOnClickListener {
                openGlb()
            }
        }

        bottomPanel.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        bottomPanel.addView(
            openButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            bottomPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                220
            )
        )

        setContentView(root)
    }

    private fun initializeViewer() {
        try {
            modelLoader = ModelLoader(sceneView.engine)

            sceneView.cameraManipulator =
                createDefaultCameraManipulator(
                    orbitRadius = 2.0f,
                    targetPosition = Position(0f, 0f, 0f)
                )

            statusText.text = "SceneView ready • Open Duck.glb"

        } catch (e: Exception) {
            statusText.text = "Viewer error: ${e.message}"
        }
    }

    private fun openGlb() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"

            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "model/gltf-binary",
                    "model/gltf+json",
                    "application/octet-stream"
                )
            )
        }

        startActivityForResult(intent, REQUEST_GLB)
    }

    @Deprecated("Deprecated Android API retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (
            requestCode == REQUEST_GLB &&
            resultCode == RESULT_OK &&
            data?.data != null
        ) {
            loadGlb(data.data!!)
        }
    }

    private fun loadGlb(uri: Uri) {
        try {
            statusText.text = "Loading GLB..."

            modelNode?.let {
                sceneView.removeChildNode(it)
            }

            modelLoader.loadModelInstanceAsync(uri.toString()) { instance ->

                val node = ModelNode(
                    modelInstance = instance,
                    scaleToUnits = 1.0f,
                    autoAnimate = true,
                    centerOrigin = Position(0f, 0f, 0f)
                )

                sceneView.addChildNode(node)
                modelNode = node

                statusText.post {
                    statusText.text = "GLB loaded • Orbit • Zoom • Pan"
                }
            }

        } catch (e: Exception) {
            statusText.text = "GLB error: ${e.message}"
        }
    }

    override fun onDestroy() {
        modelNode = null
        super.onDestroy()
    }
}
