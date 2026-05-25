package org.example.benchmark;

import java.util.HashMap;
import java.util.Map;

public class GpuProfiler {

    // Dynamiczna mapa przechowująca timery dla poszczególnych etapów
    private Map<String, GpuTimer> timers = new HashMap<>();
    private RenderPassListener listener;

    public void setListener(RenderPassListener listener) {
        this.listener = listener;
    }

    public void start(String passName) {
        if (listener == null) return; // Jeśli nie testujemy, nie obciążamy GPU

        // Tworzy nowy stoper, jeśli jeszcze takiego nie ma pod tą nazwą
        timers.computeIfAbsent(passName, k -> new GpuTimer()).start();
    }

    public void stop(String passName) {
        if (listener == null) return;

        GpuTimer timer = timers.get(passName);
        if (timer != null) {
            timer.stop();
            // Natychmiast wysyłamy wynik do BenchmarkState
            listener.onPassMeasured(passName, timer.getResultMs());
        }
    }

    public void cleanup() {
        for (GpuTimer timer : timers.values()) {
            timer.cleanup();
        }
        timers.clear();
    }
}