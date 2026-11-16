package com.myjavacardgame;

import java.util.Objects;

/** Immutable, explicit selection of normal data or a separately configured QA scenario. */
public record LaunchOptions(Scenario scenario) {
    /** Available launch profiles; never changes domain rules. */
    public enum Scenario { NORMAL, QA_VICTORY, QA_DEFEAT }

    /** Requires a known profile. */
    public LaunchOptions { Objects.requireNonNull(scenario, "scenario"); }

    /** Parses an optional single --qa=victory or --qa=defeat argument. */
    public static LaunchOptions parse(String[] args) {
        if (args.length == 0) return new LaunchOptions(Scenario.NORMAL);
        if (args.length != 1) throw new IllegalArgumentException("Укажите только один параметр --qa=victory или --qa=defeat");
        return new LaunchOptions(switch (args[0]) {
            case "--qa=victory" -> Scenario.QA_VICTORY;
            case "--qa=defeat" -> Scenario.QA_DEFEAT;
            default -> throw new IllegalArgumentException("Неизвестный параметр. Допустимы --qa=victory и --qa=defeat");
        });
    }

    /** Returns the selected setup resource without accessing files. */
    public String setupPath() {
        return scenario == Scenario.NORMAL ? "data/solo-setup.json"
                : "data/qa/solo-setup-" + suffix() + ".json";
    }

    /** Returns the selected project resource without accessing files. */
    public String projectPath() {
        return scenario == Scenario.NORMAL ? "data/fantasy-city-project.json" : "data/qa/" + suffix() + "-project.json";
    }

    /** Returns an unmistakable QA label, or empty text for normal gameplay. */
    public String label() {
        return switch (scenario) {
            case NORMAL -> "";
            case QA_VICTORY -> "ТЕСТ: ПОБЕДА";
            case QA_DEFEAT -> "ТЕСТ: ПОРАЖЕНИЕ";
        };
    }

    private String suffix() { return scenario == Scenario.QA_VICTORY ? "victory" : "defeat"; }
}
