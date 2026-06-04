package pl.pwr.jkaplone.fluidsim.benchmark;

/**
 * Interfejs dla procesorów renderujących, które wspierają sprzętowe
 * profilowanie czasu wykonania poszczególnych shaderów na GPU.
 */
public interface GpuProfilable {
    void setRenderPassListener(RenderPassListener listener);
}