package org.example;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL43.*;

public class GLContext {

    public static void init() {
        if (!GLFW.glfwInit()) throw new IllegalStateException("Failed to init GLFW");

        // Hidden tiny window
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1, 1, "Hidden", 0, 0);
        if (window == 0) throw new RuntimeException("Failed to create window");

        GLFW.glfwMakeContextCurrent(window);
        GL.createCapabilities();
        System.out.println("OpenGL version: " + glGetString(GL_VERSION));
    }
}
