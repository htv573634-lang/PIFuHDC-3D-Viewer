package com.pifuhdc.viewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Choreographer
import android.view.TextureView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.google.android.filament.utils.ModelViewer

import java.nio.ByteBuffer

class MainActivity : Activity() {

    private lateinit var modelViewer: ModelViewer
    private lateinit var statusText: TextView

    private val choreographer =
        Choreographer.getInstance()

    private val frameCallback =
        object : Choreographer.FrameCallback {

            override fun doFrame(
                frameTimeNanos: Long
            ) {
                if (::modelViewer.isInitialized) {
                    modelViewer.render(frameTimeNanos)
                }

                choreographer.postFrameCallback(this)
            }
        }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        createViewer()

        handleIntent(intent)
    }

    private fun createViewer() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(
                16,
                12,
                16,
                12
            )
        }

        val openButton = Button(this).apply {
            text = "Open GLB"

            setOnClickListener {
                openGlbPicker()
            }
        }

        statusText = TextView(this).apply {
            text = "No model loaded"
            setPadding(
                16,
                0,
                8,
                0
            )
        }

        toolbar.addView(
            openButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        toolbar.addView(
            statusText,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                2f
            )
        )

        val textureView =
            TextureView(this)

        modelViewer =
            ModelViewer(textureView)

        textureView.setOnTouchListener(
            modelViewer
        )

        root.addView(
            toolbar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            textureView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()

        if (::modelViewer.isInitialized) {
            choreographer.postFrameCallback(
                frameCallback
            )
        }
    }

    override fun onPause() {

        choreographer.removeFrameCallback(
            frameCallback
        )

        super.onPause()
    }

    override fun onDestroy() {

        choreographer.removeFrameCallback(
            frameCallback
        )

        if (::modelViewer.isInitialized) {
            modelViewer.destroy()
        }

        super.onDestroy()
    }

    override fun onNewIntent(
        intent: Intent?
    ) {
        super.onNewIntent(intent)

        if (intent != null) {
            setIntent(intent)
            handleIntent(intent)
        }
    }

    private fun handleIntent(
        intent: Intent
    ) {

        if (
            intent.action == Intent.ACTION_VIEW &&
            intent.data != null
        ) {
            loadGlb(intent.data!!)
        }
    }

    private fun openGlbPicker() {

        val pickerIntent =
            Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )

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

        startActivityForResult(
            pickerIntent,
            REQUEST_GLB
        )
    }

    @Deprecated(
        "Activity Result API will be used in a later revision."
    )
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
            resultCode == RESULT_OK
        ) {

            data?.data?.let { uri ->
                loadGlb(uri)
            }
        }
    }

    private fun loadGlb(
        uri: Uri
    ) {

        try {

            statusText.text =
                "Reading GLB..."

            val bytes =
                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->
                        input.readBytes()
                    }
                    ?: throw IllegalStateException(
                        "Unable to open file"
                    )

            if (bytes.size < 12) {
                throw IllegalArgumentException(
                    "File is too small"
                )
            }

            val magic =
                String(
                    bytes,
                    0,
                    4,
                    Charsets.US_ASCII
                )

            if (magic != "glTF") {
                throw IllegalArgumentException(
                    "Not a valid GLB file"
                )
            }

            val version =
                ByteBuffer
                    .wrap(bytes)
                    .getInt(4)

            if (version != 2) {
                throw IllegalArgumentException(
                    "Unsupported GLB version: $version"
                )
            }

            val sizeMb =
                bytes.size /
                    1024.0 /
                    1024.0

            statusText.text =
                "Loading %.1f MB...".format(sizeMb)

            modelViewer.loadModelGlb(
                ByteBuffer.wrap(bytes)
            )

            modelViewer.transformToUnitCube()

            statusText.text =
                "Model loaded"

        } catch (e: Exception) {

            statusText.text =
                "Load failed"

            Toast.makeText(
                this,
                "GLB error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {

        private const val REQUEST_GLB = 7001
    }
}
