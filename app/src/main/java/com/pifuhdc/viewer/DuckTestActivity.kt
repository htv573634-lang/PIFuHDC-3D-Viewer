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
import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.Skybox
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
            setOnClickListener { openGlb() }
        }

        bottomPanel.addView(
            statusText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
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
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        setContentView(root)
    }

    private fun initializeViewer() {
        try {
            val viewer = ModelViewer(textureView)
            modelViewer = viewer

            viewer.scene.skybox = Skybox.Builder()
                .color(floatArrayOf(0.95f, 0.95f, 1.0f, 1.0f))
                .build(viewer.engine)

            val sun = EntityManager.get().create()

            LightManager.Builder(LightManager.Type.DIRECTIONAL)
                .color(1.0f, 1.0f, 1.0f)
                .intensity(120000f)
                .direction(-0.6f, -1.0f, -0.8f)
                .castShadows(true)
                .build(viewer.engine, sun)

            viewer.scene.addEntity(sun)

            statusText.text = "Renderer ready • Open Duck.glb"

            startRendering()

        } catch (e: Exception) {
            statusText.text = "Renderer error: ${e.message}"
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
                    "application/octet-stream",
                    "*/*"
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
            statusText.text = "Reading GLB..."

            val bytes = contentResolver
                .openInputStream(uri)
                ?.use { it.readBytes() }
                ?: throw Exception("Unable to read file")

            if (bytes.size < 12) {
                throw Exception("Invalid GLB")
            }

            val buffer = ByteBuffer
                .wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)

            val magic = buffer.int
            val version = buffer.int
            val length = buffer.int

            if (magic != 0x46546C67) {
                throw Exception("Not GLB")
            }

            if (version != 2) {
                throw Exception("Unsupported GLB version: $version")
            }

            if (length > bytes.size) {
                throw Exception("Corrupted GLB")
            }

            buffer.position(0)

            val viewer = modelViewer
                ?: throw Exception("Renderer not initialized")

            viewer.loadModelGlb(buffer)
            viewer.transformToUnitCube()

            statusText.text = "GLB loaded ✓"

        } catch (e: Exception) {
            statusText.text = "GLB error: ${e.message}"
        }
    }

    private fun startRendering() {
        if (rendering) return

        rendering = true
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    private val frameCallback =
        object : Choreographer.FrameCallback {

            override fun doFrame(frameTimeNanos: Long) {

                if (!rendering) return

                try {
                    modelViewer?.render(frameTimeNanos)
                } catch (e: Exception) {
                    statusText.text = "Render error: ${e.message}"
                }

                Choreographer.getInstance().postFrameCallback(this)
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
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        super.onPause()
    }

    override fun onDestroy() {
        rendering = false
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        modelViewer?.destroy()
        modelViewer = null
        super.onDestroy()
    }
}
