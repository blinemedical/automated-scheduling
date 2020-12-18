package org.blinemedical.examination.domain;

import org.optaplanner.core.api.domain.constraintweight.ConstraintConfiguration;
import org.optaplanner.core.api.domain.constraintweight.ConstraintWeight;
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.examples.common.domain.AbstractPersistable;

@ConstraintConfiguration(constraintPackage = "org.blinemedical.examination.solver")
public class MeetingConstraintConfiguration extends AbstractPersistable {

    public static final String ROOM_CONFLICT = "Room conflict";
    public static final String DONT_GO_IN_OVERTIME = "Don't go in overtime";
    public static final String REQUIRED_ATTENDANCE_CONFLICT = "Required attendance conflict";

    public static final String ASSIGNED_MEETINGS = "Assigned meetings";

    public static final String DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE = "Do all meetings as soon as possible";
    public static final String ONE_TIME_GRAIN_BREAK_BETWEEN_TWO_CONSECUTIVE_MEETINGS =
        "One TimeGrain break between two consecutive meetings";
    public static final String OVERLAPPING_MEETINGS = "Overlapping meetings";
    public static final String ROOM_STABILITY = "Room stability";

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

    // Soft
    @ConstraintWeight(DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE)
    private HardMediumSoftScore doAllMeetingsAsSoonAsPossible = HardMediumSoftScore.ofSoft(1);
    @ConstraintWeight(ONE_TIME_GRAIN_BREAK_BETWEEN_TWO_CONSECUTIVE_MEETINGS)
    private HardMediumSoftScore oneTimeGrainBreakBetweenTwoConsecutiveMeetings = HardMediumSoftScore
        .ofSoft(0);
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

    public HardMediumSoftScore getOneTimeGrainBreakBetweenTwoConsecutiveMeetings() {
        return oneTimeGrainBreakBetweenTwoConsecutiveMeetings;
    }

    public void setOneTimeGrainBreakBetweenTwoConsecutiveMeetings(
        HardMediumSoftScore oneTimeGrainBreakBetweenTwoConsecutiveMeetings) {
        this.oneTimeGrainBreakBetweenTwoConsecutiveMeetings = oneTimeGrainBreakBetweenTwoConsecutiveMeetings;
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

    public HardMediumSoftScore getRoomStability() {
        return roomStability;
    }

    public void setRoomStability(HardMediumSoftScore roomStability) {
        this.roomStability = roomStability;
    }

}
