package com.animalvision.app.gl

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.animalvision.app.DisplayMode
import com.animalvision.core.color.ColorTransform
import com.animalvision.core.color.LutGenerator
import com.animalvision.core.model.AnimalVisionProfile
import com.animalvision.core.model.QualityTier
import com.animalvision.core.profiles.requireProfile
import com.animalvision.core.reference.CpuRenderer
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs the real GPU pipeline offscreen (EGL pbuffer) on a known camera image and checks what
 * the JVM tests cannot see: readback, orientation, GPU-vs-CPU parity, and the video encoder.
 * Each test pins a bug that shipped before: black photos, the upside-down horse blind spot,
 * and videos that were upside down and ~1000× too short.
 */
@RunWith(AndroidJUnit4::class)
class GpuPipelineTest {

    // 16-aligned so every hardware encoder accepts the size.
    private val w = 640
    private val h = 352

    private lateinit var display: EGLDisplay
    private lateinit var context: EGLContext
    private lateinit var surface: EGLSurface
    private lateinit var frames: HandlerThread
    private val renderers = mutableListOf<AnimalGlRenderer>()

    @Before
    fun setUp() {
        display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1))
        val configs = arrayOfNulls<EGLConfig>(1)
        val count = IntArray(1)
        check(
            EGL14.eglChooseConfig(
                display,
                intArrayOf(
                    EGL14.EGL_RENDERABLE_TYPE, 0x0040, // EGL_OPENGL_ES3_BIT_KHR
                    EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                    EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                    EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                    EGL14.EGL_NONE,
                ),
                0, configs, 0, 1, count, 0,
            ) && count[0] > 0,
        )
        context = EGL14.eglCreateContext(
            display, configs[0], EGL14.EGL_NO_CONTEXT,
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE), 0,
        )
        surface = EGL14.eglCreatePbufferSurface(
            display, configs[0], intArrayOf(EGL14.EGL_WIDTH, w, EGL14.EGL_HEIGHT, h, EGL14.EGL_NONE), 0,
        )
        check(EGL14.eglMakeCurrent(display, surface, surface, context))
        frames = HandlerThread("camera-frames").also { it.start() }
    }

    @After
    fun tearDown() {
        renderers.forEach { it.shutdown() }
        frames.quitSafely()
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, surface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }

    // ---- harness -------------------------------------------------------------------

    private inner class Harness(val profile: AnimalVisionProfile, mode: Int = DisplayMode.ANIMAL) {
        val renderer = AnimalGlRenderer(
            profileProvider = { profile },
            tierProvider = { QualityTier.HIGH },
            modeProvider = { mode },
        )
        lateinit var camera: SurfaceTexture

        init {
            renderers += renderer
            renderer.onSurfaceTextureReady = { camera = it }
            renderer.onSurfaceCreated(null, null)
            renderer.setCameraSize(w, h)
            renderer.onSurfaceChanged(null, w, h)
            renderer.setProfile(profile)
        }

        /** Feed [image] as the next camera frame and wait until the texture has it. */
        fun postCameraFrame(image: Bitmap) {
            val latch = CountDownLatch(1)
            camera.setOnFrameAvailableListener({ latch.countDown() }, Handler(frames.looper))
            val producer = Surface(camera)
            val canvas = producer.lockCanvas(null)
            canvas.drawBitmap(image, 0f, 0f, null)
            producer.unlockCanvasAndPost(canvas)
            producer.release()
            check(latch.await(3, TimeUnit.SECONDS)) { "camera frame never arrived" }
        }

        /** Draw frames until this profile's LUT is on the GPU. */
        fun awaitLut() {
            val deadline = System.currentTimeMillis() + 5_000
            while (renderer.uploadedLutProfileId != profile.id) {
                check(System.currentTimeMillis() < deadline) { "LUT for ${profile.id} never uploaded" }
                renderer.onDrawFrame(null)
                Thread.sleep(10)
            }
        }

        /** Render one frame and return the photo the app would save. */
        fun capture(): Bitmap {
            var photo: Bitmap? = null
            renderer.requestCapture { photo = it }
            renderer.onDrawFrame(null)
            return checkNotNull(photo) { "capture callback did not run" }
        }
    }

    private fun splitImage(top: Int, bottom: Int): Bitmap =
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            val c = Canvas(bmp)
            c.drawRect(0f, 0f, w.toFloat(), h / 2f, Paint().apply { color = top })
            c.drawRect(0f, h / 2f, w.toFloat(), h.toFloat(), Paint().apply { color = bottom })
        }

    private fun Bitmap.at(fx: Float, fy: Float): Int =
        getPixel((fx * (width - 1)).toInt(), (fy * (height - 1)).toInt())

    private fun luma(c: Int) = 0.2126 * Color.red(c) + 0.7152 * Color.green(c) + 0.0722 * Color.blue(c)

    // ---- tests ---------------------------------------------------------------------

    @Test
    fun photoIsNotBlackAndKeepsTheCameraUpright() {
        val human = Harness(requireProfile("human"))
        human.awaitLut()
        human.postCameraFrame(splitImage(top = Color.RED, bottom = Color.BLUE))
        val photo = human.capture()

        val top = photo.at(0.5f, 0.2f)
        val bottom = photo.at(0.5f, 0.8f)
        assertWithMessage("top pixel ${Integer.toHexString(top)}").that(Color.red(top)).isGreaterThan(180)
        assertThat(Color.blue(top)).isLessThan(80)
        assertWithMessage("bottom pixel ${Integer.toHexString(bottom)}").that(Color.blue(bottom)).isGreaterThan(180)
        assertThat(Color.red(bottom)).isLessThan(80)
    }

    @Test
    fun horseMuzzleBlindSpotIsAtTheBottom() {
        val horse = Harness(requireProfile("horse"))
        horse.awaitLut()
        horse.postCameraFrame(splitImage(top = 0xff909090.toInt(), bottom = 0xff909090.toInt()))
        val photo = horse.capture()

        val upperCentre = luma(photo.at(0.5f, 0.08f))
        val lowerCentre = luma(photo.at(0.5f, 0.92f))
        assertWithMessage("upper=$upperCentre lower=$lowerCentre").that(lowerCentre).isLessThan(upperCentre * 0.5)
    }

    @Test
    fun gpuMatchesTheCpuReferenceForBee() {
        // Bee: color LUT + large pyramid blur, no FOV warp (the CPU reference has no warp).
        val bee = requireProfile("bee")
        val image = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { bmp ->
            val c = Canvas(bmp)
            val colors = intArrayOf(0xffd03020.toInt(), 0xff30a040.toInt(), 0xff2040c0.toInt(), 0xff808080.toInt())
            for (q in 0..3) {
                val x0 = (q % 2) * w / 2f
                val y0 = (q / 2) * h / 2f
                c.drawRect(x0, y0, x0 + w / 2f, y0 + h / 2f, Paint().apply { color = colors[q] })
            }
        }
        val gpu = Harness(bee)
        gpu.awaitLut()
        gpu.postCameraFrame(image)
        val gpuPixels = IntArray(w * h).also { gpu.capture().getPixels(it, 0, w, 0, 0, w, h) }

        val lut = LutGenerator.generate(ColorTransform(bee), bee.colorVision.lutSize)
        val input = IntArray(w * h).also { image.getPixels(it, 0, w, 0, 0, w, h) }
        val cpuPixels = CpuRenderer(bee, lut, QualityTier.HIGH).render(input, w, h)

        var sum = 0.0
        for (i in gpuPixels.indices) {
            val a = gpuPixels[i]
            val b = cpuPixels[i]
            sum += Math.abs(Color.red(a) - Color.red(b)) +
                Math.abs(Color.green(a) - Color.green(b)) +
                Math.abs(Color.blue(a) - Color.blue(b))
        }
        val meanAbs = sum / (3.0 * gpuPixels.size)
        assertWithMessage("mean |GPU − CPU| per channel (0..255)").that(meanAbs).isLessThan(6.0)
    }

    @Test
    fun videoHasRealDurationAndUprightFrames() {
        val human = Harness(requireProfile("human"))
        human.awaitLut()
        human.postCameraFrame(splitImage(top = Color.RED, bottom = Color.BLUE))

        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "gpu_test.mp4")
        file.delete()
        var error: String? = null
        human.renderer.startRecordingOnGlThread(file) { error = it }
        assertThat(error).isNull()
        repeat(30) {
            human.renderer.onDrawFrame(null)
            Thread.sleep(33)
        }
        val stopped = CountDownLatch(1)
        human.renderer.stopRecordingOnGlThread { stopped.countDown() }
        assertThat(stopped.await(10, TimeUnit.SECONDS)).isTrue()

        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)!!.toLong()
            assertWithMessage("~1 s recorded").that(durationMs).isAtLeast(700L)
            val frame = checkNotNull(
                retriever.getFrameAtTime(durationMs * 500L, MediaMetadataRetriever.OPTION_CLOSEST),
            )
            val top = frame.at(0.5f, 0.2f)
            val bottom = frame.at(0.5f, 0.8f)
            assertWithMessage("video top ${Integer.toHexString(top)}").that(Color.red(top)).isGreaterThan(150)
            assertWithMessage("video bottom ${Integer.toHexString(bottom)}").that(Color.blue(bottom)).isGreaterThan(150)
        } finally {
            retriever.release()
            file.delete()
        }
    }
}
