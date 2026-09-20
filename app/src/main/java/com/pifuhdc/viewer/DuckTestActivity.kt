package com.pifuhdc.viewer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.TextureView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder

class DuckTestActivity : Activity() {

    private lateinit var textureView: TextureView
    private lateinit var statusText: TextView

    private var modelViewer: ModelViewer? = null
    private var rendering = false

    companion object {
        private const val REQUEST_GLB = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Utils.init()

        createUi()

        textureView.post {
            initializeViewer()
        }
    }

    private fun createUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        textureView = TextureView(this).apply {
            setBackgroundColor(Color.BLACK)
        }

        root.addView(
            textureView,
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
            text = "Starting renderer..."
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
            modelViewer = ModelViewer(textureView)

            statusText.text = "Renderer ready • Open Duck.glb"

            startRendering()

        } catch (e: Exception) {
            statusText.text =
                "Renderer error: ${e.message}"
        }
    }

    private fun openGlb() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {

            addCategory(Intent.CATEGORY_OPENABLE)

            type = "model/gltf-binary"

            putExtra(
                Intent.EXTRA_MIME_TYPES,
                arrayOf(
                    "model/gltf-binary",
                    "model/gltf+json",
                    "application/octet-stream"
                )
            )
        }

        startActivityForResult(
            intent,
            REQUEST_GLB
        )
    }

    @Deprecated("Deprecated Android API retained for compatibility")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

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
            statusText.text = "Reading GLB..."

            val bytes = contentResolver
                .openInputStream(uri)
                ?.use { input ->
                    input.readBytes()
                }
                ?: throw Exception(
                    "Unable to read selected file"
                )

            if (bytes.size < 12) {
                throw Exception(
                    "File is too small to be a GLB"
                )
            }

            val buffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)

            val magic = buffer.int
            val version = buffer.int
            val declaredLength = buffer.int

            if (magic != 0x46546C67) {
                throw Exception(
                    "Not a GLB file"
                )
            }

            if (version != 2) {
                throw Exception(
                    "Unsupported GLB version: $version"
                )
            }

            if (declaredLength > bytes.size) {
                throw Exception(
                    "Invalid GLB length: " +
                        "$declaredLength / ${bytes.size}"
                )
            }

            buffer.position(0)

            val viewer = modelViewer
                ?: throw Exception(
                    "ModelViewer not initialized"
                )

            viewer.loadModelGlb(buffer)

            viewer.transformToUnitCube()

            statusText.text =
                "GLB loaded • rendering"

        } catch (e: Exception) {

            statusText.text =
                "GLB error: ${e.message}"
        }
    }

    private fun startRendering() {

        if (rendering) {
            return
        }

        rendering = true

        Choreographer
            .getInstance()
            .postFrameCallback(frameCallback)
    }

    private val frameCallback =
        object : Choreographer.FrameCallback {

            override fun doFrame(
                frameTimeNanos: Long
            ) {

                if (!rendering) {
                    return
                }

                try {

                    modelViewer?.render(
                        frameTimeNanos
                    )

                } catch (e: Exception) {

                    statusText.text =
                        "Render error: ${e.message}"
                }

                Choreographer
                    .getInstance()
                    .postFrameCallback(this)
            }
        }

    override fun onResume() {
        super.onResume()

        if (modelViewer != null) {
            startRendering()
        }
    }

    override fun onPause() {

        rendering = false

        Choreographer
            .getInstance()
            .removeFrameCallback(
                frameCallback
            )

        super.onPause()
    }

    override fun onDestroy() {

        rendering = false

        Choreographer
            .getInstance()
            .removeFrameCallback(
                frameCallback
            )

        modelViewer?.destroy()
        modelViewer = null

        super.onDestroy()
    }
}
