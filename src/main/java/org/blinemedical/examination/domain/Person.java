package org.blinemedical.examination.domain;

import org.optaplanner.examples.common.domain.AbstractPersistable;
import org.optaplanner.examples.common.swingui.components.Labeled;

public class Person extends AbstractPersistable implements Labeled {

    private String fullName;
    private boolean patient;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public boolean isPatient() {
        return patient;
    }

    public void setPatient(boolean patient) {
        this.patient = patient;
    }

    @Override
    public String getLabel() {
        return fullName + " (" + (patient ? "P" : "L") + ")";
    }

    @Override
    public String toString() {
        return fullName + " (" + (patient ? "P" : "L") + ")";
    }

}
