package org.blinemedical.examination.solver.score;

import org.blinemedical.examination.domain.Examination;
import org.optaplanner.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import org.optaplanner.core.api.score.calculator.EasyScoreCalculator;

public class ExaminationEasyScoreCalculator implements
    EasyScoreCalculator<Examination, HardMediumSoftLongScore> {

    @Override
    public HardMediumSoftLongScore calculateScore(Examination scheduleSolution) {
        return HardMediumSoftLongScore.ZERO;
    }
}
