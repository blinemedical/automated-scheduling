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

        generator.writeMeetingSchedule(50, 5, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(100, 5, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(200, 5, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(400, 5, startTime, endTime, meetingDurationInGrains);
        generator.writeMeetingSchedule(800, 5, startTime, endTime, meetingDurationInGrains);
    }

    private final StringDataGenerator fullNameGenerator = StringDataGenerator.buildFullNames();

    protected final SolutionFileIO<MeetingSchedule> solutionFileIO;
    protected final File outputDir;

    protected Random random;

    public MeetingSchedulingGenerator() {
        solutionFileIO = new MeetingSchedulingXlsxFileIO();
        outputDir = new File(CommonApp.determineDataDir(ExaminationApp.DATA_DIR_NAME), "unsolved");
    }

    private void writeMeetingSchedule(int meetingListSize, int roomListSize, Instant startTime,
        Instant endTime, int durationInGrains) {
        Duration meetingDuration = Duration.between(startTime, endTime);
        int timeGrainListSize = (int) meetingDuration.dividedBy(GRAIN_LENGTH_IN_MINUTES)
            .toMinutes();

        String fileName = determineFileName(meetingListSize, timeGrainListSize, roomListSize);
        File outputFile = new File(outputDir,
            fileName + "." + solutionFileIO.getOutputFileExtension());
        MeetingSchedule meetingSchedule = createMeetingSchedule(fileName, meetingListSize,
            startTime, timeGrainListSize, roomListSize, durationInGrains);
        solutionFileIO.write(meetingSchedule, outputFile);
        logger.info("Saved: {}", outputFile);
    }

    private String determineFileName(int meetingListSize, int timeGrainListSize, int roomListSize) {
        return meetingListSize + "meetings-" + timeGrainListSize + "timegrains-" + roomListSize
            + "rooms";
    }

    public MeetingSchedule createMeetingSchedule(String fileName, int meetingListSize,
        Instant startTime, int timeGrainListSize,
        int roomListSize, int durationInGrains) {
        random = new Random(37);
        MeetingSchedule meetingSchedule = new MeetingSchedule();
        meetingSchedule.setId(0L);
        MeetingConstraintConfiguration constraintConfiguration = new MeetingConstraintConfiguration();
        constraintConfiguration.setId(0L);
        meetingSchedule.setConstraintConfiguration(constraintConfiguration);

        createMeetingListAndAttendanceList(meetingSchedule, meetingListSize, durationInGrains);
        createTimeGrainList(meetingSchedule, startTime, timeGrainListSize);
        createRoomList(meetingSchedule, roomListSize);
        createPersonList(meetingSchedule);
        linkAttendanceListToPersons(meetingSchedule);
        createMeetingAssignmentList(meetingSchedule);

        BigInteger possibleSolutionSize = BigInteger
            .valueOf((long) timeGrainListSize * roomListSize)
            .pow(meetingSchedule.getMeetingAssignmentList().size());
        logger.info(
            "MeetingSchedule {} has {} meetings, {} timeGrains and {} rooms with a search space of {}.",
            fileName,
            meetingListSize,
            timeGrainListSize,
            roomListSize,
            AbstractSolutionImporter.getFlooredPossibleSolutionSize(possibleSolutionSize));
        return meetingSchedule;
    }

    private void createMeetingListAndAttendanceList(MeetingSchedule meetingSchedule,
        int meetingListSize, int durationInGrains) {
        List<Meeting> meetingList = new ArrayList<>(meetingListSize);
        List<Attendance> globalAttendanceList = new ArrayList<>();
        long attendanceId = 0L;
        for (int i = 0; i < meetingListSize; i++) {
            Meeting meeting = new Meeting();
            meeting.setId((long) i);
            meeting.setDurationInGrains(durationInGrains);

            Attendance learner = new Attendance();
            learner.setId(attendanceId);
            attendanceId++;
            learner.setMeeting(meeting);
            // person is filled in later
            globalAttendanceList.add(learner);
            meeting.setRequiredLearner(learner);

            Attendance patient = new Attendance();
            patient.setId(attendanceId);
            attendanceId++;
            patient.setMeeting(meeting);
            // person is filled in later
            globalAttendanceList.add(patient);
            meeting.setRequiredPatient(patient);

            logger.trace("Created meeting with durationInGrains ({}),"
                    + " requiredAttendanceListSize ({}).",
                durationInGrains,
                2);
            meetingList.add(meeting);
        }
        meetingSchedule.setMeetingList(meetingList);
        meetingSchedule.setAttendanceList(globalAttendanceList);
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
            String name = "R " + ((i / roomsPerFloor * 100) + (i % roomsPerFloor) + 1);
            room.setName(name);
            int capacity = 2;
            room.setCapacity(capacity);
            logger.trace("Created room with name ({}), capacity ({}).", name, capacity);
            roomList.add(room);
        }
        meetingSchedule.setRoomList(roomList);
    }

    private void createPersonList(MeetingSchedule meetingSchedule) {
        int attendanceListSize = meetingSchedule.getMeetingList().size() * 2;
        int personListSize = attendanceListSize * meetingSchedule.getRoomList().size() * 3
            / (4 * meetingSchedule.getMeetingList().size());
        List<Person> personList = new ArrayList<>(personListSize);
        fullNameGenerator.predictMaximumSizeAndReset(personListSize);
        for (int i = 0; i < personListSize; i++) {
            Person person = new Person();
            person.setId((long) i);
            String fullName = fullNameGenerator.generateNextValue();
            person.setFullName(fullName);
            logger.trace("Created person with fullName ({}).",
                fullName);
            personList.add(person);
        }
        meetingSchedule.setPersonList(personList);
    }

    private void linkAttendanceListToPersons(MeetingSchedule meetingSchedule) {
        for (Meeting meeting : meetingSchedule.getMeetingList()) {
            List<Person> availablePersonList = new ArrayList<>(meetingSchedule.getPersonList());
            int attendanceListSize = 2;
            if (availablePersonList.size() < attendanceListSize) {
                throw new IllegalStateException(
                    "The availablePersonList size (" + availablePersonList.size()
                        + ") is less than the attendanceListSize (" + attendanceListSize + ").");
            }

            meeting.getRequiredLearner().setPerson(
                availablePersonList.remove(random.nextInt(availablePersonList.size())));
            meeting.getRequiredPatient().setPerson(
                availablePersonList.remove(random.nextInt(availablePersonList.size())));

        }
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
