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
import android.widget.Toast

import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : Activity() {

    private lateinit var modelViewer: ModelViewer
    private lateinit var textureView: TextureView
    private lateinit var statusText: TextView

    private var renderingStarted = false

    private val choreographer =
        Choreographer.getInstance()

    /*
     * ------------------------------------------------
     * FRAME LOOP
     * ------------------------------------------------
     */

    private val frameCallback =
        object : Choreographer.FrameCallback {

            override fun doFrame(
                frameTimeNanos: Long
            ) {

                if (::modelViewer.isInitialized) {

                    modelViewer.render(
                        frameTimeNanos
                    )
                }

                if (renderingStarted) {

                    choreographer.postFrameCallback(
                        this
                    )
                }
            }
        }

    /*
     * ------------------------------------------------
     * ACTIVITY
     * ------------------------------------------------
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        /*
         * Required by Filament.
         */
        Utils.init()

        createUserInterface()
    }

    /*
     * ------------------------------------------------
     * USER INTERFACE
     * ------------------------------------------------
     */

    private fun createUserInterface() {

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.BLACK
                )
            }

        /*
         * ------------------------------------------------
         * TOP 3D VIEW
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
         * BOTTOM CONTROL PANEL
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
                    Color.WHITE
                )
            }

        statusText =
            TextView(this).apply {

                text =
                    "Initializing 3D viewer..."

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.DKGRAY
                )

                textSize =
                    14f

                setPadding(
                    8,
                    4,
                    8,
                    8
                )
            }

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
         * TextureView must be attached before
         * creating ModelViewer.
         */

        textureView.post {

            initializeFilament()
        }
    }

    /*
     * ------------------------------------------------
     * FILAMENT INITIALIZATION
     * ------------------------------------------------
     */

    private fun initializeFilament() {

        try {

            modelViewer =
                ModelViewer(textureView)

            /*
             * Enable touch rotation / zoom.
             */
            textureView.setOnTouchListener(
                modelViewer
            )

            /*
             * Configure environment lighting.
             */
            createNeutralEnvironment()

            /*
             * Rendering must start AFTER
             * ModelViewer exists.
             */
            startRendering()

            statusText.text =
                "Ready - select a GLB"

            /*
             * Handle an Open-With GLB if one
             * was supplied when launching.
             */
            handleIntent(intent)

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

    /*
     * ------------------------------------------------
     * NEUTRAL LIGHTING / ENVIRONMENT
     * ------------------------------------------------
     *
     * This does NOT require an external KTX file.
     *
     * We create:
     *
     * 1. Indirect light
     * 2. Neutral skybox
     *
     * The ModelViewer already supplies a direct
     * directional/sun light.
     */

    private fun createNeutralEnvironment() {

        val engine =
            modelViewer.engine

        /*
         * ------------------------------------------------
         * INDIRECT LIGHT
         * ------------------------------------------------
         */

        val indirectLight =
            IndirectLight.Builder()
                .intensity(30_000.0f)
                .radiance(
                    1,
                    floatArrayOf(
                        1.0f,
                        1.0f,
                        1.0f
                    )
                )
                .irradiance(
                    1,
                    floatArrayOf(
                        1.0f,
                        1.0f,
                        1.0f
                    )
                )
                .build(engine)

        modelViewer.scene.indirectLight =
            indirectLight

        /*
         * Keep reference so ModelViewer.destroy()
         * can clean up correctly.
         *
         * This is a simple constant environment,
         * so there is no external texture.
         */

        /*
         * ------------------------------------------------
         * SKYBOX
         * ------------------------------------------------
         *
         * Light gray background makes the model
         * easier to see.
         */

        val skybox =
            Skybox.Builder()
                .color(
                    0.08f,
                    0.08f,
                    0.08f,
                    1.0f
                )
                .build(engine)

        modelViewer.scene.skybox =
            skybox

        /*
         * Mobile-friendly rendering settings.
         */

        modelViewer.view.renderQuality =
            modelViewer.view.renderQuality.apply {

                hdrColorBuffer =
                    View.QualityLevel.MEDIUM
            }

        modelViewer.view.antiAliasing =
            View.AntiAliasing.FXAA
    }

    /*
     * ------------------------------------------------
     * RENDER LOOP
     * ------------------------------------------------
     */

    private fun startRendering() {

        if (renderingStarted) {
            return
        }

        renderingStarted = true

        choreographer.removeFrameCallback(
            frameCallback
        )

        choreographer.postFrameCallback(
            frameCallback
        )
    }

    private fun stopRendering() {

        renderingStarted = false

        choreographer.removeFrameCallback(
            frameCallback
        )
    }

    override fun onResume() {

        super.onResume()

        if (::modelViewer.isInitialized) {

            startRendering()
        }
    }

    override fun onPause() {

        stopRendering()

        super.onPause()
    }

    override fun onDestroy() {

        stopRendering()

        if (::modelViewer.isInitialized) {

            try {

                modelViewer.destroy()

            } catch (_: Exception) {
            }
        }

        super.onDestroy()
    }

    /*
     * ------------------------------------------------
     * OPEN GLB PICKER
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

                type =
                    "*/*"

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

    /*
     * ------------------------------------------------
     * OPEN-WITH SUPPORT
     * ------------------------------------------------
     */

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
        incomingIntent: Intent
    ) {

        if (
            incomingIntent.action ==
                Intent.ACTION_VIEW &&
            incomingIntent.data != null
        ) {

            val uri =
                incomingIntent.data!!

            if (::modelViewer.isInitialized) {

                loadGlb(uri)

            } else {

                textureView.post {

                    if (
                        ::modelViewer.isInitialized
                    ) {

                        loadGlb(uri)
                    }
                }
            }
        }
    }

    /*
     * ------------------------------------------------
     * PICKER RESULT
     * ------------------------------------------------
     */

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
     * GLB LOADING
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
             * Read the complete GLB.
             */

            val bytes =
                contentResolver
                    .openInputStream(uri)
                    ?.use { input ->

                        input.readBytes()

                    }
                    ?: throw IllegalStateException(
                        "Unable to open GLB file"
                    )

            /*
             * ------------------------------------------------
             * VALIDATE GLB HEADER
             * ------------------------------------------------
             */

            if (bytes.size < 12) {

                throw IllegalArgumentException(
                    "File is smaller than a GLB header"
                )
            }

            /*
             * Magic = glTF
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
                    "This is not a valid GLB file"
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
             * ------------------------------------------------
             * CREATE BUFFER
             * ------------------------------------------------
             */

            val glbBuffer =
                ByteBuffer
                    .wrap(bytes)
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            /*
             * ------------------------------------------------
             * LOAD GLB
             * ------------------------------------------------
             *
             * Filament loads resources asynchronously.
             * The render loop will call asyncUpdateLoad()
             * internally and populate the scene when
             * resources become ready.
             */

            modelViewer.loadModelGlb(
                glbBuffer
            )

            /*
             * Set initial model transform.
             */
            modelViewer.transformToUnitCube()

            /*
             * Make absolutely sure the render loop
             * remains active.
             */
            startRendering()

            statusText.text =
                "Loading model..."

            /*
             * Monitor asynchronous resource loading.
             */
            monitorModelLoading()

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

    /*
     * ------------------------------------------------
     * ASYNC MODEL LOADING MONITOR
     * ------------------------------------------------
     *
     * ModelViewer loads GLB resources asynchronously.
     * We watch progress and re-apply the transform after
     * resources become available.
     */

    private fun monitorModelLoading() {

        val checkRunnable =
            object : Runnable {

                override fun run() {

                    if (
                        !::modelViewer.isInitialized
                    ) {
                        return
                    }

                    val progress =
                        modelViewer.progress

                    if (progress < 1.0f) {

                        statusText.text =
                            "Loading model... %d%%"
                                .format(
                                    (progress * 100.0f)
                                        .toInt()
                                )

                        textureView.postDelayed(
                            this,
                            100L
                        )

                    } else {

                        /*
                         * Resources are ready.
                         *
                         * Apply transform again now that
                         * the asset is fully loaded.
                         */
                        try {

                            modelViewer
                                .transformToUnitCube()

                            statusText.text =
                                "Model loaded"

                        } catch (e: Exception) {

                            statusText.text =
                                "Model loaded with transform warning"
                        }

                        startRendering()
                    }
                }
            }

        textureView.post(
            checkRunnable
        )
    }

    companion object {

        private const val REQUEST_GLB =
            7001
    }
}
