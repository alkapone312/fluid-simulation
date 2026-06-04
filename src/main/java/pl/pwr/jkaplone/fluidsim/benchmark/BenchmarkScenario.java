package pl.pwr.jkaplone.fluidsim.benchmark;

public class BenchmarkScenario {
    private final String name;
    private final Runnable setupLogic;

    public BenchmarkScenario(String name, Runnable setupLogic) {
        this.name = name;
        this.setupLogic = setupLogic;
    }

    public String getName() {
        return name;
    }

    public void applySetup() {
        if (setupLogic != null) {
            setupLogic.run();
        }
    }
}