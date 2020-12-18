package org.blinemedical.examination.domain;

import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.examples.common.domain.AbstractPersistable;

@ConstraintConfiguration(constraintPackage = "org.blinemedical.examination.solver")
public class MeetingConstraintConfiguration extends AbstractPersistable {
    // Drools rule names
    public static final String ROOM_CONFLICT = "Room conflict";
    public static final String DONT_GO_IN_OVERTIME = "Don't go in overtime";
    public static final String REQUIRED_ATTENDANCE_CONFLICT = "Required attendance conflict";

    public static final String ASSIGNED_MEETINGS = "Assigned meetings";
    public static final String HALF_ASSIGNED_MEETINGS = "Half assigned meetings";

    public static final String DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE = "Do all meetings as soon as possible";
    public static final String OVERLAPPING_MEETINGS = "Overlapping meetings";
    public static final String ROOM_STABILITY = "Room stability";

    // Descriptions
    public static final String ROOM_CONFLICT_DESCRIPTION = "Two meetings must not use the same room at the same time.";
    public static final String DONT_GO_IN_OVERTIME_DESCRIPTION  = "Meetings should start and end within the specified time window for the day";
    public static final String REQUIRED_ATTENDANCE_CONFLICT_DESCRIPTION  = "A person cannot have two required meetings at the same time.";

    public static final String ASSIGNED_MEETINGS_DESCRIPTION  = "Assign one meeting per Learner-Scenario combination";
    public static final String HALF_ASSIGNED_MEETINGS_DESCRIPTION  = "Do not assign only a room or only a time grain to meetings. They should be fully assigned or completely unassigned";

    public static final String DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE_DESCRIPTION  = "Schedule all meetings as soon as possible.";
    public static final String OVERLAPPING_MEETINGS_DESCRIPTION  = "To minimize the number of meetings in parallel so people don’t have to choose one meeting over the other.";
    public static final String ROOM_STABILITY_DESCRIPTION  = "If a person has two consecutive meetings with two or less time grains break between them they better be in the same room.";

    // Hard
    @ConstraintWeight(ROOM_CONFLICT)
    private HardMediumSoftScore roomConflict = HardMediumSoftScore.ofHard(1);
    @ConstraintWeight(DONT_GO_IN_OVERTIME)
    private HardMediumSoftScore dontGoInOvertime = HardMediumSoftScore.ofHard(1);
    @ConstraintWeight(REQUIRED_ATTENDANCE_CONFLICT)
    private HardMediumSoftScore requiredAttendanceConflict = HardMediumSoftScore.ofHard(1);

    // Medium
    @ConstraintWeight(ASSIGNED_MEETINGS)
    private HardMediumSoftScore assignedMeetings = HardMediumSoftScore.ofMedium(1);
    @ConstraintWeight(HALF_ASSIGNED_MEETINGS)
    private HardMediumSoftScore halfAssignedMeetings = HardMediumSoftScore.ofMedium(1);

    // Soft
    @ConstraintWeight(DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE)
    private HardMediumSoftScore doAllMeetingsAsSoonAsPossible = HardMediumSoftScore.ofSoft(1);
    @ConstraintWeight(OVERLAPPING_MEETINGS)
    private HardMediumSoftScore overlappingMeetings = HardMediumSoftScore.ofSoft(10);
    @ConstraintWeight(ROOM_STABILITY)
    private HardMediumSoftScore roomStability = HardMediumSoftScore.ofSoft(1);

    public MeetingConstraintConfiguration() {
    }

    public MeetingConstraintConfiguration(long id) {
        super(id);
    }

    // ************************************************************************
    // Simple getters and setters
    // ************************************************************************

    public HardMediumSoftScore getRoomConflict() {
        return roomConflict;
    }

    public void setRoomConflict(HardMediumSoftScore roomConflict) {
        this.roomConflict = roomConflict;
    }

    public HardMediumSoftScore getDontGoInOvertime() {
        return dontGoInOvertime;
    }

    public void setDontGoInOvertime(HardMediumSoftScore dontGoInOvertime) {
        this.dontGoInOvertime = dontGoInOvertime;
    }

    public HardMediumSoftScore getRequiredAttendanceConflict() {
        return requiredAttendanceConflict;
    }

    public void setRequiredAttendanceConflict(HardMediumSoftScore requiredAttendanceConflict) {
        this.requiredAttendanceConflict = requiredAttendanceConflict;
    }

    public HardMediumSoftScore getDoAllMeetingsAsSoonAsPossible() {
        return doAllMeetingsAsSoonAsPossible;
    }

    public void setDoAllMeetingsAsSoonAsPossible(
        HardMediumSoftScore doAllMeetingsAsSoonAsPossible) {
        this.doAllMeetingsAsSoonAsPossible = doAllMeetingsAsSoonAsPossible;
    }

    public HardMediumSoftScore getOverlappingMeetings() {
        return overlappingMeetings;
    }

    public void setOverlappingMeetings(HardMediumSoftScore overlappingMeetings) {
        this.overlappingMeetings = overlappingMeetings;
    }

    public HardMediumSoftScore getAssignedMeetings() {
        return assignedMeetings;
    }

    public void setAssignedMeetings(HardMediumSoftScore assignedMeetings) {
        this.assignedMeetings = assignedMeetings;
    }

    public HardMediumSoftScore getHalfAssignedMeetings() {
        return halfAssignedMeetings;
    }

    public void setHalfAssignedMeetings(HardMediumSoftScore halfAssignedMeetings) {
        this.halfAssignedMeetings = halfAssignedMeetings;
    }

    public HardMediumSoftScore getRoomStability() {
        return roomStability;
    }

    public void setRoomStability(HardMediumSoftScore roomStability) {
        this.roomStability = roomStability;
    }

}
