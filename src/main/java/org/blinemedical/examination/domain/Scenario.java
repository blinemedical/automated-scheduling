package org.blinemedical.examination.domain;

import java.util.List;

public class Scenario extends AbstractPersistable {

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
}
