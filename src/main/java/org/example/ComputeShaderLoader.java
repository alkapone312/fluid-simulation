package org.example;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ComputeShaderLoader {

    private static final Pattern INCLUDE_PATTERN =
        Pattern.compile("^\\s*#include\\s*<(.+)>\\s*$");

    /**
     * Loads, processes includes, and returns a new ComputeShader.
     * @param path The absolute path on the classpath (e.g., "/shaders/sim.glsl").
     */
    public static ComputeShader load(String path) {
        String fullSource = resolveIncludes(path, new HashSet<>());

        return new ComputeShader(fullSource);
    }

    private static String resolveIncludes(String path, Set<String> loadedFiles) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (loadedFiles.contains(path)) return "";
        loadedFiles.add(path);

        StringBuilder sb = new StringBuilder();

        try (InputStream in = ComputeShaderLoader.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new RuntimeException("Shader file not found on classpath: " + path);
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = INCLUDE_PATTERN.matcher(line);
                if (matcher.find()) {
                    String includePath = matcher.group(1);
                    sb.append("// Start Include: ").append(includePath).append("\n");
                    sb.append(resolveIncludes(includePath, loadedFiles));
                    sb.append("\n// End Include: ").append(includePath).append("\n");
                } else {
                    sb.append(line).append("\n");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error processing shader: " + path, e);
        }
        return sb.toString();
    }
}