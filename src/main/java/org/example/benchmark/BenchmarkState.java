package org.example.benchmark;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.post.SceneProcessor;
import org.example.Simulation3D;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BenchmarkState extends BaseAppState implements RenderPassListener {

    private Simulation3D app;
    private BenchmarkProcessor processor;

    // Zarządzanie testami
    private List<BenchmarkSet> testSets = new ArrayList<>();
    private int currentSetIndex = 0;
    private int currentScenarioIndex = 0;

    // Pliki zapisu
    private PrintWriter mainCsvWriter;
    private PrintWriter gpuCsvWriter;

    private Map<String, Double> passTimeSums = new LinkedHashMap<>();

    public BenchmarkState() {
        // Pusty konstruktor, konfigurację przeprowadzamy dodając Sety
    }

    public void addBenchmarkSet(BenchmarkSet set) {
        testSets.add(set);
    }

    @Override
    protected void initialize(Application app) {
        this.app = (Simulation3D) app;
        this.processor = new BenchmarkProcessor(this);
        System.out.println("Zainicjowano BenchmarkState.");
    }

    @Override
    protected void onEnable() {
        app.getViewPort().addProcessor(processor);

        if (!testSets.isEmpty()) {
            startSet(testSets.get(0));
        } else {
            System.out.println("Brak zestawów testowych do uruchomienia!");
            setEnabled(false);
        }
    }

    @Override
    protected void onDisable() {
        app.getViewPort().removeProcessor(processor);
        closeWriters();
    }

    @Override
    public void update(float tpf) {
        var processors = app.getViewPort().getProcessors();

        for (SceneProcessor sp : processors) {
            if (sp instanceof GpuProfilable) {
                ((GpuProfilable) sp).setRenderPassListener(this);
            }
        }

        if (!processors.isEmpty() && processors.get(processors.size() - 1) != processor) {
            app.getViewPort().removeProcessor(processor);
            app.getViewPort().addProcessor(processor);
        }
    }

    private void startSet(BenchmarkSet set) {
        closeWriters(); // Zamknij pliki poprzedniego zestawu
        currentScenarioIndex = 0;

        try {
            // Bezpieczna nazwa pliku (usuwamy spacje)
            String safeSetName = set.getName().replace(" ", "_");

            mainCsvWriter = new PrintWriter(new FileWriter("raport_" + safeSetName + "_ogolny.csv", false));
            mainCsvWriter.println("Scenariusz,Sredni Czas Klatki (ms),Srednie FPS");

            gpuCsvWriter = new PrintWriter(new FileWriter("raport_" + safeSetName + "_gpu.csv", false));
            gpuCsvWriter.println("Scenariusz,Nazwa Shadera (Pass),Sredni Czas na GPU (ms)");

        } catch (IOException e) {
            System.err.println("Nie udało się utworzyć plików CSV: " + e.getMessage());
        }

        System.out.println(">>> ROZPOCZYNAM ZESTAW TESTOW: " + set.getName() + " <<<");
        startNextTestScenario();
    }

    private void startNextTestScenario() {
        passTimeSums.clear();
        BenchmarkSet currentSet = testSets.get(currentSetIndex);
        BenchmarkScenario scenario = currentSet.getScenarios().get(currentScenarioIndex);

        System.out.println("Rozpoczynam rozgrzewkę dla: " + scenario.getName());

        // Aplikujemy ustawienia specyficzne dla tego scenariusza
        scenario.applySetup();

        processor.resetForNextTest();
    }

    public void onTestCompleted(double avgFrameTimeMs, double avgFps) {
        BenchmarkSet currentSet = testSets.get(currentSetIndex);
        BenchmarkScenario scenario = currentSet.getScenarios().get(currentScenarioIndex);
        int frames = processor.getMeasureFrames(); // (załóżmy że dodałeś taki getter w Processorze)

        // Zapis do głównego CSV
        if (mainCsvWriter != null) {
            mainCsvWriter.printf(java.util.Locale.US, "%s,%.4f,%.2f%n",
                scenario.getName(), avgFrameTimeMs, avgFps);
            mainCsvWriter.flush();
        }

        // Zapis do CSV dla GPU - każdy pass w osobnym wierszu (idealne do wykresów!)
        if (gpuCsvWriter != null && !passTimeSums.isEmpty()) {
            passTimeSums.forEach((passName, sumMs) -> {
                double avgPassTime = sumMs / frames;
                gpuCsvWriter.printf(java.util.Locale.US, "%s,%s,%.4f%n",
                    scenario.getName(), passName, avgPassTime);
            });
            gpuCsvWriter.flush();
        }

        System.out.printf("Zakończono: %s (%.1f FPS)%n", scenario.getName(), avgFps);

        // Przejście do kolejnego scenariusza lub zestawu
        currentScenarioIndex++;
        if (currentScenarioIndex < currentSet.getScenarios().size()) {
            startNextTestScenario();
        } else {
            currentSetIndex++;
            if (currentSetIndex < testSets.size()) {
                startSet(testSets.get(currentSetIndex));
            } else {
                System.out.println("Wszystkie zestawy benchmarków zakończone pomyślnie.");
                setEnabled(false);
            }
        }
    }

    @Override
    public void onPassMeasured(String passName, double timeMs) {
        if (processor.isWarmingUp()) return;
        passTimeSums.put(passName, passTimeSums.getOrDefault(passName, 0.0) + timeMs);
    }

    @Override
    protected void cleanup(Application app) {
        closeWriters();
    }

    private void closeWriters() {
        if (mainCsvWriter != null) mainCsvWriter.close();
        if (gpuCsvWriter != null) gpuCsvWriter.close();
    }
}