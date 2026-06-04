package pl.pwr.jkaplone.fluidsim.benchmark;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.post.SceneProcessor;
import pl.pwr.jkaplone.fluidsim.Simulation3D;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;

public class BenchmarkState extends BaseAppState implements RenderPassListener {

    private Simulation3D app;
    private BenchmarkProcessor processor;

    private List<BenchmarkSet> testSets = new ArrayList<>();
    private int currentSetIndex = 0;
    private int currentScenarioIndex = 0;

    private PrintWriter mainCsvWriter;
    private PrintWriter gpuCsvWriter;
    private PrintWriter frameTimeCsvWriter; // Nowy writer dla surowych czasów klatek

    private Map<String, List<Double>> passTimeSamples = new LinkedHashMap<>();

    public BenchmarkState() {
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
        closeWriters();
        currentScenarioIndex = 0;

        try {
            String safeSetName = set.getName().replace(" ", "_");

            // Rozszerzone nagłówki dla głównego CSV
            mainCsvWriter = new PrintWriter(new FileWriter("raport_" + safeSetName + "_ogolny.csv", false));
            mainCsvWriter.println("Scenariusz,Sredni Czas Klatki (ms),Srednie FPS,Odchylenie Std (ms),Min (ms),Max (ms),1% Low (ms),0.1% Low (ms)");

            // Rozszerzone nagłówki dla GPU CSV
            gpuCsvWriter = new PrintWriter(new FileWriter("raport_" + safeSetName + "_gpu.csv", false));
            gpuCsvWriter.println("Scenariusz,Nazwa Shadera (Pass),Sredni Czas (ms),Odchylenie Std (ms),Min (ms),Max (ms)");

            // Inicjalizacja pliku na surowe czasy klatek
            frameTimeCsvWriter = new PrintWriter(new FileWriter("raport_" + safeSetName + "_surowe_klatki.csv", false));
            frameTimeCsvWriter.println("Scenariusz,Numer Klatki,Czas Klatki (ms)");

        } catch (IOException e) {
            System.err.println("Nie udało się utworzyć plików CSV: " + e.getMessage());
        }

        System.out.println(">>> ROZPOCZYNAM ZESTAW TESTOW: " + set.getName() + " <<<");
        startNextTestScenario();
    }

    private void startNextTestScenario() {
        passTimeSamples.clear();
        BenchmarkSet currentSet = testSets.get(currentSetIndex);
        BenchmarkScenario scenario = currentSet.getScenarios().get(currentScenarioIndex);

        System.out.println("Rozpoczynam rozgrzewkę dla: " + scenario.getName());
        scenario.applySetup();
        processor.resetForNextTest();
    }

    public void onTestCompleted(List<Double> frameTimeSamples) {
        BenchmarkSet currentSet = testSets.get(currentSetIndex);
        BenchmarkScenario scenario = currentSet.getScenarios().get(currentScenarioIndex);

        if (frameTimeSamples == null || frameTimeSamples.isEmpty()) {
            System.err.println("Brak danych pomiarowych dla scenariusza: " + scenario.getName());
            return;
        }

        // --- ZAPIS SUROWYCH CZASÓW KLATEK DO OSOBNEGO PLIKU ---
        if (frameTimeCsvWriter != null) {
            for (int i = 0; i < frameTimeSamples.size(); i++) {
                frameTimeCsvWriter.printf(java.util.Locale.US, "%s,%d,%.4f%n",
                    scenario.getName(), (i + 1), frameTimeSamples.get(i));
            }
            frameTimeCsvWriter.flush(); // Wymuszenie zapisu na dysk po każdym scenariuszu
        }

        // --- OBLICZENIA DLA CAŁEJ KLATKI ---
        double avgFrameTime = calculateAverage(frameTimeSamples);
        double avgFps = 1000.0 / avgFrameTime;
        double stdDevFrame = calculateStandardDeviation(frameTimeSamples, avgFrameTime);
        double minFrame = Collections.min(frameTimeSamples);
        double maxFrame = Collections.max(frameTimeSamples);
        double onePercentLow = calculatePercentileLow(frameTimeSamples, 0.99);
        double zeroOnePercentLow = calculatePercentileLow(frameTimeSamples, 0.999);

        // Zapis do głównego CSV
        if (mainCsvWriter != null) {
            mainCsvWriter.printf(java.util.Locale.US, "%s,%.4f,%.2f,%.4f,%.4f,%.4f,%.4f,%.4f%n",
                scenario.getName(), avgFrameTime, avgFps, stdDevFrame, minFrame, maxFrame, onePercentLow, zeroOnePercentLow);
            mainCsvWriter.flush();
        }

        // --- OBLICZENIA DLA POSZCZEGÓLNYCH PASSÓW GPU ---
        if (gpuCsvWriter != null && !passTimeSamples.isEmpty()) {
            passTimeSamples.forEach((passName, samples) -> {
                double avgPassTime = calculateAverage(samples);
                double stdDevPass = calculateStandardDeviation(samples, avgPassTime);
                double minPass = Collections.min(samples);
                double maxPass = Collections.max(samples);

                gpuCsvWriter.printf(java.util.Locale.US, "%s,%s,%.4f,%.4f,%.4f,%.4f%n",
                    scenario.getName(), passName, avgPassTime, stdDevPass, minPass, maxPass);
            });
            gpuCsvWriter.flush();
        }

        System.out.printf("Zakończono: %s (Śr: %.2f ms, StdDev: %.2f ms, %.1f FPS)%n",
            scenario.getName(), avgFrameTime, stdDevFrame, avgFps);

        // Potok przejścia do kolejnych testów
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
                System.exit(0);
            }
        }
    }

    @Override
    public void onPassMeasured(String passName, double timeMs) {
        if (processor.isWarmingUp()) return;

        // Dodajemy próbkę do listy skojarzonej z danym shaderem
        passTimeSamples.computeIfAbsent(passName, k -> new ArrayList<>()).add(timeMs);
    }

    // --- PRYWATNE METODY STATYSTYCZNE ---

    private double calculateAverage(List<Double> samples) {
        double sum = 0;
        for (double val : samples) sum += val;
        return sum / samples.size();
    }

    private double calculateStandardDeviation(List<Double> samples, double mean) {
        if (samples.size() <= 1) return 0.0;
        double varianceSum = 0;
        for (double val : samples) {
            varianceSum += Math.pow(val - mean, 2);
        }
        return Math.sqrt(varianceSum / samples.size());
    }

    private double calculatePercentileLow(List<Double> samples, double percentile) {
        if (samples.isEmpty()) return 0.0;
        List<Double> sorted = new ArrayList<>(samples);
        Collections.sort(sorted);

        int index = (int) Math.round(percentile * (sorted.size() - 1));
        return sorted.get(index);
    }

    @Override
    protected void cleanup(Application app) {
        closeWriters();
    }

    private void closeWriters() {
        if (mainCsvWriter != null) mainCsvWriter.close();
        if (gpuCsvWriter != null) gpuCsvWriter.close();
        if (frameTimeCsvWriter != null) frameTimeCsvWriter.close(); // Pamiętamy o zamknięciu streamu
    }
}