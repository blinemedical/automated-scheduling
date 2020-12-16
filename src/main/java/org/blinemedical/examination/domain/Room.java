package org.blinemedical.examination.domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.optaplanner.examples.common.swingui.components.Labeled;

@XStreamAlias("Room")
public class Room extends AbstractPersistable implements Labeled {

    private int capacity;
    private int penalty;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getPenalty() {
        return penalty;
    }

    public void setPenalty(int penalty) {
        this.penalty = penalty;
    }

    @Override
    public String getLabel() {
        return Long.toString(id);
    }

    @Override
    public String toString() {
        return Long.toString(id);
    }

    // ************************************************************************
    // With methods
    // ************************************************************************

    public Room withId(long id) {
        this.setId(id);
        return this;
    }

    public Room withCapacity(int capacity) {
        this.setCapacity(capacity);
        return this;
    }

    public Room withPenalty(int penalty) {
        this.setPenalty(penalty);
        return this;
    }

}
