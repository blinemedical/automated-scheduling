package org.blinemedical.examination.domain;

public class Meeting extends AbstractPersistable {

    /**
     * Multiply by {@link TimeGrain#GRAIN_LENGTH_IN_MINUTES} to get duration in minutes.
     */
    private int durationInGrains;

    private Attendance requiredLearner;
    private Attendance requiredPatient;
    private Long scenarioId;

    public int getDurationInGrains() {
        return durationInGrains;
    }

    public void setDurationInGrains(int durationInGrains) {
        this.durationInGrains = durationInGrains;
    }

    public Attendance getRequiredLearner() {
        return requiredLearner;
    }

    public void setRequiredLearner(Attendance requiredLearner) {
        this.requiredLearner = requiredLearner;
    }

    public Attendance getRequiredPatient() {
        return requiredPatient;
    }

    public void setRequiredPatient(Attendance requiredPatient) {
        this.requiredPatient = requiredPatient;
    }

    public Long getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(Long scenarioId) {
        this.scenarioId = scenarioId;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public String getDurationString() {
        return (durationInGrains * TimeGrain.GRAIN_LENGTH_IN_MINUTES) + " minutes";
    }
}
