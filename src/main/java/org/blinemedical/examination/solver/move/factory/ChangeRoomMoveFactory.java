package org.blinemedical.examination.solver.move.factory;

import java.util.List;
import org.blinemedical.examination.domain.MeetingSchedule;
import org.optaplanner.core.impl.heuristic.move.Move;
import org.optaplanner.core.impl.heuristic.selector.move.factory.MoveListFactory;

public class ChangeRoomMoveFactory implements MoveListFactory<MeetingSchedule> {

    @Override
    public List<? extends Move<MeetingSchedule>> createMoveList(
        MeetingSchedule scheduleSolution) {
        return null;
    }
}
