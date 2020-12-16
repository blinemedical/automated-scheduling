package org.blinemedical.examination.domain;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
@XStreamAlias("Exam")
public class Exam extends AbstractPersistable {

    // Planning variables: changes during planning, between score calculations.
    protected Room room;

    @PlanningVariable(valueRangeProviderRefs = { "roomRange" })
    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }
}
