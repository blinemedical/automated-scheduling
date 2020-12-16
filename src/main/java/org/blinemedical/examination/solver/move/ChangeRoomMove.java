package org.blinemedical.examination.solver.move;

import org.blinemedical.examination.domain.Examination;
import org.optaplanner.core.api.score.director.ScoreDirector;
import org.optaplanner.core.impl.heuristic.move.AbstractMove;

public class ChangeRoomMove extends AbstractMove<Examination> {

    @Override
    protected AbstractMove<Examination> createUndoMove(
        ScoreDirector<Examination> scoreDirector) {
        return null;
    }

    @Override
    protected void doMoveOnGenuineVariables(ScoreDirector<Examination> scoreDirector) {

    }

    @Override
    public boolean isMoveDoable(ScoreDirector<Examination> scoreDirector) {
        return false;
    }
}
