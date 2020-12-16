package org.blinemedical.examination.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.blinemedical.examination.domain.Exam;
import org.blinemedical.examination.domain.Examination;
import org.blinemedical.examination.persistence.ExaminationXmlSolutionFileIO;
import org.blinemedical.examination.solver.score.ExaminationEasyScoreCalculator;
import org.blinemedical.examination.swingui.examinationPanel;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import org.optaplanner.core.config.constructionheuristic.ConstructionHeuristicType;
import org.optaplanner.core.config.heuristic.selector.common.SelectionOrder;
import org.optaplanner.core.config.heuristic.selector.move.generic.ChangeMoveSelectorConfig;
import org.optaplanner.core.config.localsearch.LocalSearchPhaseConfig;
import org.optaplanner.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import org.optaplanner.core.config.phase.PhaseConfig;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;
import org.optaplanner.examples.common.app.CommonApp;
import org.optaplanner.examples.common.swingui.SolutionPanel;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

public class ExaminationApp extends CommonApp<Examination> {

    public static final String SOLVER_CONFIG = "org/blinemedical/examination/solver/solverConfig.xml";

    public static final String DATA_DIR_NAME = "examination";

    public ExaminationApp() {
        super("Examination Solver",
            "Creates an exam schedule for students and learners",
            SOLVER_CONFIG,
            DATA_DIR_NAME,
            examinationPanel.LOGO_PATH);
    }

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new ExaminationApp().init();
    }

    @Override
    protected SolverFactory<Examination> createSolverFactory() {
        return createSolverFactoryByXml();
    }

    /**
     * Normal way to create a {@link Solver}.
     *
     * @return never null
     */
    protected SolverFactory<Examination> createSolverFactoryByXml() {
        return SolverFactory.createFromXmlResource(SOLVER_CONFIG);
    }

    /**
     * Unused alternative. A way to create a {@link Solver} without using XML.
     * <p>
     * It is recommended to use {@link #createSolverFactoryByXml()} instead.
     *
     * @return never null
     */
    protected SolverFactory<Examination> createSolverFactoryByApi() {
        SolverConfig solverConfig = new SolverConfig();

        solverConfig.setSolutionClass(Examination.class);
        solverConfig.setEntityClassList(Collections.singletonList(Exam.class));

        ScoreDirectorFactoryConfig scoreDirectorFactoryConfig = new ScoreDirectorFactoryConfig();
        scoreDirectorFactoryConfig.setEasyScoreCalculatorClass(ExaminationEasyScoreCalculator.class);
        solverConfig.setScoreDirectorFactoryConfig(scoreDirectorFactoryConfig);

        solverConfig.setTerminationConfig(new TerminationConfig().withSecondsSpentLimit(30L));
        List<PhaseConfig> phaseConfigList = new ArrayList<>();

        ConstructionHeuristicPhaseConfig constructionHeuristicPhaseConfig = new ConstructionHeuristicPhaseConfig();
        constructionHeuristicPhaseConfig.setConstructionHeuristicType(
            ConstructionHeuristicType.FIRST_FIT);
        phaseConfigList.add(constructionHeuristicPhaseConfig);

        LocalSearchPhaseConfig localSearchPhaseConfig = new LocalSearchPhaseConfig();
        ChangeMoveSelectorConfig changeMoveSelectorConfig = new ChangeMoveSelectorConfig();
        changeMoveSelectorConfig.setSelectionOrder(SelectionOrder.ORIGINAL);
        localSearchPhaseConfig.setMoveSelectorConfig(changeMoveSelectorConfig);
        localSearchPhaseConfig
            .setAcceptorConfig(new LocalSearchAcceptorConfig()
                .withEntityTabuSize(5)
            );
        phaseConfigList.add(localSearchPhaseConfig);

        solverConfig.setPhaseConfigList(phaseConfigList);
        return SolverFactory.create(solverConfig);
    }

    @Override
    protected SolutionPanel<Examination> createSolutionPanel() {
        return new examinationPanel();
    }

    @Override
    public SolutionFileIO<Examination> createSolutionFileIO() {
        return new ExaminationXmlSolutionFileIO();
    }
}
