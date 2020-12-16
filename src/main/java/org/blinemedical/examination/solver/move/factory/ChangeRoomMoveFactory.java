package org.blinemedical.examination.solver.move.factory;

import java.util.List;
import org.blinemedical.examination.domain.Examination;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveListFactory;

public class ChangeRoomMoveFactory implements MoveListFactory<Examination> {

    @Override
    public List<? extends Move<Examination>> createMoveList(
        Examination scheduleSolution) {
        return null;
    }
}
