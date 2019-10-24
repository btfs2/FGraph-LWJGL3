package uk.ac.cam.cl.furthergfx.crsid.tick;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import java.nio.FloatBuffer;
import java.util.concurrent.atomic.AtomicLong;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

/**
 * A standalone GLFW windowed app which renders a black quad using the GLFW default shaders. Based
 * on 'HelloGL.java':
 * https://github.com/AlexBenton/Teaching/tree/master/AdvGraph1617/OpenGL%20Demos/com/bentonian/gldemos/hellogl/HelloGL.java
 */
public class TickCanvas {
  protected int vao; // Vertex Array Object
  protected int vbo; // Vertex Buffer Object

  protected long window;

  /**
   * Set up GLFW and OpenGL
   */
  public void init() {
    setupDisplay();
    setupInputs();
    setupOpenGL();
    setupGeometry();
  }

  /**
   * Loop until window is closed
   */
  public void run() {
    while (!glfwWindowShouldClose(window)) {
      render();
    }
  }

  /**
   * Clean up and release resources
   */
  public void shutdown() {
    GL15.glDeleteBuffers(vbo);
    GL30.glDeleteVertexArrays(vao);
    glfwDestroyWindow(window);
  }

  /**
   * Set up Display
   */
  protected void setupDisplay() {
    // Make LWJGL log to sysout
    glfwSetErrorCallback(GLFWErrorCallback.createPrint(System.err));
    if (!glfwInit())
      throw new IllegalStateException("Unable to initialize GLFW");

    // Request GL 3.3
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    // Make resizable
    glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

    // Create window
    window = glfwCreateWindow(800, 600, "Further Graphics - Tick", 0, 0);
    glfwMakeContextCurrent(window);

    // Set Vsync
    glfwSwapInterval(1);

    // Create GL Context
    GL.createCapabilities();

    // Verify GL context version (will error if no context exists)
    System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION));
  }

  /**
   * Sets up GLFW input callbacks
   */
  protected void setupInputs() {

    // LWJGL3 Inputs are callback based because they are more efficient

    // Handle resize
    glfwSetWindowSizeCallback(window, (long window, int width, int height) -> {
      onResized(width, height);
    });

    // Handle key presses
    glfwSetKeyCallback(window, (long window, int key, int scancode, int action, int mods) -> {
      if (GLFW_RELEASE == action) {
        onKeyPressed(key);
      }
    });

    // Handle scrolling
    // Note that most scrolling is up/down so we use the y offset
    glfwSetScrollCallback(window, (long window, double xoffset, double yoffset) -> {
      onMouseScroll(yoffset);
    });

    // Handle mouse moves
    // Atomics used as pointer stores; could probs be done more efficiently
    AtomicLong xPosLast = new AtomicLong(), yPosLast = new AtomicLong();
    glfwSetCursorPosCallback(window, (long window, double xpos, double ypos) -> {
      if (glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
        //HACKY BIT STUFF
        double xpl = Double.longBitsToDouble(xPosLast.get());
        double ypl = Double.longBitsToDouble(yPosLast.get());
        double dx = xpos - xpl, dy = ypos - ypl;
        onMouseDrag(dx, dy);
      }
      xPosLast.set(Double.doubleToLongBits(xpos));
      yPosLast.set(Double.doubleToLongBits(ypos));
    });
  }

  /**
   * Set up OpenGL itself
   */
  protected void setupOpenGL() {
    GL11.glClearColor(0.2f, 0.4f, 0.6f, 0.0f);
    GL11.glClearDepth(1.0f);
    //Pointers
    int[] width = new int[1];
    int[] height = new int[1];
    GLFW.glfwGetWindowSize(window, width, height);
    GL11.glViewport(0, 0, width[0], height[0]);
  }

  protected void render() {
    glfwPollEvents();
    glfwSwapBuffers(window);
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    GL30.glBindVertexArray(vao);
    GL11.glDrawArrays(GL11.GL_TRIANGLES, 0 /* start */, 6 /* num vertices */);
  }

  protected void onMouseScroll(double delta) {}

  protected void onMouseDrag(double dx, double dy) {}

  protected void onKeyPressed(int key) {}

  protected void onResized(int width, int height) {
    GL11.glViewport(0, 0, width, height);
  }

  /**
   * Set up minimal geometry to render a quad
   */
  private void setupGeometry() {

    // Fill a Java FloatBuffer object with two triangles forming a quad from (-1, -1) to (1, 1)
    float[] coords = new float[] {-1, -1, 1f, 1, -1, 1, -1, 1, 1, 1, -1, 1, 1, 1, 1, -1, 1, 1f};
    FloatBuffer fbo = BufferUtils.createFloatBuffer(coords.length);
    fbo.put(coords); // Copy the vertex coords into the floatbuffer
    fbo.flip(); // Mark the floatbuffer ready for reads

    // Store the FloatBuffer's contents in a Vertex Buffer Object
    vbo = GL15.glGenBuffers(); // Get an OGL name for the VBO
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo); // Activate the VBO
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, fbo, GL15.GL_STATIC_DRAW); // Send VBO data to GPU

    // Bind the VBO in a Vertex Array Object
    vao = GL30.glGenVertexArrays(); // Get an OGL name for the VAO
    GL30.glBindVertexArray(vao); // Activate the VAO
    GL20.glEnableVertexAttribArray(0); // Enable the VAO's first attribute (0)
    GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 0, 0); // Link VBO to VAO attrib 0
  }
}
