import org.jetbrains.skija.*
import org.jetbrains.skija.impl.Library
import org.jetbrains.skija.impl.Stats
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.opengl.GL
import org.lwjgl.system.MemoryUtil.NULL
import java.util.*


fun main() {
    GLFWErrorCallback.createPrint(System.err).set()
    check(glfwInit()) { "Unable to initialize GLFW" }

    val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())
    val width = (vidmode!!.width() * 0.75).toInt()
    val height = (vidmode.height() * 0.75).toInt()
    val bounds = IRect.makeXYWH(
        Math.max(0, (vidmode.width() - width) / 2),
        Math.max(0, (vidmode.height() - height) / 2),
        width,
        height
    )
    val w = Window()
    w.run(bounds)


}

class App(private val canvas: Canvas) {

    fun draw() {
        val paint = Paint()
        paint.color = -0x10000
        canvas.drawCircle(50F, 50F, 30F, paint);
    }
}

class Window {
    var window: Long = 0L
    var width = 0
    var height = 0
    var dpi = 1f
    var xpos = 0
    var ypos = 0
    var vsync = true
    var stats = true
    private val os = System.getProperty("os.name").lowercase(Locale.getDefault())

    lateinit var refreshRates: IntArray

    private lateinit var context: DirectContext
    private var renderTarget: BackendRenderTarget? = null
    private var surface: Surface? = null
    private lateinit var canvas: Canvas
    private lateinit var app: App


    fun run(bounds: IRect?) {
        refreshRates = getRefreshRates1()
        createWindow(bounds!!)
        loop()
        Callbacks.glfwFreeCallbacks(window)
        glfwDestroyWindow(window)
        glfwTerminate()
        glfwSetErrorCallback(null)!!.free()
    }

    private fun getRefreshRates1(): IntArray {
        val monitors = glfwGetMonitors()
        val res = IntArray(monitors!!.capacity())
        for (i in 0 until monitors.capacity()) {
            res[i] = glfwGetVideoMode(monitors[i])!!.refreshRate()
        }
        return res
    }


    private fun createWindow(bounds: IRect) {
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

        window = glfwCreateWindow(bounds.width, bounds.height, "Skija LWJGL Demo", NULL, NULL);
        if (window == NULL)
            throw RuntimeException("Failed to create the GLFW window");


        glfwSetKeyCallback(window) { window1: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) glfwSetWindowShouldClose(
                window1,
                true
            )
        }

        glfwSetWindowPos(window, bounds.left, bounds.top)
        updateDimensions()
        xpos = width / 2
        ypos = height / 2

        glfwMakeContextCurrent(window)
        glfwSwapInterval(if (vsync) 1 else 0) // Enable v-sync
        glfwShowWindow(window)
    }

    private fun updateDimensions() {
        val width = IntArray(1)
        val height = IntArray(1)
        glfwGetFramebufferSize(window, width, height)
        val xscale = FloatArray(1)
        val yscale = FloatArray(1)
        glfwGetWindowContentScale(window, xscale, yscale)
        assert(xscale[0] == yscale[0]) { "Horizontal dpi=" + xscale[0] + ", vertical dpi=" + yscale[0] }
        this.width = (width[0] / xscale[0]).toInt()
        this.height = (height[0] / yscale[0]).toInt()
        dpi = xscale[0]
        println("FramebufferSize " + width[0] + "x" + height[0] + ", scale " + dpi + ", window " + this.width + "x" + this.height)
    }

    private fun initSkia() {
        Stats.enabled = true
        surface?.close()
        renderTarget?.close()

        renderTarget = BackendRenderTarget.makeGL(
            (width * dpi).toInt(), (height * dpi).toInt(),  /*samples*/
            0,  /*stencil*/
            8,  /*fbId*/
            0,
            FramebufferFormat.GR_GL_RGBA8
        )
        surface = Surface.makeFromBackendRenderTarget(
            context,
            renderTarget!!,
            SurfaceOrigin.BOTTOM_LEFT,
            SurfaceColorFormat.RGBA_8888,
            ColorSpace.getDisplayP3(),  // TODO load monitor profile
            SurfaceProps(PixelGeometry.RGB_H)
        )
        canvas = surface!!.canvas

        app = App(canvas)
    }

    private fun draw() {
        app.draw()
        context.flush()
        glfwSwapBuffers(window)
    }

    private fun loop() {
        GL.createCapabilities()
        if ("false" == System.getProperty("skija.staticLoad")) Library.load()
        context = DirectContext.makeGL()
        glfwSetWindowSizeCallback(window) { window: Long, width: Int, height: Int ->
            updateDimensions()
            initSkia()
            draw()
        }
        glfwSetCursorPosCallback(
            window
        ) { window: Long, xpos: Double, ypos: Double ->
            if (os.contains("mac") || os.contains("darwin")) {
                this.xpos = xpos.toInt()
                this.ypos = ypos.toInt()
            } else {
                this.xpos = (xpos / dpi).toInt()
                this.ypos = (ypos / dpi).toInt()
            }
        }
        glfwSetMouseButtonCallback(window) { window: Long, button: Int, action: Int, mods: Int -> }
        glfwSetScrollCallback(window) { window: Long, xoffset: Double, yoffset: Double ->
            println("scroll ${xoffset.toFloat()} ${yoffset.toFloat()}")
        }
        glfwSetKeyCallback(
            window
        ) { window: Long, key: Int, scancode: Int, action: Int, mods: Int ->
            if (action == GLFW_PRESS) {
                when (key) {
                    GLFW_KEY_LEFT -> println("left")
                    GLFW_KEY_RIGHT -> println("right")
                    GLFW_KEY_UP -> println("up")
                    GLFW_KEY_DOWN -> println("down")
                    GLFW_KEY_V -> {
                        vsync = !vsync
                        glfwSwapInterval(if (vsync) 1 else 0)
                        println("VSync: " + if (vsync) "ON" else "OFF")
                    }

                    GLFW_KEY_S -> {
                        stats = !stats
                        Stats.enabled = stats
                        println("Stats: " + if (stats) "ON" else "OFF")
                    }

                    GLFW_KEY_G -> {
                        println("Before GC " + Stats.allocated)
                        System.gc()
                    }
                    GLFW_KEY_ESCAPE -> {
                        glfwSetWindowShouldClose(window,true)
                    }
                }
            }
        }
        initSkia()
        while (!glfwWindowShouldClose(window)) {
            draw()
            glfwPollEvents()
        }
    }

}

