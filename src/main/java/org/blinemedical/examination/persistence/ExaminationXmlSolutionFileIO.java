package org.blinemedical.examination.persistence;

import org.blinemedical.examination.domain.Examination;
import org.optaplanner.persistence.xstream.impl.domain.solution.XStreamSolutionFileIO;

public class ExaminationXmlSolutionFileIO extends XStreamSolutionFileIO<Examination> {

    public ExaminationXmlSolutionFileIO() {
        super(Examination.class);
    }
}