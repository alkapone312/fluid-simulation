package org.example.benchmark;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL33;

public class GpuTimer {

    private final int queryId;
    private boolean isQueryRunning = false;
    private boolean hasResult = false;

    public GpuTimer() {
        // Generujemy identyfikator zapytania w OpenGL
        queryId = GL15.glGenQueries();
    }

    public void start() {
        if (!isQueryRunning) {
            // Informujemy GPU: "Zacznij mierzyć czas wszystkiego, co teraz rysuję"
            GL15.glBeginQuery(GL33.GL_TIME_ELAPSED, queryId);
            isQueryRunning = true;
            hasResult = false;
        }
    }

    public void stop() {
        if (isQueryRunning) {
            // Informujemy GPU: "Koniec pomiaru dla tego zapytania"
            GL15.glEndQuery(GL33.GL_TIME_ELAPSED);
            isQueryRunning = false;
            hasResult = true;
        }
    }

    /**
     * Zwraca czas wykonania w milisekundach.
     * UWAGA: Ta metoda blokuje CPU, dopóki GPU nie skończy renderować danej klatki!
     * W trybie testowym to pożądane, w finalnej grze zabiłoby to wydajność.
     */
    public double getResultMs() {
        if (!hasResult) return 0.0;

        // Pobieramy wynik z pamięci karty graficznej (w nanosekundach)
        long elapsedNanos = GL33.glGetQueryObjectui64(queryId, GL15.GL_QUERY_RESULT);
        return elapsedNanos / 1_000_000.0;
    }

    public void cleanup() {
        GL15.glDeleteQueries(queryId);
    }
}