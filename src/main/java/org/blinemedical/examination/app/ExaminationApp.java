package org.blinemedical.examination.app;

import org.blinemedical.examination.domain.MeetingSchedule;
import org.blinemedical.examination.persistence.MeetingSchedulingXlsxFileIO;
import org.blinemedical.examination.swingui.MeetingSchedulingPanel;
import org.optaplanner.examples.common.app.CommonApp;
import org.optaplanner.examples.common.swingui.SolutionPanel;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

public class ExaminationApp extends CommonApp<MeetingSchedule> {

    public static final String SOLVER_CONFIG = "org/blinemedical/examination/solver/solverConfig.xml";

    public static final String DATA_DIR_NAME = "examination";

    public ExaminationApp() {
        super("Examination Schedule Solver",
            "Creates an exam schedule for SPs and learners",
            SOLVER_CONFIG,
            DATA_DIR_NAME,
            MeetingSchedulingPanel.LOGO_PATH);
    }

    public static void main(String[] args) {
        prepareSwingEnvironment();
        new ExaminationApp().init();
    }

    @Override
    protected SolutionPanel<MeetingSchedule> createSolutionPanel() {
        return new MeetingSchedulingPanel();
    }

    @Override
    public SolutionFileIO<MeetingSchedule> createSolutionFileIO() {
        return new MeetingSchedulingXlsxFileIO();
    }
}
