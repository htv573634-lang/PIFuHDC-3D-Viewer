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

    private val choreographer =
        Choreographer.getInstance()

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

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        Utils.init()

        createUi()
    }

    private fun createUi() {

        val root =
            LinearLayout(this).apply {
                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.BLACK
                )
            }

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
            initializeViewer()
        }
    }

    private fun initializeViewer() {

        try {

            modelViewer =
                ModelViewer(
                    textureView
                )

            textureView.setOnTouchListener(
                modelViewer
            )

            setupRendering()

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
                "Filament error: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun setupRendering() {

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

        createDirectionalLight()
    }

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
                "Directional light created"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Directional light failed",
                e
            )
        }
    }

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

        startActivityForResult(
            pickerIntent,
            REQUEST_GLB
        )
    }

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

            data?.data?.let { uri ->
                loadGlb(uri)
            }
        }
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

            /*
             * IMPORTANT:
             *
             * Filament's ModelViewer uses this
             * transformation to scale and center
             * the loaded model into a unit cube.
             *
             * This is the main visibility fix.
             */
            modelViewer.transformToUnitCube()

            startRendering()

            statusText.text =
                "GLB loaded - preparing view..."

            Log.d(
                TAG,
                "loadModelGlb() completed"
            )

            Log.d(
                TAG,
                "transformToUnitCube() applied"
            )

            /*
             * Give the resource loader time to
             * finish GPU resource creation.
             */
            textureView.postDelayed(
                {
                    inspectModel()
                },
                1500L
            )

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
            "TextureView size = " +
                "${textureView.width} x " +
                "${textureView.height}"
        )

        Log.d(
            TAG,
            "================================"
        )

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

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Renderable inspection failed",
                    e
                )
            }
        }

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

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Camera diagnostic failed",
                e
            )
        }

        if (
            renderableCount > 0 &&
            sceneRenderableCount > 0
        ) {

            statusText.text =
                "RENDERABLES $renderableCount • SCENE $sceneRenderableCount"

            Log.d(
                TAG,
                "RESULT = RENDERABLE MODEL IN SCENE"
            )

        } else if (
            renderableCount > 0
        ) {

            statusText.text =
                "ASSET $renderableCount • SCENE 0"

            Log.e(
                TAG,
                "RESULT = ASSET HAS RENDERABLES BUT SCENE IS EMPTY"
            )

        } else {

            statusText.text =
                "NO RENDERABLES"

            Log.e(
                TAG,
                "RESULT = NO RENDERABLE ENTITIES"
            )
        }

        startRendering()
    }

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
