package org.blinemedical.examination.domain;

import java.util.Objects;

/**
 * Used in Drools scoring
 */
public class AssignedScenarioLearner {

    private Scenario scenario;
    private Person learner;

    public AssignedScenarioLearner(Scenario scenario, Person learner) {
        this.scenario = scenario;
        this.learner = learner;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public Person getLearner() {
        return learner;
    }

    public void setLearner(Person learner) {
        this.learner = learner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final AssignedScenarioLearner other = (AssignedScenarioLearner) o;
        return Objects.equals(scenario, other.scenario) &&
            Objects.equals(learner, other.learner);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scenario, learner);
    }

    @Override
    public String toString() {
        return "AssignedScenarioLearner{" +
            "scenario=" + scenario +
            ", person=" + learner +
            '}';
    }
}
