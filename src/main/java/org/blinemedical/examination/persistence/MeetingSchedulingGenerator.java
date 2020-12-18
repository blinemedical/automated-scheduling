package org.blinemedical.examination.persistence;

import static org.blinemedical.examination.domain.TimeGrain.GRAIN_LENGTH_IN_MINUTES;

import java.io.File;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blinemedical.examination.app.ExaminationApp;
import org.blinemedical.examination.domain.Attendance;
import org.blinemedical.examination.domain.Day;
import org.blinemedical.examination.domain.Meeting;
import org.blinemedical.examination.domain.MeetingAssignment;
import org.blinemedical.examination.domain.MeetingConstraintConfiguration;
import org.blinemedical.examination.domain.MeetingSchedule;
import org.blinemedical.examination.domain.Person;
import org.blinemedical.examination.domain.Room;
import org.blinemedical.examination.domain.Scenario;
import org.blinemedical.examination.domain.TimeGrain;
import org.optaplanner.examples.common.app.CommonApp;
import org.optaplanner.examples.common.persistence.AbstractSolutionImporter;
import org.optaplanner.examples.common.persistence.generator.StringDataGenerator;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

public class MeetingSchedulingGenerator {

    private static final Logger logger = LogManager.getLogger(MeetingSchedulingGenerator.class);

    public static void main(String[] args) {
        MeetingSchedulingGenerator generator = new MeetingSchedulingGenerator();

        Instant startTime = Instant.parse("2020-12-18T08:00:00.00Z");
        Instant endTime = Instant.parse("2020-12-18T16:00:00.00Z");
        Duration meetingDuration = Duration.ofHours(1L);
        int meetingDurationInGrains = (int) (meetingDuration.toMinutes() / GRAIN_LENGTH_IN_MINUTES);

        generator.writeMeetingSchedule(2, 2, 2, 1, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(7, 3, 2, 5, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(10, 4, 3, 6, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(15, 4, 4, 7, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(20, 5, 4, 10, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(30, 6, 5, 15, startTime, endTime, meetingDurationInGrains);
    }

    private final StringDataGenerator fullNameGenerator = StringDataGenerator.buildFullNames();

    protected final SolutionFileIO<MeetingSchedule> solutionFileIO;
    protected final File outputDir;

    protected Random random;

    public MeetingSchedulingGenerator() {
        solutionFileIO = new MeetingSchedulingXlsxFileIO();
        outputDir = new File(CommonApp.determineDataDir(ExaminationApp.DATA_DIR_NAME), "unsolved");
    }

    private void writeMeetingSchedule(int learnersListSize, int numScenarios,
        int patientsPerScenario, int roomListSize,
        Instant startTime,
        Instant endTime, int durationInGrains) {
        Duration meetingDuration = Duration.between(startTime, endTime);
        int timeGrainListSize = (int) (meetingDuration.toMinutes() / GRAIN_LENGTH_IN_MINUTES);

        String fileName = determineFileName(
            learnersListSize,
            numScenarios,
            patientsPerScenario,
            roomListSize);
        File outputFile = new File(outputDir,
            fileName + "." + solutionFileIO.getOutputFileExtension());
        MeetingSchedule meetingSchedule = createMeetingSchedule(
            fileName,
            learnersListSize,
            numScenarios,
            patientsPerScenario,
            startTime,
            timeGrainListSize,
            roomListSize,
            durationInGrains);
        solutionFileIO.write(meetingSchedule, outputFile);
        logger.info("Saved: {}", outputFile);
    }

    private String determineFileName(int learnersListSize, int numScenarios,
        int patientsPerScenario, int roomListSize) {
        return "FAKE-"
            + learnersListSize + "L-"
            + numScenarios + "SC-"
            + patientsPerScenario + "SP-"
            + roomListSize + "R";
    }

    public MeetingSchedule createMeetingSchedule(
        String fileName,
        int learnersListSize,
        int numScenarios,
        int patientsPerScenario,
        Instant startTime,
        int timeGrainListSize,
        int roomListSize,
        int durationInGrains) {

        random = new Random(37);
        MeetingSchedule meetingSchedule = new MeetingSchedule();
        meetingSchedule.setId(0L);
        MeetingConstraintConfiguration constraintConfiguration = new MeetingConstraintConfiguration();
        constraintConfiguration.setId(0L);
        meetingSchedule.setConstraintConfiguration(constraintConfiguration);

        meetingSchedule.setAttendanceList(new ArrayList<>());
        meetingSchedule.setPersonList(new ArrayList<>());

        List<Attendance> learnerList = createLearners(meetingSchedule, learnersListSize);
        createScenariosAndPatients(meetingSchedule, learnersListSize, numScenarios, patientsPerScenario);
        createMeetingListAndAttendanceList(meetingSchedule, learnerList,
            durationInGrains);
        createTimeGrainList(meetingSchedule, startTime, timeGrainListSize);
        createRoomList(meetingSchedule, roomListSize);
        createMeetingAssignmentList(meetingSchedule);

        BigInteger possibleSolutionSize = BigInteger
            .valueOf((long) timeGrainListSize * roomListSize)
            .pow(meetingSchedule.getMeetingAssignmentList().size());
        logger.info(
            "MeetingSchedule {} has {} learners, {} scenarios, {} patients per scenario, {} timeGrains and {} rooms with a search space of {}.",
            fileName,
            learnersListSize,
            numScenarios,
            patientsPerScenario,
            timeGrainListSize,
            roomListSize,
            AbstractSolutionImporter.getFlooredPossibleSolutionSize(possibleSolutionSize));
        return meetingSchedule;
    }

    private List<Attendance> createLearners(MeetingSchedule meetingSchedule, int learnersListSize) {
        List<Attendance> learnerList = new ArrayList<>(learnersListSize);
        List<Person> personList = new ArrayList<>();

        long attendanceId = 0L;
        long personId = 0L;

        for (int learnerIdx = 0; learnerIdx < learnersListSize; learnerIdx++) {
            Attendance learner = new Attendance();
            learner.setId(attendanceId);
            attendanceId++;

            Person person = createPerson(personId++);
            person.setPatient(false);
            learner.setPerson(person);
            personList.add(person);

            learnerList.add(learner);
        }

        meetingSchedule.getAttendanceList().addAll(learnerList);
        meetingSchedule.getPersonList().addAll(personList);

        return learnerList;
    }

    private void createScenariosAndPatients(MeetingSchedule meetingSchedule, int learnersListSize, int numScenarios,
        int patientsPerScenario) {

        List<Scenario> scenarioList = new ArrayList<>(numScenarios);
        List<Attendance> patientList = new ArrayList<>(numScenarios * patientsPerScenario);
        List<Person> personList = new ArrayList<>();

        long scenarioId = 0L;
        long attendanceId = learnersListSize;
        long personId = learnersListSize;

        for (int scenarioIdx = 0; scenarioIdx < numScenarios; scenarioIdx++) {
            Scenario scenario = new Scenario();
            scenario.setId(scenarioId);
            scenario.setName("Scenario-" + scenarioId);
            scenarioId++;

            scenario.setPatients(new ArrayList<>());

            for (int patientIdx = 0; patientIdx < patientsPerScenario; patientIdx++) {
                Attendance patient = new Attendance();
                patient.setId(attendanceId++);

                Person person = createPerson(personId++);
                person.setPatient(true);
                patient.setPerson(person);
                personList.add(person);

                patientList.add(patient);
                scenario.getPatients().add(patient);
            }

            scenarioList.add(scenario);
            logger.trace("Created scenario with name ({}) and ({}) patients.",
                scenario.getName(), scenario.getPatients().size());
        }

        meetingSchedule.setScenarioList(scenarioList);
        meetingSchedule.getAttendanceList().addAll(patientList);
        meetingSchedule.getPersonList().addAll(personList);
    }

    public static void createMeetingListAndAttendanceList(MeetingSchedule meetingSchedule,
        List<Attendance> learnerList, int durationInGrains) {

        List<Meeting> meetingList = new ArrayList<>();
        long meetingId = 0L;

        for(Attendance learner : learnerList) {
            for(Scenario scenario : meetingSchedule.getScenarioList()) {
                for(Attendance patient : scenario.getPatients()) {
                    Meeting meeting = new Meeting();
                    meeting.setId(meetingId++);
                    meeting.setDurationInGrains(durationInGrains);
                    meeting.setScenarioId(scenario.getId());

                    meeting.setRequiredLearner(learner);
                    meeting.setRequiredPatient(patient);

                    learner.setMeeting(meeting);
                    patient.setMeeting(meeting);

                    logger.trace("Created meeting with durationInGrains ({}),"
                            + " requiredLearner ({}),"
                            + " requiredPatient ({}).",
                        durationInGrains,
                        learner,
                        patient);
                    meetingList.add(meeting);
                }
            }
        }

        meetingSchedule.setMeetingList(meetingList);
    }

    public static void createTimeGrainList(MeetingSchedule meetingSchedule, Instant startTime,
        int timeGrainListSize) {
        List<Day> dayList = new ArrayList<>(timeGrainListSize);
        long dayId = 0;
        Day day = null;
        List<TimeGrain> timeGrainList = new ArrayList<>(timeGrainListSize);
        int[] startingMinuteOfDayOptions = getStartingMinuteOfDayOptions(startTime,
            timeGrainListSize);
        for (int i = 0; i < timeGrainListSize; i++) {
            TimeGrain timeGrain = new TimeGrain();
            timeGrain.setId((long) i);
            timeGrain.setGrainIndex(i);
            int dayOfYear = (i / startingMinuteOfDayOptions.length) + 1;
            if (day == null || day.getDayOfYear() != dayOfYear) {
                day = new Day();
                day.setId(dayId);
                day.setDayOfYear(dayOfYear);
                dayId++;
                dayList.add(day);
            }
            timeGrain.setDay(day);
            int startingMinuteOfDay = startingMinuteOfDayOptions[i
                % startingMinuteOfDayOptions.length];
            timeGrain.setStartingMinuteOfDay(startingMinuteOfDay);
            logger.trace(
                "Created timeGrain with grainIndex ({}), dayOfYear ({}), startingMinuteOfDay ({}).",
                i, dayOfYear, startingMinuteOfDay);
            timeGrainList.add(timeGrain);
        }
        meetingSchedule.setDayList(dayList);
        meetingSchedule.setTimeGrainList(timeGrainList);
    }

    public static int[] getStartingMinuteOfDayOptions(Instant startTime, int timeGrainListSize) {
        int[] options = new int[timeGrainListSize];

        for (int i = 0; i < timeGrainListSize; i++) {
            Instant startingTimeOption = startTime.plus(Duration.ofMinutes(
                (long) GRAIN_LENGTH_IN_MINUTES * i));
            ZonedDateTime zonedStartingTimeOption = startingTimeOption.atZone(ZoneOffset.UTC);
            options[i] =
                zonedStartingTimeOption.getHour() * 60 + zonedStartingTimeOption.getMinute();
        }

        return options;
    }

    private void createRoomList(MeetingSchedule meetingSchedule, int roomListSize) {
        final int roomsPerFloor = 20;
        List<Room> roomList = new ArrayList<>(roomListSize);
        for (int i = 0; i < roomListSize; i++) {
            Room room = new Room();
            room.setId((long) i);
            String name = "Room-" + ((i / roomsPerFloor * 100) + (i % roomsPerFloor) + 1);
            room.setName(name);
            logger.trace("Created room with name ({}).", name);
            roomList.add(room);
        }
        meetingSchedule.setRoomList(roomList);
    }

    private Person createPerson(long id) {
        Person person = new Person();
        person.setId(id);
        String fullName = fullNameGenerator.generateNextValue();
        person.setFullName(fullName);
        logger.trace("Created person with fullName ({}).", fullName);
        return person;
    }

    private void createMeetingAssignmentList(MeetingSchedule meetingSchedule) {
        List<Meeting> meetingList = meetingSchedule.getMeetingList();
        List<MeetingAssignment> meetingAssignmentList = new ArrayList<>(meetingList.size());
        for (Meeting meeting : meetingList) {
            MeetingAssignment meetingAssignment = new MeetingAssignment();
            meetingAssignment.setId(meeting.getId());
            meetingAssignment.setMeeting(meeting);
            meetingAssignmentList.add(meetingAssignment);
        }
        meetingSchedule.setMeetingAssignmentList(meetingAssignmentList);
    }

}
