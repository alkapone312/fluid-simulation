package pl.pwr.jkaplone.fluidsim.benchmark;

import java.util.ArrayList;
import java.util.List;

public class BenchmarkSet {
    private final String name;
    private final List<BenchmarkScenario> scenarios = new ArrayList<>();

    public BenchmarkSet(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<BenchmarkScenario> getScenarios() {
        return scenarios;
    }

    public void addScenario(String name, Runnable setupLogic) {
        scenarios.add(new BenchmarkScenario(name, setupLogic));
    }
}