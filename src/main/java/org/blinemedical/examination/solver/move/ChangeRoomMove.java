package org.blinemedical.examination.solver.move;

import org.blinemedical.examination.domain.MeetingSchedule;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

public class ChangeRoomMove extends AbstractMove<MeetingSchedule> {

    @Override
    protected AbstractMove<MeetingSchedule> createUndoMove(
        ScoreDirector<MeetingSchedule> scoreDirector) {
        return null;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<MeetingSchedule> scoreDirector) {

    }

    @Override
    public boolean isMoveDoable(ScoreDirector<MeetingSchedule> scoreDirector) {
        return false;
    }
}
