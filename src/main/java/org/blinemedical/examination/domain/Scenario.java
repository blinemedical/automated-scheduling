package org.blinemedical.examination.domain;

import java.util.List;
import org.optaplanner.examples.common.swingui.components.Labeled;

public class Scenario extends AbstractPersistable implements Labeled {

    private String name;
    private List<Attendance> patients;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Attendance> getPatients() {
        return patients;
    }

    public void setPatients(List<Attendance> patients) {
        this.patients = patients;
    }

    @Override
    public String getLabel() {
        return this.toString();
    }
}
