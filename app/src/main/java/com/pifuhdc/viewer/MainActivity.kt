package com.pifuhdc.viewer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Choreographer
import android.view.Gravity
import android.view.TextureView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : Activity() {

    private lateinit var modelViewer: ModelViewer
    private lateinit var statusText: TextView
    private lateinit var textureView: TextureView

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

        Utils.init()

        createViewer()

        handleIntent(intent)
    }

    private fun createViewer() {

        /*
         * Main screen
         *
         * Upper area:
         *     3D GLB viewer
         *
         * Bottom area:
         *     Status
         *     Open GLB button
         */

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    android.graphics.Color.BLACK
                )
            }

        /*
         * ------------------------------------------------
         * 3D VIEWER
         * ------------------------------------------------
         */

        textureView =
            TextureView(this)

        root.addView(
            textureView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.82f
            )
        )

        /*
         * ------------------------------------------------
         * BOTTOM CONTROL AREA
         * ------------------------------------------------
         */

        val bottomPanel =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    16,
                    8,
                    16,
                    12
                )

                setBackgroundColor(
                    android.graphics.Color.WHITE
                )
            }

        /*
         * Status text
         */

        statusText =
            TextView(this).apply {

                text =
                    "No model loaded"

                gravity =
                    Gravity.CENTER

                setTextColor(
                    android.graphics.Color.DKGRAY
                )

                textSize = 14f

                setPadding(
                    8,
                    4,
                    8,
                    8
                )
            }

        /*
         * Open GLB button
         */

        val openButton =
            Button(this).apply {

                text =
                    "Open GLB"

                setOnClickListener {
                    openGlbPicker()
                }
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
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        /*
         * Bottom panel occupies remaining space.
         */

        root.addView(
            bottomPanel,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                0.18f
            )
        )

        setContentView(root)

        /*
         * Create Filament ModelViewer only after
         * TextureView is attached to the window.
         */

        textureView.post {

            try {

                modelViewer =
                    ModelViewer(textureView)

                textureView.setOnTouchListener(
                    modelViewer
                )

                statusText.text =
                    "No model loaded"

            } catch (e: Exception) {

                statusText.text =
                    "Viewer initialization failed"

                Toast.makeText(
                    this,
                    "Filament error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
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

    /*
     * ------------------------------------------------
     * OPEN WITH / FILE INTENT
     * ------------------------------------------------
     */

    private fun handleIntent(
        intent: Intent
    ) {

        if (
            intent.action ==
                Intent.ACTION_VIEW &&
            intent.data != null
        ) {

            loadGlb(
                intent.data!!
            )
        }
    }

    /*
     * ------------------------------------------------
     * GLB FILE PICKER
     * ------------------------------------------------
     */

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

        @Suppress("DEPRECATION")
        startActivityForResult(
            pickerIntent,
            REQUEST_GLB
        )
    }

    @Deprecated(
        "Activity Result API will be used later."
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

    /*
     * ------------------------------------------------
     * GLB LOADER
     * ------------------------------------------------
     */

    private fun loadGlb(
        uri: Uri
    ) {

        if (!::modelViewer.isInitialized) {

            Toast.makeText(
                this,
                "Viewer is still initializing.",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        try {

            statusText.text =
                "Reading GLB..."

            /*
             * Read selected file.
             */

            val bytes =
                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->
                        input.readBytes()
                    }
                    ?: throw IllegalStateException(
                        "Unable to open file"
                    )

            /*
             * GLB header is 12 bytes:
             *
             * 0-3   magic
             * 4-7   version
             * 8-11  total length
             */

            if (bytes.size < 12) {

                throw IllegalArgumentException(
                    "File is too small"
                )
            }

            /*
             * Check magic.
             */

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

            /*
             * GLB integers are LITTLE-ENDIAN.
             */

            val header =
                ByteBuffer
                    .wrap(bytes)
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            val version =
                header.getInt(4)

            if (version != 2) {

                throw IllegalArgumentException(
                    "Unsupported GLB version: $version"
                )
            }

            /*
             * Validate declared GLB size.
             */

            val declaredLength =
                header.getInt(8)

            if (
                declaredLength < 12 ||
                declaredLength > bytes.size
            ) {

                throw IllegalArgumentException(
                    "Invalid GLB length: $declaredLength"
                )
            }

            val sizeMb =
                bytes.size /
                    1024.0 /
                    1024.0

            statusText.text =
                "Loading %.1f MB..."
                    .format(sizeMb)

            /*
             * Pass GLB to Filament.
             */

            val glbBuffer =
                ByteBuffer
                    .wrap(bytes)
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            modelViewer.loadModelGlb(
                glbBuffer
            )

            /*
             * Automatically fit the model.
             */

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
