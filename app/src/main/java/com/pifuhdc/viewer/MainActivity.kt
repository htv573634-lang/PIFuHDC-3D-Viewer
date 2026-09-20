package com.pifuhdc.viewer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
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

import com.google.android.filament.EntityManager
import com.google.android.filament.LightManager
import com.google.android.filament.View
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils

import java.nio.ByteBuffer
import java.nio.ByteOrder

class MainActivity : Activity() {

    companion object {
        private const val TAG = "PIFuHDC_VIEWER"
        private const val REQUEST_GLB = 7001
    }

    private lateinit var modelViewer: ModelViewer
    private lateinit var textureView: TextureView
    private lateinit var statusText: TextView

    private var renderingStarted = false
    private var lightEntity = 0

    /*
     * Diagnostic state.
     */
    private var renderFrameCount = 0L
    private var successfulRenderCount = 0L
    private var frameCaptureRequested = false
    private var diagnosticCompleted = false
    private var modelReadyLogged = false
    private var lastProgressLogged = -1f

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

                    renderFrameCount++

                    try {

                        val rendered =
                            modelViewer.render(
                                frameTimeNanos
                            )

                        if (rendered) {
                            successfulRenderCount++
                        }

                        /*
                         * Log the first few frames so we know whether
                         * ModelViewer is actually getting a render surface.
                         */
                        if (
                            renderFrameCount <= 10L ||
                            renderFrameCount % 120L == 0L
                        ) {

                            Log.d(
                                TAG,
                                "FRAME #$renderFrameCount " +
                                    "rendered=$rendered " +
                                    "progress=${modelViewer.progress} " +
                                    "successful=$successfulRenderCount"
                            )
                        }

                        /*
                         * Track resource loading progress.
                         */
                        val progress =
                            modelViewer.progress

                        if (
                            progress != lastProgressLogged &&
                            (
                                progress == 0.0f ||
                                progress >= 1.0f ||
                                progress - lastProgressLogged >= 0.1f
                            )
                        ) {

                            lastProgressLogged =
                                progress

                            Log.d(
                                TAG,
                                "RESOURCE PROGRESS = $progress"
                            )
                        }

                        /*
                         * Once Filament reports that resources are
                         * completely loaded, inspect the scene and request
                         * one actual rendered frame capture.
                         */
                        if (
                            progress >= 1.0f &&
                            !modelReadyLogged
                        ) {

                            modelReadyLogged = true

                            Log.d(
                                TAG,
                                "================================"
                            )

                            Log.d(
                                TAG,
                                "RESOURCES FULLY LOADED"
                            )

                            Log.d(
                                TAG,
                                "Progress = $progress"
                            )

                            Log.d(
                                TAG,
                                "Render frames = $renderFrameCount"
                            )

                            Log.d(
                                TAG,
                                "Successful renders = $successfulRenderCount"
                            )

                            Log.d(
                                TAG,
                                "TextureView = " +
                                    "${textureView.width} x " +
                                    "${textureView.height}"
                            )

                            Log.d(
                                TAG,
                                "================================"
                            )

                            statusText.text =
                                "Resources loaded • capturing frame..."

                            /*
                             * Important:
                             * debugGetNextFrameCallback captures the
                             * NEXT rendered frame. We request it here,
                             * after the current render call.
                             */
                            requestFrameCapture()

                            /*
                             * Inspect scene/camera after the asynchronous
                             * resources have actually finished loading.
                             */
                            textureView.post {

                                inspectModel()
                            }
                        }

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
     * CREATE
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
                    "Initializing 3D viewer..."

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

        textureView.post {

            Log.d(
                TAG,
                "TextureView.post: size=" +
                    "${textureView.width} x " +
                    "${textureView.height}"
            )

            initializeViewer()
        }
    }

    /*
     * ------------------------------------------------
     * INITIALIZE FILAMENT
     * ------------------------------------------------
     */

    private fun initializeViewer() {

        try {

            Log.d(
                TAG,
                "Creating ModelViewer..."
            )

            Log.d(
                TAG,
                "TextureView dimensions before ModelViewer = " +
                    "${textureView.width} x " +
                    "${textureView.height}"
            )

            modelViewer =
                ModelViewer(
                    textureView
                )

            textureView.setOnTouchListener(
                modelViewer
            )

            setupEnvironment()

            createDirectionalLight()

            startRendering()

            statusText.text =
                "Ready - Open a GLB"

            Log.d(
                TAG,
                "ModelViewer initialized"
            )

            Log.d(
                TAG,
                "ModelViewer view viewport = " +
                    "${modelViewer.view.viewport.width} x " +
                    "${modelViewer.view.viewport.height}"
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
                "Filament error: ${e.message}",
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
         * Indirect light.
         */

        try {

            val indirectLight =
                com.google.android.filament.IndirectLight.Builder()
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
                "Indirect light installed"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Indirect light failed",
                e
            )
        }

        /*
         * Skybox.
         */

        try {

            val skybox =
                com.google.android.filament.Skybox.Builder()
                    .color(
                        0.05f,
                        0.05f,
                        0.05f,
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
         * Anti-aliasing.
         */

        try {

            modelViewer.view.antiAliasing =
                View.AntiAliasing.FXAA

            Log.d(
                TAG,
                "FXAA enabled"
            )

        } catch (e: Exception) {

            Log.w(
                TAG,
                "FXAA unavailable",
                e
            )
        }
    }

    /*
     * ------------------------------------------------
     * DIRECTIONAL LIGHT
     * ------------------------------------------------
     */

    private fun createDirectionalLight() {

        try {

            val engine =
                modelViewer.engine

            val entityManager =
                EntityManager.get()

            lightEntity =
                entityManager.create()

            LightManager.Builder(
                LightManager.Type.DIRECTIONAL
            )
                .color(
                    1.0f,
                    1.0f,
                    1.0f
                )
                .intensity(
                    100_000.0f
                )
                .direction(
                    0.0f,
                    -1.0f,
                    -1.0f
                )
                .castShadows(
                    false
                )
                .build(
                    engine,
                    lightEntity
                )

            modelViewer.scene.addEntity(
                lightEntity
            )

            Log.d(
                TAG,
                "Directional light installed"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Directional light creation failed",
                e
            )
        }
    }

    /*
     * ------------------------------------------------
     * RENDER OUTPUT CAPTURE
     * ------------------------------------------------
     */

    private fun requestFrameCapture() {

        if (frameCaptureRequested) {
            return
        }

        frameCaptureRequested = true

        Log.d(
            TAG,
            "Requesting next rendered frame capture..."
        )

        try {

            modelViewer.debugGetNextFrameCallback {

                bitmap ->

                try {

                    analyzeRenderedBitmap(
                        bitmap
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Frame bitmap analysis failed",
                        e
                    )

                    statusText.text =
                        "Frame capture analysis failed"
                }
            }

        } catch (e: Exception) {

            frameCaptureRequested = false

            Log.e(
                TAG,
                "debugGetNextFrameCallback failed",
                e
            )

            statusText.text =
                "Frame capture API failed"
        }
    }

    private fun analyzeRenderedBitmap(
        bitmap: Bitmap
    ) {

        val width =
            bitmap.width

        val height =
            bitmap.height

        if (
            width <= 0 ||
            height <= 0
        ) {

            Log.e(
                TAG,
                "FRAME RESULT: invalid bitmap " +
                    "${width}x${height}"
            )

            statusText.text =
                "FRAME INVALID"

            return
        }

        val pixels =
            IntArray(
                width * height
            )

        bitmap.getPixels(
            pixels,
            0,
            width,
            0,
            0,
            width,
            height
        )

        var nonBlackPixels = 0L
        var brightPixels = 0L
        var totalBrightness = 0L

        var minBrightness =
            255

        var maxBrightness =
            0

        for (pixel in pixels) {

            val red =
                (pixel shr 16) and 0xff

            val green =
                (pixel shr 8) and 0xff

            val blue =
                pixel and 0xff

            val brightness =
                (red + green + blue) / 3

            totalBrightness +=
                brightness.toLong()

            if (brightness > 3) {

                nonBlackPixels++
            }

            if (brightness > 30) {

                brightPixels++
            }

            if (
                brightness <
                    minBrightness
            ) {

                minBrightness =
                    brightness
            }

            if (
                brightness >
                    maxBrightness
            ) {

                maxBrightness =
                    brightness
            }
        }

        val pixelCount =
            pixels.size.toLong()

        val averageBrightness =
            if (pixelCount > 0) {
                totalBrightness.toDouble() /
                    pixelCount.toDouble()
            } else {
                0.0
            }

        val nonBlackPercent =
            if (pixelCount > 0) {
                nonBlackPixels.toDouble() *
                    100.0 /
                    pixelCount.toDouble()
            } else {
                0.0
            }

        val brightPercent =
            if (pixelCount > 0) {
                brightPixels.toDouble() *
                    100.0 /
                    pixelCount.toDouble()
            } else {
                0.0
            }

        val centerX =
            width / 2

        val centerY =
            height / 2

        val centerPixel =
            bitmap.getPixel(
                centerX,
                centerY
            )

        val centerRed =
            (centerPixel shr 16) and 0xff

        val centerGreen =
            (centerPixel shr 8) and 0xff

        val centerBlue =
            centerPixel and 0xff

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "RENDER OUTPUT DIAGNOSTIC"
        )

        Log.d(
            TAG,
            "Bitmap = ${width} x ${height}"
        )

        Log.d(
            TAG,
            "Average brightness = $averageBrightness"
        )

        Log.d(
            TAG,
            "Min brightness = $minBrightness"
        )

        Log.d(
            TAG,
            "Max brightness = $maxBrightness"
        )

        Log.d(
            TAG,
            "Non-black pixels = " +
                "$nonBlackPixels / $pixelCount " +
                "($nonBlackPercent%)"
        )

        Log.d(
            TAG,
            "Bright pixels = " +
                "$brightPixels / $pixelCount " +
                "($brightPercent%)"
        )

        Log.d(
            TAG,
            "Center pixel RGB = " +
                "($centerRed, $centerGreen, $centerBlue)"
        )

        Log.d(
            TAG,
            "Successful renders = " +
                successfulRenderCount
        )

        Log.d(
            TAG,
            "================================"
        )

        /*
         * We deliberately do NOT declare the model visible or invisible
         * based only on one threshold. The purpose of this diagnostic is
         * to tell us what Filament actually produced.
         */

        if (
            averageBrightness < 1.0 &&
            nonBlackPixels == 0L
        ) {

            statusText.text =
                "FRAME BLACK • check renderer/camera"

            Log.e(
                TAG,
                "FRAME RESULT = COMPLETELY BLACK"
            )

        } else if (
            nonBlackPercent > 1.0
        ) {

            statusText.text =
                "FRAME HAS PIXELS • inspect display/model"

            Log.d(
                TAG,
                "FRAME RESULT = NON-BLACK CONTENT EXISTS"
            )

        } else {

            statusText.text =
                "FRAME VERY DARK • inspect model/material"

            Log.w(
                TAG,
                "FRAME RESULT = VERY DARK / FEW NON-BLACK PIXELS"
            )
        }

        diagnosticCompleted = true
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
            "Render loop started"
        )
    }

    private fun stopRendering() {

        renderingStarted = false

        choreographer.removeFrameCallback(
            frameCallback
        )

        Log.d(
            TAG,
            "Render loop stopped"
        )
    }

    /*
     * ------------------------------------------------
     * GLB PICKER
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
     * PICKER RESULT
     * ------------------------------------------------
     */

    @Deprecated(
        "Activity Result API can be used later"
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

            data?.data?.let { uri ->

                loadGlb(uri)
            }
        }
    }

    /*
     * ------------------------------------------------
     * OPEN WITH
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

        try {

            /*
             * Reset diagnostic state for the new model.
             */
            frameCaptureRequested = false
            diagnosticCompleted = false
            modelReadyLogged = false
            renderFrameCount = 0L
            successfulRenderCount = 0L
            lastProgressLogged = -1f

            statusText.text =
                "Reading GLB..."

            val bytes =
                contentResolver
                    .openInputStream(uri)
                    ?.use {
                        it.readBytes()
                    }
                    ?: throw IllegalStateException(
                        "Unable to read GLB"
                    )

            Log.d(
                TAG,
                "GLB size = ${bytes.size} bytes"
            )

            /*
             * GLB header.
             */

            if (bytes.size < 12) {

                throw IllegalArgumentException(
                    "GLB is smaller than 12 bytes"
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
                    "Invalid GLB magic"
                )
            }

            /*
             * GLB uses little endian.
             */

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
                    "Invalid GLB length"
                )
            }

            /*
             * ------------------------------------------------
             * LOAD INTO FILAMENT
             * ------------------------------------------------
             */

            statusText.text =
                "Loading GLB..."

            val glbBuffer =
                ByteBuffer
                    .wrap(bytes)
                    .order(
                        ByteOrder.LITTLE_ENDIAN
                    )

            modelViewer.loadModelGlb(
                glbBuffer
            )

            Log.d(
                TAG,
                "loadModelGlb() completed"
            )

            /*
             * Scale and center the model.
             *
             * This is the same official ModelViewer operation used
             * by Filament's validation/sample flow.
             */
            try {

                modelViewer.transformToUnitCube()

                Log.d(
                    TAG,
                    "transformToUnitCube applied"
                )

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "transformToUnitCube failed",
                    e
                )
            }

            startRendering()

            statusText.text =
                "GLB loading resources..."

            /*
             * IMPORTANT:
             *
             * We intentionally removed the old fixed 1500 ms timer.
             * Filament loads resources asynchronously, so the diagnostic
             * now waits for modelViewer.progress >= 1.0.
             */

        } catch (e: Exception) {

            Log.e(
                TAG,
                "GLB loading failed",
                e
            )

            statusText.text =
                "GLB load failed"

            Toast.makeText(
                this,
                "GLB error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /*
     * ------------------------------------------------
     * MODEL DIAGNOSTICS
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
                "Asset NULL"

            return
        }

        /*
         * Filament 1.76.1 exposes renderableEntities.
         */

        val renderableEntities =
            asset.renderableEntities

        val renderableCount =
            renderableEntities.size

        val sceneRenderableCount =
            modelViewer.scene.renderableCount

        Log.d(
            TAG,
            "================================"
        )

        Log.d(
            TAG,
            "GLB DIAGNOSTIC"
        )

        Log.d(
            TAG,
            "Renderable entities = $renderableCount"
        )

        Log.d(
            TAG,
            "Scene renderables = $sceneRenderableCount"
        )

        Log.d(
            TAG,
            "Asset entities = ${asset.entities.size}"
        )

        Log.d(
            TAG,
            "Asset lights = ${asset.lightEntities.size}"
        )

        Log.d(
            TAG,
            "ModelViewer progress = ${modelViewer.progress}"
        )

        Log.d(
            TAG,
            "TextureView size = " +
                "${textureView.width} x " +
                "${textureView.height}"
        )

        Log.d(
            TAG,
            "ModelViewer viewport = " +
                "${modelViewer.view.viewport.width} x " +
                "${modelViewer.view.viewport.height}"
        )

        Log.d(
            TAG,
            "================================"
        )

        /*
         * ------------------------------------------------
         * RENDERABLE DETAILS
         * ------------------------------------------------
         */

        val renderableManager =
            modelViewer.engine
                .renderableManager

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
                    "Entity=$entity primitives=$primitiveCount"
                )

                /*
                 * Log material information where possible.
                 */
                for (
                    primitiveIndex in
                        0 until primitiveCount
                ) {

                    try {

                        val materialInstance =
                            renderableManager
                                .getMaterialInstanceAt(
                                    instance,
                                    primitiveIndex
                                )

                        Log.d(
                            TAG,
                            "Entity=$entity " +
                                "primitive=$primitiveIndex " +
                                "material=$materialInstance"
                        )

                    } catch (e: Exception) {

                        Log.w(
                            TAG,
                            "Material diagnostic failed " +
                                "entity=$entity " +
                                "primitive=$primitiveIndex",
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

        /*
         * ------------------------------------------------
         * CAMERA DIAGNOSTIC
         * ------------------------------------------------
         */

        try {

            val camera =
                modelViewer.camera

            val position =
                FloatArray(3)

            camera.getPosition(
                position
            )

            Log.d(
                TAG,
                "Camera position = " +
                    "(${position[0]}, " +
                    "${position[1]}, " +
                    "${position[2]})"
            )

            val projection =
                DoubleArray(16)

            camera.getProjectionMatrix(
                projection
            )

            Log.d(
                TAG,
                "Camera projection matrix read"
            )

            Log.d(
                TAG,
                "Projection[0] = ${projection[0]}"
            )

            Log.d(
                TAG,
                "Projection[5] = ${projection[5]}"
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
         * FINAL SCENE RESULT
         * ------------------------------------------------
         */

        if (
            renderableCount > 0 &&
            sceneRenderableCount > 0
        ) {

            Log.d(
                TAG,
                "RESULT = RENDERABLE MODEL IN SCENE"
            )

        } else if (
            renderableCount > 0
        ) {

            Log.e(
                TAG,
                "RESULT = ASSET HAS RENDERABLES BUT SCENE IS EMPTY"
            )

        } else {

            Log.e(
                TAG,
                "RESULT = NO RENDERABLE ENTITIES"
            )
        }

        /*
         * Don't overwrite the more useful frame-output status if the
         * frame diagnostic already completed.
         */
        if (!diagnosticCompleted) {

            statusText.text =
                if (
                    renderableCount > 0 &&
                    sceneRenderableCount > 0
                ) {

                    "RENDERABLES $renderableCount • SCENE $sceneRenderableCount"

                } else if (
                    renderableCount > 0
                ) {

                    "ASSET $renderableCount • SCENE 0"

                } else {

                    "NO RENDERABLES"
                }
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

                if (lightEntity != 0) {

                    modelViewer.engine
                        .destroyEntity(
                            lightEntity
                        )

                    EntityManager.get()
                        .destroy(
                            lightEntity
                        )

                    lightEntity = 0
                }

            } catch (e: Exception) {

                Log.w(
                    TAG,
                    "Light cleanup failed",
                    e
                )
            }

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
