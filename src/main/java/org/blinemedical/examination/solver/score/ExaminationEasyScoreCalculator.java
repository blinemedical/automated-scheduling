package org.blinemedical.examination.solver.score;

import org.blinemedical.examination.domain.MeetingSchedule;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

public class ExaminationEasyScoreCalculator implements
    EasyScoreCalculator<MeetingSchedule, HardMediumSoftLongScore> {

    @Override
    public HardMediumSoftLongScore calculateScore(MeetingSchedule scheduleSolution) {
        return HardMediumSoftLongScore.ZERO;
    }
}
