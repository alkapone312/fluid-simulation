package org.example.benchmark;

public interface RenderPassListener {
    /**
     * Wywoływane po zakończeniu pomiaru konkretnego shadera/passa na karcie graficznej.
     * @param passName Nazwa etapu (np. "SSFR_Depth", "Volume_Raycast")
     * @param timeMs Czas wykonania na GPU w milisekundach
     */
    void onPassMeasured(String passName, double timeMs);
}