package org.blinemedical.examination.domain;

import java.util.List;

public class Meeting extends AbstractPersistable {

    private boolean entireGroupMeeting;
    /**
     * Multiply by {@link TimeGrain#GRAIN_LENGTH_IN_MINUTES} to get duration in minutes.
     */
    private int durationInGrains;

    private List<Attendance> requiredAttendanceList;

    public boolean isEntireGroupMeeting() {
        return entireGroupMeeting;
    }

    public void setEntireGroupMeeting(boolean entireGroupMeeting) {
        this.entireGroupMeeting = entireGroupMeeting;
    }

    public int getDurationInGrains() {
        return durationInGrains;
    }

    public void setDurationInGrains(int durationInGrains) {
        this.durationInGrains = durationInGrains;
    }

    public List<Attendance> getRequiredAttendanceList() {
        return requiredAttendanceList;
    }

    public void setRequiredAttendanceList(List<Attendance> requiredAttendanceList) {
        this.requiredAttendanceList = requiredAttendanceList;
    }

    // ************************************************************************
    // Complex methods
    // ************************************************************************

    public int getRequiredCapacity() {
        return requiredAttendanceList.size();
    }

    public String getDurationString() {
        return (durationInGrains * TimeGrain.GRAIN_LENGTH_IN_MINUTES) + " minutes";
    }
}
