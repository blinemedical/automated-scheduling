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

        generator.writeMeetingSchedule(7, 5, 5, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(10, 10,6, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(15, 13,7, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(20, 20,10, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(30, 26,15, startTime, endTime, meetingDurationInGrains);
    }

    private final StringDataGenerator fullNameGenerator = StringDataGenerator.buildFullNames();

    protected final SolutionFileIO<MeetingSchedule> solutionFileIO;
    protected final File outputDir;

    protected Random random;

    public MeetingSchedulingGenerator() {
        solutionFileIO = new MeetingSchedulingXlsxFileIO();
        outputDir = new File(CommonApp.determineDataDir(ExaminationApp.DATA_DIR_NAME), "unsolved");
    }

    private void writeMeetingSchedule(int learnersListSize, int patientsListSize, int roomListSize,
        Instant startTime,
        Instant endTime, int durationInGrains) {
        Duration meetingDuration = Duration.between(startTime, endTime);
        int timeGrainListSize = (int) meetingDuration.dividedBy(GRAIN_LENGTH_IN_MINUTES)
            .toMinutes();

        String fileName = determineFileName(learnersListSize, patientsListSize, roomListSize);
        File outputFile = new File(outputDir,
            fileName + "." + solutionFileIO.getOutputFileExtension());
        MeetingSchedule meetingSchedule = createMeetingSchedule(fileName, learnersListSize,
            patientsListSize,
            startTime, timeGrainListSize, roomListSize, durationInGrains);
        solutionFileIO.write(meetingSchedule, outputFile);
        logger.info("Saved: {}", outputFile);
    }

    private String determineFileName(int learnersListSize, int patientsListSize, int roomListSize) {
        return learnersListSize + "L-"
            + patientsListSize + "SP-"
            + roomListSize + "R";
    }

    public MeetingSchedule createMeetingSchedule(String fileName, int learnersListSize,
        int patientsListSize,
        Instant startTime, int timeGrainListSize,
        int roomListSize, int durationInGrains) {
        random = new Random(37);
        MeetingSchedule meetingSchedule = new MeetingSchedule();
        meetingSchedule.setId(0L);
        MeetingConstraintConfiguration constraintConfiguration = new MeetingConstraintConfiguration();
        constraintConfiguration.setId(0L);
        meetingSchedule.setConstraintConfiguration(constraintConfiguration);

        createMeetingListAndAttendanceList(meetingSchedule, learnersListSize, patientsListSize,
            durationInGrains);
        createTimeGrainList(meetingSchedule, startTime, timeGrainListSize);
        createRoomList(meetingSchedule, roomListSize);
        createMeetingAssignmentList(meetingSchedule);

        BigInteger possibleSolutionSize = BigInteger
            .valueOf((long) timeGrainListSize * roomListSize)
            .pow(meetingSchedule.getMeetingAssignmentList().size());
        logger.info(
            "MeetingSchedule {} has {} learners, {} patients, {} timeGrains and {} rooms with a search space of {}.",
            fileName,
            learnersListSize,
            patientsListSize,
            timeGrainListSize,
            roomListSize,
            AbstractSolutionImporter.getFlooredPossibleSolutionSize(possibleSolutionSize));
        return meetingSchedule;
    }

    private void createMeetingListAndAttendanceList(MeetingSchedule meetingSchedule,
        int learnersListSize, int patientsListSize, int durationInGrains) {

        List<Meeting> meetingList = new ArrayList<>(); // TODO instantiate with capacity
        List<Attendance> learnerList = new ArrayList<>(learnersListSize);
        List<Attendance> patientList = new ArrayList<>(patientsListSize);
        List<Attendance> globalAttendanceList = new ArrayList<>();
        List<Person> personList = new ArrayList<>();

        long attendanceId = 0L;
        long personId = 0L;
        long meetingId = 0L;
        for (int learnerIdx = 0; learnerIdx < learnersListSize; learnerIdx++) {
            Attendance learner = new Attendance();
            learner.setId(attendanceId);
            attendanceId++;

            Person person = createPerson(personId++);
            person.setPatient(false);
            learner.setPerson(person);
            personList.add(person);

            // person is filled in later
            globalAttendanceList.add(learner);
            learnerList.add(learner);
        }

        for (int patientIdx = 0; patientIdx < patientsListSize; patientIdx++) {
            Attendance patient = new Attendance();
            patient.setId(attendanceId);
            attendanceId++;

            Person person = createPerson(personId++);
            person.setPatient(true);
            patient.setPerson(person);
            personList.add(person);

            // person is filled in later
            globalAttendanceList.add(patient);
            patientList.add(patient);
        }

        for (Attendance learner : learnerList) {
            for (Attendance patient : patientList) {
                Meeting meeting = new Meeting();
                meeting.setId(meetingId++);
                meeting.setDurationInGrains(durationInGrains);

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

        meetingSchedule.setMeetingList(meetingList);
        meetingSchedule.setAttendanceList(globalAttendanceList);
        meetingSchedule.setPersonList(personList);
    }

    private void createTimeGrainList(MeetingSchedule meetingSchedule, Instant startTime,
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

    private int[] getStartingMinuteOfDayOptions(Instant startTime, int timeGrainListSize) {
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
        logger.trace("Created person with fullName ({}).",
            fullName);
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
