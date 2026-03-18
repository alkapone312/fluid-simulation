package org.example;

import org.lwjgl.opengl.GL43;

import java.nio.*;
import java.util.HashMap;
import java.util.Map;

public class ComputeShader {
    public enum IntegerType {
        SIGNED,
        UNSIGNED
    }

    private final int programId;
    private final Map<String, Integer> uniformCache = new HashMap<>();
    private final Map<Integer, Integer> ssbos = new HashMap<>();

    public ComputeShader(String source) {
        int shaderId = GL43.glCreateShader(GL43.GL_COMPUTE_SHADER);
        GL43.glShaderSource(shaderId, source);
        GL43.glCompileShader(shaderId);

        int status = GL43.glGetShaderi(shaderId, GL43.GL_COMPILE_STATUS);
        if (status == GL43.GL_FALSE) {
            throw new RuntimeException("Compute shader compilation failed:\n" + GL43.glGetShaderInfoLog(shaderId));
        }

        programId = GL43.glCreateProgram();
        GL43.glAttachShader(programId, shaderId);
        GL43.glLinkProgram(programId);
        GL43.glValidateProgram(programId);

        GL43.glDeleteShader(shaderId);
    }

    public void bind() {
        GL43.glUseProgram(programId);
        for (Map.Entry<Integer, Integer> entry : ssbos.entrySet()) {
            GL43.glBindBufferBase(GL43.GL_SHADER_STORAGE_BUFFER, entry.getKey(), entry.getValue());
        }
    }

    public void dispatch(int x, int y, int z) {
        GL43.glDispatchCompute(x, y, z);
        GL43.glMemoryBarrier(GL43.GL_ALL_BARRIER_BITS);
    }

    public void setUniform(String name, float value) {
        int loc = uniformLocation(name);
        GL43.glUniform1f(loc, value);
    }

    public void setUniform(String name, int value, IntegerType type) {
        int loc = uniformLocation(name);
        if (type == IntegerType.UNSIGNED) {
            GL43.glUniform1ui(loc, value);

            return;
        }

        GL43.glUniform1i(loc, value);
    }

    public void setUniform(String name, float x, float y, float z) {
        int loc = uniformLocation(name);
        GL43.glUniform3f(loc, x, y, z);
    }

    public void setUniform(String name, float x, float y) {
        int loc = uniformLocation(name);
        GL43.glUniform2f(loc, x, y);
    }

    private HashMap<Integer, Integer> ssboBindings = new HashMap<>();

    public void setData(int binding, Buffer data) {
        int bufferId = ssboBindings.computeIfAbsent(binding, (a) -> GL43.glGenBuffers());
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);
        if (data instanceof FloatBuffer fb) {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, fb, GL43.GL_DYNAMIC_COPY);
        } else if (data instanceof IntBuffer ib) {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, ib, GL43.GL_DYNAMIC_COPY);
        } else if (data instanceof ByteBuffer bb) {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, bb, GL43.GL_DYNAMIC_COPY);
        } else if (data instanceof ShortBuffer sb) {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, sb, GL43.GL_DYNAMIC_COPY);
        } else if (data instanceof LongBuffer lb) {
            GL43.glBufferData(GL43.GL_SHADER_STORAGE_BUFFER, lb, GL43.GL_DYNAMIC_COPY);
        } else {
            throw new IllegalArgumentException(
                "Unsupported buffer type: " + data.getClass()
            );
        }

        int size = GL43.glGetBufferParameteri(
            GL43.GL_SHADER_STORAGE_BUFFER,
            GL43.GL_BUFFER_SIZE
        );
        System.out.println("SSBO " + binding + " size = " + size);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        ssbos.put(binding, bufferId);
    }

    @SuppressWarnings("unchecked")
    public <T extends Buffer> T getData(int binding, Class<T> bufferType) {
        Integer bufferId = ssbos.get(binding);
        if (bufferId == null) {
            throw new IllegalArgumentException("No SSBO bound to binding: " + binding);
        }

        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, bufferId);

        // Map the GPU memory to a ByteBuffer
        ByteBuffer mappedBuffer = GL43.glMapBuffer(GL43.GL_SHADER_STORAGE_BUFFER, GL43.GL_READ_ONLY);

        T result;

        if (bufferType == FloatBuffer.class) {
            FloatBuffer view = mappedBuffer.asFloatBuffer();
            FloatBuffer copy = FloatBuffer.allocate(view.remaining());
            copy.put(view);
            copy.flip();
            result = (T) copy;
        } else if (bufferType == IntBuffer.class) {
            IntBuffer view = mappedBuffer.asIntBuffer();
            IntBuffer copy = IntBuffer.allocate(view.remaining());
            copy.put(view);
            copy.flip();
            result = (T) copy;
        } else if (bufferType == ByteBuffer.class) {
            ByteBuffer copy = ByteBuffer.allocate(mappedBuffer.remaining());
            copy.put(mappedBuffer);
            copy.flip();
            result = (T) copy;
        } else {
            GL43.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
            throw new UnsupportedOperationException("Unsupported buffer type: " + bufferType.getName());
        }

        GL43.glUnmapBuffer(GL43.GL_SHADER_STORAGE_BUFFER);
        GL43.glBindBuffer(GL43.GL_SHADER_STORAGE_BUFFER, 0);

        return result;
    }

    public void cleanup() {
        GL43.glDeleteProgram(programId);
        for (int bufferId : ssbos.values()) {
            GL43.glDeleteBuffers(bufferId);
        }
        ssbos.clear();
    }

    private int uniformLocation(String name) {
        return uniformCache.computeIfAbsent(name, n -> GL43.glGetUniformLocation(programId, n));
    }
}
