package com.pifuhdc.viewer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import kotlin.math.max

class MainActivity : Activity() {

    companion object {
        private const val TAG = "PIFuHDC_VIEWER"
        private const val REQUEST_GLB = 7001
    }

    private lateinit var modelViewer: ModelViewer
    private lateinit var textureView: TextureView
    private lateinit var statusText: TextView

    private var renderingStarted = false
    private var modelLoaded = false

    private val choreographer =
        Choreographer.getInstance()

    /*
     * ------------------------------------------------
     * FRAME CALLBACK
     * ------------------------------------------------
     */

    private val frameCallback =
        object : Choreographer.FrameCallback {

            override fun doFrame(
                frameTimeNanos: Long
            ) {

                if (::modelViewer.isInitialized) {

                    try {
                        modelViewer.render(
                            frameTimeNanos
                        )
                    } catch (e: Exception) {

                        Log.e(
                            TAG,
                            "Render error",
                            e
                        )
                    }
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
     * CREATE ACTIVITY
     * ------------------------------------------------
     */

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        Utils.init()

        createUi()
    }

    /*
     * ------------------------------------------------
     * UI
     * ------------------------------------------------
     */

    private fun createUi() {

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
         * 3D VIEW
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
         * BOTTOM PANEL
         * ------------------------------------------------
         */

        val bottomPanel =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER

                setPadding(
                    12,
                    6,
                    12,
                    10
                )

                setBackgroundColor(
                    Color.WHITE
                )
            }

        statusText =
            TextView(this).apply {

                text =
                    "Initializing..."

                gravity =
                    Gravity.CENTER

                setTextColor(
                    Color.DKGRAY
                )

                textSize =
                    13f

                setPadding(
                    4,
                    2,
                    4,
                    6
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
         * ModelViewer must be created after
         * TextureView has been attached.
         */

        textureView.post {

            initializeViewer()
        }
    }

    /*
     * ------------------------------------------------
     * FILAMENT VIEWER
     * ------------------------------------------------
     */

    private fun initializeViewer() {

        try {

            modelViewer =
                ModelViewer(
                    textureView
                )

            /*
             * Touch controls:
             *
             * drag  = rotate
             * pinch = zoom
             */
            textureView.setOnTouchListener(
                modelViewer
            )

            setupEnvironment()

            startRendering()

            statusText.text =
                "Ready - Open a GLB"

            Log.d(
                TAG,
                "ModelViewer initialized"
            )

            handleIntent(intent)

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Viewer initialization failed",
                e
            )

            statusText.text =
                "Viewer initialization failed"

            Toast.makeText(
                this,
                "Filament: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * ------------------------------------------------
     * ENVIRONMENT
     * ------------------------------------------------
     */

    private fun setupEnvironment() {

        val engine =
            modelViewer.engine

        /*
         * Neutral indirect light.
         */

        try {

            val indirectLight =
                IndirectLight.Builder()
                    .intensity(
                        50_000.0f
                    )
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
                    .build(
                        engine
                    )

            modelViewer.scene.indirectLight =
                indirectLight

            Log.d(
                TAG,
                "IndirectLight installed"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "IndirectLight failed",
                e
            )
        }

        /*
         * Dark gray background.
         * This makes a white/light Duck
         * easy to see.
         */

        try {

            val skybox =
                Skybox.Builder()
                    .color(
                        0.06f,
                        0.06f,
                        0.06f,
                        1.0f
                    )
                    .build(
                        engine
                    )

            modelViewer.scene.skybox =
                skybox

            Log.d(
                TAG,
                "Skybox installed"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Skybox failed",
                e
            )
        }

        /*
         * FXAA.
         */

        try {

            modelViewer.view.antiAliasing =
                View.AntiAliasing.FXAA

        } catch (e: Exception) {

            Log.w(
                TAG,
                "Could not enable FXAA",
                e
            )
        }
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

        Log.d(
            TAG,
            "Render loop STARTED"
        )
    }

    private fun stopRendering() {

        renderingStarted = false

        choreographer.removeFrameCallback(
            frameCallback
        )

        Log.d(
            TAG,
            "Render loop STOPPED"
        )
    }

    /*
     * ------------------------------------------------
     * GLB PICKER
     * ------------------------------------------------
     */

    private fun openGlbPicker() {

        val intent =
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
            intent,
            REQUEST_GLB
        )
    }

    /*
     * ------------------------------------------------
     * PICKER RESULT
     * ------------------------------------------------
     */

    @Deprecated(
        "Using Activity Result API later"
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
            requestCode ==
                REQUEST_GLB &&
            resultCode ==
                RESULT_OK
        ) {

            data?.data?.let {

                loadGlb(it)
            }
        }
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

            if (
                ::modelViewer.isInitialized
            ) {

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
     * LOAD GLB
     * ------------------------------------------------
     */

    private fun loadGlb(
        uri: Uri
    ) {

        if (
            !::modelViewer.isInitialized
        ) {

            Toast.makeText(
                this,
                "Viewer not ready",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        modelLoaded = false

        try {

            statusText.text =
                "Reading GLB..."

            /*
             * Read file.
             */

            val bytes =
                contentResolver
                    .openInputStream(uri)
                    ?.use {
                        it.readBytes()
                    }
                    ?: throw IllegalStateException(
                        "Cannot read file"
                    )

            Log.d(
                TAG,
                "GLB bytes = ${bytes.size}"
            )

            /*
             * ------------------------------------------------
             * HEADER
             * ------------------------------------------------
             */

            if (bytes.size < 12) {

                throw IllegalArgumentException(
                    "File is smaller than 12 bytes"
                )
            }

            val magic =
                String(
                    bytes,
                    0,
                    4,
                    Charsets.US_ASCII
                )

            Log.d(
                TAG,
                "GLB magic = $magic"
            )

            if (magic != "glTF") {

                throw IllegalArgumentException(
                    "Invalid GLB magic: $magic"
                )
            }

            val header =
                ByteBuffer
                    .wrap(bytes)
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            val version =
                header.getInt(4)

            val declaredLength =
                header.getInt(8)

            Log.d(
                TAG,
                "GLB version = $version"
            )

            Log.d(
                TAG,
                "GLB declared length = $declaredLength"
            )

            if (version != 2) {

                throw IllegalArgumentException(
                    "Unsupported GLB version: $version"
                )
            }

            if (
                declaredLength < 12 ||
                declaredLength > bytes.size
            ) {

                throw IllegalArgumentException(
                    "Invalid GLB declared length"
                )
            }

            /*
             * ------------------------------------------------
             * LOAD
             * ------------------------------------------------
             */

            statusText.text =
                "Parsing GLB..."

            val buffer =
                ByteBuffer
                    .wrap(bytes)
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            /*
             * ModelViewer internally:
             *
             * 1. creates FilamentAsset
             * 2. begins ResourceLoader
             * 3. asyncUpdateLoad() runs every frame
             * 4. ready renderables are added
             *
             * This is how the official implementation works.
             */

            modelViewer.loadModelGlb(
                buffer
            )

            /*
             * DO NOT immediately declare it rendered.
             */

            statusText.text =
                "GLB parsed - waiting for resources..."

            Log.d(
                TAG,
                "loadModelGlb() returned"
            )

            startRendering()

            /*
             * Give Filament several frames to populate
             * the scene before collecting diagnostics.
             */

            textureView.postDelayed(
                {

                    inspectModel()

                },
                1000L
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "GLB LOAD FAILED",
                e
            )

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
     * DIAGNOSTIC INSPECTION
     * ------------------------------------------------
     */

    private fun inspectModel() {

        if (
            !::modelViewer.isInitialized
        ) {
            return
        }

        val asset =
            modelViewer.asset

        if (asset == null) {

            Log.e(
                TAG,
                "DIAGNOSTIC: asset = NULL"
            )

            statusText.text =
                "ERROR: Asset = NULL"

            return
        }

        /*
         * Entity count.
         */

        val entityCount =
            asset.entityCount

        /*
         * Renderable entities.
         *
         * FilamentAsset exposes these entities
         * from the glTF asset.
         */

        val renderableEntities =
            asset.renderableEntities

        val renderableCount =
            renderableEntities.size

        /*
         * Scene count.
         */

        val sceneRenderableCount =
            modelViewer.scene.renderableCount

        /*
         * Bounding box.
         */

        val bounds =
            asset.boundingBox

        val centerX =
            bounds.center[0]

        val centerY =
            bounds.center[1]

        val centerZ =
            bounds.center[2]

        val halfX =
            bounds.halfExtent[0]

        val halfY =
            bounds.halfExtent[1]

        val halfZ =
            bounds.halfExtent[2]

        val width =
            halfX * 2.0f

        val height =
            halfY * 2.0f

        val depth =
            halfZ * 2.0f

        Log.d(
            TAG,
            "========== GLB DIAGNOSTIC =========="
        )

        Log.d(
            TAG,
            "Entity count = $entityCount"
        )

        Log.d(
            TAG,
            "Renderable entity count = $renderableCount"
        )

        Log.d(
            TAG,
            "Scene renderable count = $sceneRenderableCount"
        )

        Log.d(
            TAG,
            "BoundingBox center = ($centerX, $centerY, $centerZ)"
        )

        Log.d(
            TAG,
            "BoundingBox size = ($width, $height, $depth)"
        )

        Log.d(
            TAG,
            "Viewport = ${textureView.width} x ${textureView.height}"
        )

        Log.d(
            TAG,
            "===================================="
        )

        /*
         * ------------------------------------------------
         * RENDERABLE DETAILS
         * ------------------------------------------------
         */

        if (renderableCount > 0) {

            val engine =
                modelViewer.engine

            val renderableManager =
                engine.renderableManager

            for (
                entity in renderableEntities
            ) {

                try {

                    val instance =
                        renderableManager
                            .getInstance(entity)

                    val primitiveCount =
                        renderableManager
                            .getPrimitiveCount(
                                instance
                            )

                    Log.d(
                        TAG,
                        "Renderable entity=$entity primitives=$primitiveCount"
                    )

                    /*
                     * Print material information.
                     */

                    for (
                        primitive in
                        0 until primitiveCount
                    ) {

                        try {

                            val material =
                                renderableManager
                                    .getMaterialInstanceAt(
                                        instance,
                                        primitive
                                    )

                            Log.d(
                                TAG,
                                "  primitive=$primitive material=$material"
                            )

                        } catch (e: Exception) {

                            Log.w(
                                TAG,
                                "  Material inspection failed",
                                e
                            )
                        }
                    }

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Renderable inspection failed",
                        e
                    )
                }
            }
        }

        /*
         * ------------------------------------------------
         * CAMERA DIAGNOSTIC
         * ------------------------------------------------
         */

        try {

            val camera =
                modelViewer.camera

            val position =
                camera.getPosition()

            Log.d(
                TAG,
                "Camera position = " +
                    "(${position[0]}, " +
                    "${position[1]}, " +
                    "${position[2]})"
            )

            val projection =
                camera.getProjectionMatrix()

            Log.d(
                TAG,
                "Camera projection matrix available = " +
                    (projection.size >= 16)
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Camera diagnostic failed",
                e
            )
        }

        /*
         * ------------------------------------------------
         * FORCE MODEL INTO VIEW
         * ------------------------------------------------
         */

        try {

            modelViewer.transformToUnitCube()

            Log.d(
                TAG,
                "transformToUnitCube() applied"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "transformToUnitCube failed",
                e
            )
        }

        /*
         * ------------------------------------------------
         * RESULT
         * ------------------------------------------------
         */

        if (
            renderableCount > 0 &&
            sceneRenderableCount > 0
        ) {

            statusText.text =
                "VISIBLE DATA • Entities $entityCount • Renderables $renderableCount"

            modelLoaded = true

            Log.d(
                TAG,
                "RESULT: Filament has renderables in scene"
            )

        } else if (
            renderableCount > 0
        ) {

            statusText.text =
                "RENDERABLES FOUND • Scene $sceneRenderableCount"

            Log.e(
                TAG,
                "RESULT: Asset has renderables but scene has none"
            )

        } else {

            statusText.text =
                "NO RENDERABLES • Entities $entityCount"

            Log.e(
                TAG,
                "RESULT: Asset contains NO renderable entities"
            )
        }

        startRendering()
    }

    /*
     * ------------------------------------------------
     * LIFECYCLE
     * ------------------------------------------------
     */

    override fun onResume() {

        super.onResume()

        if (
            ::modelViewer.isInitialized
        ) {

            startRendering()
        }
    }

    override fun onPause() {

        stopRendering()

        super.onPause()
    }

    override fun onDestroy() {

        stopRendering()

        if (
            ::modelViewer.isInitialized
        ) {

            try {

                modelViewer.destroy()

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "ModelViewer destroy failed",
                    e
                )
            }
        }

        super.onDestroy()
    }
}
