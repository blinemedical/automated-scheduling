package org.blinemedical.examination.persistence;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.ASSIGNED_MEETINGS;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.DONT_GO_IN_OVERTIME;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.HALF_ASSIGNED_MEETINGS;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.ONE_TIME_GRAIN_BREAK_BETWEEN_TWO_CONSECUTIVE_MEETINGS;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.OVERLAPPING_MEETINGS;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.REQUIRED_ATTENDANCE_CONFLICT;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.ROOM_CONFLICT;
import static org.blinemedical.examination.domain.MeetingConstraintConfiguration.ROOM_STABILITY;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Year;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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
import org.optaplanner.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import org.optaplanner.core.api.score.constraint.ConstraintMatch;
import org.optaplanner.core.api.score.constraint.Indictment;
import org.optaplanner.examples.common.persistence.AbstractXlsxSolutionFileIO;
import org.optaplanner.examples.meetingscheduling.app.MeetingSchedulingApp;

public class MeetingSchedulingXlsxFileIO extends AbstractXlsxSolutionFileIO<MeetingSchedule> {

    private static final String COMMA_DELIMITER = ", ";

    @Override
    public MeetingSchedule read(File inputScheduleFile) {
        try (InputStream in = new BufferedInputStream(new FileInputStream(inputScheduleFile))) {
            XSSFWorkbook workbook = new XSSFWorkbook(in);
            return new MeetingSchedulingXlsxReader(workbook).read();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Failed reading inputScheduleFile ("
                + inputScheduleFile + ").", e);
        }
    }

    private static class MeetingSchedulingXlsxReader extends
        AbstractXlsxReader<MeetingSchedule, HardMediumSoftScore> {

        MeetingSchedulingXlsxReader(XSSFWorkbook workbook) {
            super(workbook, MeetingSchedulingApp.SOLVER_CONFIG);
        }

        public MeetingSchedule read() {
            solution = new MeetingSchedule();
            readConfiguration();
            readDayList();
            readRoomList();
            readPersonList();
            readMeetingList();
            readScenarios();

            return solution;
        }

        private void readConfiguration() {
            nextSheet("Configuration");
            nextRow();
            nextRow(true);
            readHeaderCell("Constraint");
            readHeaderCell("Weight");
            readHeaderCell("Description");

            MeetingConstraintConfiguration constraintConfiguration = new MeetingConstraintConfiguration();
            constraintConfiguration.setId(0L);

            // TODO refactor this to allow setting pos/neg, weight and score level
            // Hard
            readIntConstraintParameterLine(ROOM_CONFLICT,
                hardScore -> constraintConfiguration
                    .setRoomConflict(HardMediumSoftScore.ofHard(hardScore)), "");
            readIntConstraintParameterLine(DONT_GO_IN_OVERTIME,
                hardScore -> constraintConfiguration
                    .setDontGoInOvertime(HardMediumSoftScore.ofHard(hardScore)), "");
            readIntConstraintParameterLine(REQUIRED_ATTENDANCE_CONFLICT,
                hardScore -> constraintConfiguration
                    .setRequiredAttendanceConflict(HardMediumSoftScore.ofHard(hardScore)),
                "");
            // Medium
            readIntConstraintParameterLine(ASSIGNED_MEETINGS,
                mediumScore -> constraintConfiguration
                    .setAssignedMeetings(HardMediumSoftScore.ofMedium(mediumScore)), "");
            readIntConstraintParameterLine(HALF_ASSIGNED_MEETINGS,
                mediumScore -> constraintConfiguration
                    .setHalfAssignedMeetings(HardMediumSoftScore.ofMedium(mediumScore)), "");
            // Soft
            readIntConstraintParameterLine(DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE,
                softScore -> constraintConfiguration
                    .setDoAllMeetingsAsSoonAsPossible(HardMediumSoftScore.ofSoft(softScore)), "");
            readIntConstraintParameterLine(ONE_TIME_GRAIN_BREAK_BETWEEN_TWO_CONSECUTIVE_MEETINGS,
                softScore -> constraintConfiguration
                    .setOneTimeGrainBreakBetweenTwoConsecutiveMeetings(
                        HardMediumSoftScore.ofSoft(softScore)),
                "");
            readIntConstraintParameterLine(OVERLAPPING_MEETINGS,
                softScore -> constraintConfiguration
                    .setOverlappingMeetings(HardMediumSoftScore.ofSoft(softScore)), "");
            readIntConstraintParameterLine(ROOM_STABILITY,
                softScore -> constraintConfiguration
                    .setRoomStability(HardMediumSoftScore.ofSoft(softScore)), "");

            solution.setConstraintConfiguration(constraintConfiguration);
        }

        private void readPersonList() {
            nextSheet("Persons");
            nextRow(false);
            readHeaderCell("Full name");
            readHeaderCell("Patient");
            readHeaderCell("Id");
            List<Person> personList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            while (nextRow()) {
                Person person = new Person();
                person.setFullName(nextStringCell().getStringCellValue());
                if (!VALID_NAME_PATTERN.matcher(person.getFullName()).matches()) {
                    throw new IllegalStateException(
                        currentPosition() + ": The person name (" + person.getFullName()
                            + ") must match to the regular expression (" + VALID_NAME_PATTERN
                            + ").");
                }
                person.setPatient(nextStringCell().getStringCellValue().equalsIgnoreCase("y"));
                person.setId((long) nextCell().getNumericCellValue());
                personList.add(person);
            }
            solution.setPersonList(personList);
        }

        private void readMeetingList() {
            Map<String, Person> personMap = solution.getPersonList().stream().collect(
                toMap(Person::getFullName, person -> person));
            nextSheet("Meetings");
            nextRow(false);
            readHeaderCell("Duration");
            readHeaderCell("Required Learner");
            readHeaderCell("Required Patient");
            readHeaderCell("Scenario Id");
            readHeaderCell("Day");
            readHeaderCell("Starting time");
            readHeaderCell("Room");

            List<Meeting> meetingList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            List<MeetingAssignment> meetingAssignmentList = new ArrayList<>(
                currentSheet.getLastRowNum() - 1);
            List<Attendance> attendanceList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            long meetingId = 0L, meetingAssignmentId = 0L, attendanceId = 0L;
            Map<LocalDateTime, TimeGrain> timeGrainMap = solution.getTimeGrainList().stream()
                .collect(
                    Collectors.toMap(TimeGrain::getDateTime, Function.identity()));
            Map<String, Room> roomMap = solution.getRoomList().stream().collect(
                Collectors.toMap(Room::getName, Function.identity()));

            while (nextRow()) {
                Meeting meeting = new Meeting();
                MeetingAssignment meetingAssignment = new MeetingAssignment();
                meeting.setId(meetingId++);
                meetingAssignment.setId(meetingAssignmentId++);

                readMeetingDuration(meeting);

                List<Attendance> meetingAttendanceList = getAttendanceLists(meeting, personMap,
                    attendanceId);
                meeting.setScenarioId(Long.parseLong(nextStringCell().getStringCellValue()));
                attendanceId += meetingAttendanceList.size();
                attendanceList.addAll(meetingAttendanceList);

                meetingAssignment.setStartingTimeGrain(extractTimeGrain(meeting, timeGrainMap));
                meetingAssignment.setRoom(extractRoom(meeting, roomMap));
                meetingList.add(meeting);
                meetingAssignment.setMeeting(meeting);
                meetingAssignmentList.add(meetingAssignment);
            }
            solution.setMeetingList(meetingList);
            solution.setMeetingAssignmentList(meetingAssignmentList);
            solution.setAttendanceList(attendanceList);
        }

        private void readScenarios() {
            nextSheet("Scenarios");
            nextRow(false);
            readHeaderCell("Name");
            readHeaderCell("Patients");
            readHeaderCell("Id");

            Map<Long, List<Attendance>> personIdToAttendanceMap = new HashMap<>();
            for (Attendance attendance : solution.getAttendanceList()) {
                List<Attendance> attendances = personIdToAttendanceMap
                    .computeIfAbsent(attendance.getPerson().getId(), id -> new ArrayList<>());
                attendances.add(attendance);
            }

            List<Scenario> scenarioList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            long scenarioId = 0L;
            while (nextRow()) {
                Scenario scenario = new Scenario();
                scenario.setId(scenarioId++);
                scenario.setName(nextStringCell().getStringCellValue());

                scenario.setPatients(
                    Arrays.stream(nextStringCell().getStringCellValue().split(COMMA_DELIMITER))
                        .filter(personId -> !personId.isEmpty())
                        .flatMap(personIdString -> {
                            Long personId = Long.parseLong(personIdString);
                            List<Attendance> attendances = personIdToAttendanceMap.get(personId);
                            return attendances.stream();
                        })
                        .collect(toList()));
                scenario.setId((long) nextCell().getNumericCellValue());
                scenarioList.add(scenario);
            }
            solution.setScenarioList(scenarioList);
        }

        private void readMeetingDuration(Meeting meeting) {
            double durationDouble = nextNumericCell().getNumericCellValue();
            if (durationDouble <= 0 || durationDouble != Math.floor(durationDouble)) {
                throw new IllegalStateException(
                    currentPosition() + ": The meeting with id (" + meeting.getId()
                        + ")'s has a duration (" + durationDouble
                        + ") that isn't a strictly positive integer number.");
            }
            if (durationDouble % TimeGrain.GRAIN_LENGTH_IN_MINUTES != 0) {
                throw new IllegalStateException(
                    currentPosition() + ": The meeting with id (" + meeting.getId()
                        + ") has a duration (" + durationDouble + ") that isn't a multiple of "
                        + TimeGrain.GRAIN_LENGTH_IN_MINUTES + ".");
            }
            meeting.setDurationInGrains((int) durationDouble / TimeGrain.GRAIN_LENGTH_IN_MINUTES);
        }

        private List<Attendance> getAttendanceLists(Meeting meeting, Map<String, Person> personMap,
            long attendanceId) {
            List<Attendance> attendanceList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            Set<Person> requiredPersonSet = new HashSet<>();

            Attendance requiredLearner = getRequiredAttendee(meeting, personMap, requiredPersonSet);
            requiredLearner.setId(attendanceId++);
            Attendance requiredPatient = getRequiredAttendee(meeting, personMap, requiredPersonSet);
            requiredPatient.setId(attendanceId++);

            meeting.setRequiredLearner(requiredLearner);
            meeting.setRequiredPatient(requiredPatient);
            attendanceList.add(requiredLearner);
            attendanceList.add(requiredPatient);

            return attendanceList;
        }

        private Attendance getRequiredAttendee(Meeting meeting, Map<String, Person> personMap,
            Set<Person> requiredPersonSet) {
            String personName = nextStringCell().getStringCellValue();
            Attendance requiredAttendance = new Attendance();
            Person person = personMap.get(personName);
            if (person == null) {
                throw new IllegalStateException(
                    currentPosition() + ": The meeting with id (" + meeting.getId()
                        + ") has a required attendee (" + personName
                        + ") that doesn't exist in the Persons list.");
            }
            if (requiredPersonSet.contains(person)) {
                throw new IllegalStateException(
                    currentPosition() + ": The meeting with id (" + meeting.getId()
                        + ") has a duplicate required attendee (" + personName + ").");
            }
            requiredPersonSet.add(person);
            requiredAttendance.setMeeting(meeting);
            requiredAttendance.setPerson(person);
            return requiredAttendance;
        }

        private TimeGrain extractTimeGrain(Meeting meeting,
            Map<LocalDateTime, TimeGrain> timeGrainMap) {
            String dateString = nextStringCell().getStringCellValue();
            String startTimeString = nextStringCell().getStringCellValue();
            if (!dateString.isEmpty() || !startTimeString.isEmpty()) {
                LocalDateTime dateTime;
                try {
                    dateTime = LocalDateTime.of(LocalDate.parse(dateString, DAY_FORMATTER),
                        LocalTime.parse(startTimeString, TIME_FORMATTER));
                } catch (DateTimeParseException e) {
                    throw new IllegalStateException(
                        currentPosition() + ": The meeting with id (" + meeting.getId()
                            + ") has a timeGrain date (" + dateString + ") and startTime ("
                            + startTimeString
                            + ") that doesn't parse as a date or time.", e);
                }

                TimeGrain timeGrain = timeGrainMap.get(dateTime);
                if (timeGrain == null) {
                    throw new IllegalStateException(
                        currentPosition() + ": The meeting with id (" + meeting.getId()
                            + ") has a timeGrain date (" + dateString + ") and startTime ("
                            + startTimeString
                            + ") that doesn't exist in the other sheet (Day).");
                }
                return timeGrain;
            }
            return null;
        }

        private Room extractRoom(Meeting meeting, Map<String, Room> roomMap) {
            String roomName = nextStringCell().getStringCellValue();
            if (!roomName.isEmpty()) {
                Room room = roomMap.get(roomName);
                if (room == null) {
                    throw new IllegalStateException(
                        currentPosition() + ": The meeting with id (" + meeting.getId()
                            + ") has a roomName (" + roomName
                            + ") that doesn't exist in the other sheet (Rooms).");
                }
                return room;
            }
            return null;
        }

        private void readDayList() {
            nextSheet("Days");
            nextRow(false);
            readHeaderCell("Day");
            readHeaderCell("Start");
            readHeaderCell("End");
            List<Day> dayList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            List<TimeGrain> timeGrainList = new ArrayList<>();
            long dayId = 0L, timeGrainId = 0L;
            while (nextRow()) {
                Day day = new Day();
                day.setId(dayId++);
                day.setDayOfYear(
                    LocalDate.parse(nextStringCell().getStringCellValue(), DAY_FORMATTER)
                        .getDayOfYear());
                dayList.add(day);

                LocalTime startTime = LocalTime
                    .parse(nextStringCell().getStringCellValue(), TIME_FORMATTER);
                LocalTime endTime = LocalTime
                    .parse(nextStringCell().getStringCellValue(), TIME_FORMATTER);
                int startMinuteOfDay = startTime.getHour() * 60 + startTime.getMinute();
                int endMinuteOfDay = endTime.getHour() * 60 + endTime.getMinute();
                for (int i = 0;
                    (endMinuteOfDay - startMinuteOfDay) > i * TimeGrain.GRAIN_LENGTH_IN_MINUTES;
                    i++) {
                    int timeGrainStartingMinuteOfDay =
                        i * TimeGrain.GRAIN_LENGTH_IN_MINUTES + startMinuteOfDay;

                    TimeGrain timeGrain = new TimeGrain();
                    timeGrain.setId(timeGrainId);
                    timeGrain.setGrainIndex((int) timeGrainId++);
                    timeGrain.setDay(day);
                    timeGrain.setStartingMinuteOfDay(timeGrainStartingMinuteOfDay);
                    timeGrainList.add(timeGrain);
                }
            }
            solution.setDayList(dayList);
            solution.setTimeGrainList(timeGrainList);
        }

        private void readRoomList() {
            nextSheet("Rooms");
            nextRow();
            readHeaderCell("Name");
            List<Room> roomList = new ArrayList<>(currentSheet.getLastRowNum() - 1);
            long id = 0L;
            while (nextRow()) {
                Room room = new Room();
                room.setId(id++);
                room.setName(nextStringCell().getStringCellValue());
                if (!VALID_NAME_PATTERN.matcher(room.getName()).matches()) {
                    throw new IllegalStateException(
                        currentPosition() + ": The room name (" + room.getName()
                            + ") must match to the regular expression (" + VALID_NAME_PATTERN
                            + ").");
                }
                roomList.add(room);
            }
            solution.setRoomList(roomList);
        }
    }

    @Override
    public void write(MeetingSchedule solution, File outputScheduleFile) {
        try (FileOutputStream out = new FileOutputStream(outputScheduleFile)) {
            Workbook workbook = new MeetingSchedulingXlsxWriter(solution).write();
            workbook.write(out);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException(
                "Failed writing outputScheduleFile (" + outputScheduleFile
                    + ") for schedule (" + solution + ").", e);
        }
    }

    private static class MeetingSchedulingXlsxWriter extends
        AbstractXlsxWriter<MeetingSchedule, HardMediumSoftScore> {

        MeetingSchedulingXlsxWriter(
            MeetingSchedule solution) {
            super(solution, ExaminationApp.SOLVER_CONFIG);
        }

        @Override
        public Workbook write() {
            workbook = new XSSFWorkbook();
            creationHelper = workbook.getCreationHelper();
            createStyles();
            writeConfiguration();
            writeDays();
            writeRooms();
            writePersons();
            writeScenarios();
            writeMeetings();
            writeRoomsView();
            writePersonsView();
            writePrintedFormView();
            writeScoreView(justificationList -> justificationList.stream()
                .filter(o -> o instanceof MeetingAssignment)
                .map(Object::toString)
                .collect(joining(COMMA_DELIMITER)));
            return workbook;
        }

        private void writeConfiguration() {
            nextSheet("Configuration", 1, 3, false);
            nextRow();
            nextCell()
                .setCellValue(DAY_FORMATTER.format(LocalDateTime.now()) + " " + TIME_FORMATTER
                    .format(LocalDateTime.now()));
            nextRow();
            nextRow();
            nextHeaderCell("Constraint");
            nextHeaderCell("Weight");
            nextHeaderCell("Description");

            MeetingConstraintConfiguration constraintConfiguration = solution
                .getConstraintConfiguration();

            // TODO refactor this to allow setting pos/neg, weight and score level
            // Hard
            writeIntConstraintParameterLine(ROOM_CONFLICT,
                constraintConfiguration.getRoomConflict().getHardScore(), "");
            writeIntConstraintParameterLine(DONT_GO_IN_OVERTIME,
                constraintConfiguration.getDontGoInOvertime().getHardScore(),
                "");
            writeIntConstraintParameterLine(REQUIRED_ATTENDANCE_CONFLICT,
                constraintConfiguration.getRequiredAttendanceConflict().getHardScore(), "");
            nextRow();

            // Medium
            writeIntConstraintParameterLine(ASSIGNED_MEETINGS,
                constraintConfiguration.getHalfAssignedMeetings().getMediumScore(), "");
            writeIntConstraintParameterLine(HALF_ASSIGNED_MEETINGS,
                constraintConfiguration.getAssignedMeetings().getMediumScore(), "");
            nextRow();

            // Soft
            writeIntConstraintParameterLine(DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE,
                constraintConfiguration.getDoAllMeetingsAsSoonAsPossible().getSoftScore(), "");
            writeIntConstraintParameterLine(ONE_TIME_GRAIN_BREAK_BETWEEN_TWO_CONSECUTIVE_MEETINGS,
                constraintConfiguration.getOneTimeGrainBreakBetweenTwoConsecutiveMeetings()
                    .getSoftScore(), "");
            writeIntConstraintParameterLine(OVERLAPPING_MEETINGS,
                constraintConfiguration.getOverlappingMeetings().getSoftScore(), "");
            writeIntConstraintParameterLine(ROOM_STABILITY,
                constraintConfiguration.getRoomStability().getSoftScore(), "");

            autoSizeColumnsWithHeader();
        }

        private void writeScenarios() {
            nextSheet("Scenarios", 1, 0, false);
            nextRow();
            nextHeaderCell("Name");
            nextHeaderCell("Patients");
            nextHeaderCell("Id");

            for (Scenario scenario : solution.getScenarioList()) {
                nextRow();
                nextCell().setCellValue(scenario.getName());
                nextCell().setCellValue(scenario.getPatients().stream()
                    .map(Attendance::getPerson)
                    .map(Person::getId)
                    .map(Object::toString)
                    .collect(joining(COMMA_DELIMITER)));
                nextCell().setCellValue(scenario.getId());
            }
            autoSizeColumnsWithHeader();
        }

        private void writePersons() {
            nextSheet("Persons", 1, 0, false);
            nextRow();
            nextHeaderCell("Full name");
            nextHeaderCell("Patient");
            nextHeaderCell("Id");
            for (Person person : solution.getPersonList()) {
                nextRow();
                nextCell().setCellValue(person.getFullName());
                nextCell().setCellValue(person.isPatient() ? "Y" : "");
                nextCell().setCellValue(person.getId());
            }
            autoSizeColumnsWithHeader();
        }

        private void writeMeetings() {
            nextSheet("Meetings", 1, 1, false);
            nextRow();
            nextHeaderCell("Duration");
            nextHeaderCell("Required Learner");
            nextHeaderCell("Required Patient");
            nextHeaderCell("Scenario Id");
            nextHeaderCell("Day");
            nextHeaderCell("Starting time");
            nextHeaderCell("Room");
            Map<Meeting, List<MeetingAssignment>> meetingAssignmentMap = solution
                .getMeetingAssignmentList().stream()
                .collect(groupingBy(MeetingAssignment::getMeeting, toList()));
            for (Meeting meeting : solution.getMeetingList()) {
                nextRow();
                nextCell().setCellValue(
                    meeting.getDurationInGrains() * TimeGrain.GRAIN_LENGTH_IN_MINUTES);
                nextCell().setCellValue(meeting.getRequiredLearner().getPerson().getFullName());
                nextCell().setCellValue(meeting.getRequiredPatient().getPerson().getFullName());
                nextCell().setCellValue(meeting.getScenarioId().toString());
                List<MeetingAssignment> meetingAssignmentList = meetingAssignmentMap.get(meeting);
                if (meetingAssignmentList.size() != 1) {
                    throw new IllegalStateException("Impossible state: the meeting (" + meeting
                        + ") does not have exactly one assignment, but " + meetingAssignmentList
                        .size()
                        + " assignments instead.");
                }
                MeetingAssignment meetingAssignment = meetingAssignmentList.get(0);
                TimeGrain startingTimeGrain = meetingAssignment.getStartingTimeGrain();
                nextCell().setCellValue(startingTimeGrain == null ? ""
                    : DAY_FORMATTER.format(startingTimeGrain.getDate()));
                nextCell().setCellValue(startingTimeGrain == null ? ""
                    : TIME_FORMATTER.format(startingTimeGrain.getTime()));
                nextCell().setCellValue(meetingAssignment.getRoom() == null ? ""
                    : meetingAssignment.getRoom().getName());
            }
            setSizeColumnsWithHeader(5000);
        }

        private void writeDays() {
            nextSheet("Days", 1, 1, false);
            nextRow();
            nextHeaderCell("Day");
            nextHeaderCell("Start");
            nextHeaderCell("End");
            for (Day dayOfYear : solution.getDayList()) {
                nextRow();
                LocalDate date = LocalDate
                    .ofYearDay(Year.now().getValue(), dayOfYear.getDayOfYear());
                int startMinuteOfDay = 24 * 60, endMinuteOfDay = 0;
                for (TimeGrain timeGrain : solution.getTimeGrainList()) {
                    if (timeGrain.getDay().equals(dayOfYear)) {
                        startMinuteOfDay = Math.min(
                            timeGrain.getStartingMinuteOfDay(),
                            startMinuteOfDay);
                        endMinuteOfDay = Math.max(
                            timeGrain.getStartingMinuteOfDay() + TimeGrain.GRAIN_LENGTH_IN_MINUTES,
                            endMinuteOfDay);
                    }
                }
                LocalTime startTime = LocalTime.ofSecondOfDay(startMinuteOfDay * 60);
                LocalTime endTime = LocalTime.ofSecondOfDay(endMinuteOfDay * 60);

                nextCell().setCellValue(DAY_FORMATTER.format(date));
                nextCell().setCellValue(TIME_FORMATTER.format(startTime));
                nextCell().setCellValue(TIME_FORMATTER.format(endTime));
            }
            autoSizeColumnsWithHeader();
        }

        private void writeRooms() {
            nextSheet("Rooms", 1, 1, false);
            nextRow();
            nextHeaderCell("Name");
            for (Room room : solution.getRoomList()) {
                nextRow();
                nextCell().setCellValue(room.getName());
            }
            autoSizeColumnsWithHeader();
        }

        private void writeRoomsView() {
            nextSheet("Rooms view", 1, 2, true);
            nextRow();
            nextHeaderCell("");
            writeTimeGrainDaysHeaders();
            nextRow();
            nextHeaderCell("Room");
            writeTimeGrainHoursHeaders();
            for (Room room : solution.getRoomList()) {
                nextRow();
                currentRow.setHeightInPoints(2 * currentSheet.getDefaultRowHeightInPoints());
                nextCell().setCellValue(room.getName());
                List<MeetingAssignment> roomMeetingAssignmentList = solution
                    .getMeetingAssignmentList().stream()
                    .filter(meetingAssignment -> meetingAssignment.getRoom() == room)
                    .collect(toList());
                writeMeetingAssignmentList(roomMeetingAssignmentList);
            }
            autoSizeColumnsWithHeader();
        }

        private void writePersonsView() {
            nextSheet("Persons view", 1, 2, true);
            nextRow();
            nextHeaderCell("");
            nextHeaderCell("");
            writeTimeGrainDaysHeaders();
            nextRow();
            nextHeaderCell("Person");
            writeTimeGrainHoursHeaders();
            for (Person person : solution.getPersonList()) {
                writePersonMeetingList(person);
            }
            autoSizeColumnsWithHeader();
        }

        private void writePersonMeetingList(Person person) {
            nextRow();
            currentRow.setHeightInPoints(2 * currentSheet.getDefaultRowHeightInPoints());
            nextHeaderCell(person.getFullName());

            List<Meeting> personMeetingList = solution.getAttendanceList().stream()
                .filter(attendance -> attendance.getPerson().equals(person))
                .map(Attendance::getMeeting)
                .collect(toList());

            List<MeetingAssignment> personMeetingAssignmentList = solution
                .getMeetingAssignmentList().stream()
                .filter(
                    meetingAssignment -> personMeetingList.contains(meetingAssignment.getMeeting()))
                .collect(toList());
            writeMeetingAssignmentList(personMeetingAssignmentList);
        }

        private void writePrintedFormView() {
            nextSheet("Printed form view", 1, 1, true);
            nextRow();
            nextHeaderCell("");
            writeTimeGrainsHoursVertically(30);
            currentColumnNumber = 0;
            for (Room room : solution.getRoomList()) {
                List<MeetingAssignment> roomMeetingAssignmentList = solution
                    .getMeetingAssignmentList().stream()
                    .filter(meetingAssignment -> meetingAssignment.getRoom() == room)
                    .collect(toList());
                if (roomMeetingAssignmentList.isEmpty()) {
                    continue;
                }

                currentColumnNumber++;
                currentRowNumber = -1;
                nextHeaderCellVertically(room.getName());
                writeMeetingAssignmentListVertically(roomMeetingAssignmentList);
            }
            setSizeColumnsWithHeader(6000);
        }

        private void writeMeetingAssignmentListVertically(
            List<MeetingAssignment> roomMeetingAssignmentList) {
            int mergeStart = -1;
            int previousMeetingRemainingTimeGrains = 0;
            boolean mergingPreviousTimeGrain = false;
            for (TimeGrain timeGrain : solution.getTimeGrainList()) {
                List<MeetingAssignment> meetingAssignmentList = roomMeetingAssignmentList.stream()
                    .filter(
                        meetingAssignment -> meetingAssignment.getStartingTimeGrain() == timeGrain)
                    .collect(toList());
                if (meetingAssignmentList.isEmpty() && mergingPreviousTimeGrain
                    && previousMeetingRemainingTimeGrains > 0) {
                    previousMeetingRemainingTimeGrains--;
                    nextCellVertically();
                } else {
                    if (mergingPreviousTimeGrain && mergeStart < currentRowNumber) {
                        currentSheet.addMergedRegion(
                            new CellRangeAddress(mergeStart, currentRowNumber, currentColumnNumber,
                                currentColumnNumber));
                    }

                    StringBuilder meetingInfo = new StringBuilder();
                    for (MeetingAssignment meetingAssignment : meetingAssignmentList) {
                        String startTimeString = getTimeString(
                            meetingAssignment.getStartingTimeGrain().getStartingMinuteOfDay());
                        int lastTimeGrainIndex = meetingAssignment.getLastTimeGrainIndex()
                            <= solution.getTimeGrainList().size()
                            - 1 ? meetingAssignment.getLastTimeGrainIndex()
                            : solution.getTimeGrainList().size() - 1;
                        String endTimeString = getTimeString(
                            solution.getTimeGrainList().get(lastTimeGrainIndex)
                                .getStartingMinuteOfDay()
                                + TimeGrain.GRAIN_LENGTH_IN_MINUTES);
                        meetingInfo
                            .append("\n  ")
                            .append(startTimeString).append(" - ").append(endTimeString)
                            .append(" (")
                            .append(meetingAssignment.getMeeting().getDurationInGrains()
                                * TimeGrain.GRAIN_LENGTH_IN_MINUTES)
                            .append(" mins)");
                    }
                    nextCellVertically().setCellValue(meetingInfo.toString());

                    previousMeetingRemainingTimeGrains =
                        getLongestDurationInGrains(meetingAssignmentList) - 1;
                    mergingPreviousTimeGrain = previousMeetingRemainingTimeGrains > 0;
                    mergeStart = currentRowNumber;
                }
            }
            if (mergeStart < currentRowNumber) {
                currentSheet.addMergedRegion(
                    new CellRangeAddress(mergeStart, currentRowNumber, currentColumnNumber,
                        currentColumnNumber));
            }
        }

        private String getTimeString(int minuteOfDay) {
            return TIME_FORMATTER.format(LocalTime.ofSecondOfDay(minuteOfDay * 60));
        }

        private void writeMeetingAssignmentList(List<MeetingAssignment> meetingAssignmentList) {
            String[] filteredConstraintNames = {
                ROOM_CONFLICT,
                DONT_GO_IN_OVERTIME,
                REQUIRED_ATTENDANCE_CONFLICT,

                ASSIGNED_MEETINGS,
                HALF_ASSIGNED_MEETINGS,

                DO_ALL_MEETINGS_AS_SOON_AS_POSSIBLE,
                ONE_TIME_GRAIN_BREAK_BETWEEN_TWO_CONSECUTIVE_MEETINGS,
                OVERLAPPING_MEETINGS,
                ROOM_STABILITY
            };
            int mergeStart = -1;
            int previousMeetingRemainingTimeGrains = 0;
            boolean mergingPreviousMeetingList = false;

            for (TimeGrain timeGrain : solution.getTimeGrainList()) {
                List<MeetingAssignment> timeGrainMeetingAssignmentList = meetingAssignmentList
                    .stream()
                    .filter(
                        meetingAssignment -> meetingAssignment.getStartingTimeGrain() == timeGrain)
                    .collect(toList());
                if (timeGrainMeetingAssignmentList.isEmpty() && mergingPreviousMeetingList
                    && previousMeetingRemainingTimeGrains > 0) {
                    previousMeetingRemainingTimeGrains--;
                    nextCell();
                } else {
                    if (mergingPreviousMeetingList && mergeStart < currentColumnNumber) {
                        currentSheet.addMergedRegion(
                            new CellRangeAddress(currentRowNumber, currentRowNumber, mergeStart,
                                currentColumnNumber));
                    }
                    nextMeetingAssignmentListCell(
                        timeGrainMeetingAssignmentList,
                        Arrays.asList(filteredConstraintNames));
                    mergingPreviousMeetingList = !timeGrainMeetingAssignmentList.isEmpty();
                    mergeStart = currentColumnNumber;
                    previousMeetingRemainingTimeGrains =
                        getLongestDurationInGrains(timeGrainMeetingAssignmentList) - 1;
                }
            }

            if (mergingPreviousMeetingList && mergeStart < currentColumnNumber) {
                currentSheet.addMergedRegion(
                    new CellRangeAddress(currentRowNumber, currentRowNumber, mergeStart,
                        currentColumnNumber));
            }
        }

        private int getLongestDurationInGrains(List<MeetingAssignment> meetingAssignmentList) {
            int longestDurationInGrains = 1;
            for (MeetingAssignment meetingAssignment : meetingAssignmentList) {
                if (meetingAssignment.getMeeting().getDurationInGrains()
                    > longestDurationInGrains) {
                    longestDurationInGrains = meetingAssignment.getMeeting().getDurationInGrains();
                }
            }
            return longestDurationInGrains;
        }

        private void writeTimeGrainDaysHeaders() {
            Day previousTimeGrainDay = null;
            int mergeStart = -1;

            for (TimeGrain timeGrain : solution.getTimeGrainList()) {
                Day timeGrainDay = timeGrain.getDay();
                if (timeGrainDay.equals(previousTimeGrainDay)) {
                    nextHeaderCell("");
                } else {
                    if (previousTimeGrainDay != null) {
                        currentSheet.addMergedRegion(
                            new CellRangeAddress(currentRowNumber, currentRowNumber, mergeStart,
                                currentColumnNumber));
                    }
                    nextHeaderCell(DAY_FORMATTER.format(
                        LocalDate.ofYearDay(Year.now().getValue(), timeGrainDay.getDayOfYear())));
                    previousTimeGrainDay = timeGrainDay;
                    mergeStart = currentColumnNumber;
                }
            }
            if (previousTimeGrainDay != null) {
                currentSheet.addMergedRegion(
                    new CellRangeAddress(currentRowNumber, currentRowNumber, mergeStart,
                        currentColumnNumber));
            }
        }

        private void writeTimeGrainHoursHeaders() {
            for (TimeGrain timeGrain : solution.getTimeGrainList()) {
                LocalTime startTime = LocalTime
                    .ofSecondOfDay(timeGrain.getStartingMinuteOfDay() * 60L);
                nextHeaderCell(TIME_FORMATTER.format(startTime));
            }
        }

        private void writeTimeGrainsHoursVertically(int minimumInterval) {
            int mergeStart = -1;
            for (TimeGrain timeGrain : solution.getTimeGrainList()) {
                if (timeGrain.getGrainIndex() % (Math
                    .ceil(minimumInterval * 1.0 / TimeGrain.GRAIN_LENGTH_IN_MINUTES)) == 0) {
                    nextRow();
                    nextCell().setCellValue(timeGrain.getDateTimeString());
                    mergeStart = currentRowNumber;
                } else {
                    nextRow();
                }
            }
            if (mergeStart < currentRowNumber) {
                currentSheet
                    .addMergedRegion(new CellRangeAddress(mergeStart, currentRowNumber, 0, 0));
            }
        }

        void nextMeetingAssignmentListCell(
            List<MeetingAssignment> meetingAssignmentList,
            List<String> filteredConstraintNames) {
            if (meetingAssignmentList == null) {
                meetingAssignmentList = Collections.emptyList();
            }
            HardMediumSoftScore score = meetingAssignmentList.stream()
                .map(indictmentMap::get).filter(Objects::nonNull)
                .flatMap(indictment -> indictment.getConstraintMatchSet().stream())
                // Filter out filtered constraints
                .filter(constraintMatch -> filteredConstraintNames == null
                    || filteredConstraintNames.contains(constraintMatch.getConstraintName()))
                .map(ConstraintMatch::getScore)
                // Filter out positive constraints
                .filter(indictmentScore -> !(indictmentScore.getHardScore() >= 0
                    && indictmentScore.getSoftScore() >= 0))
                .reduce(HardMediumSoftScore::add).orElse(HardMediumSoftScore.ZERO);

            XSSFCell cell = getXSSFCellOfScore(score);

            if (!meetingAssignmentList.isEmpty()) {
                ClientAnchor anchor = creationHelper.createClientAnchor();
                anchor.setCol1(cell.getColumnIndex());
                anchor.setCol2(cell.getColumnIndex() + 4);
                anchor.setRow1(currentRow.getRowNum());
                anchor.setRow2(currentRow.getRowNum() + 4);
                Comment comment = currentDrawing.createCellComment(anchor);
                String commentString = getMeetingAssignmentListString(meetingAssignmentList);
                comment.setString(creationHelper.createRichTextString(commentString));
                cell.setCellComment(comment);
            }
            currentRow.setHeightInPoints(Math.max(currentRow.getHeightInPoints(),
                meetingAssignmentList.size() * currentSheet.getDefaultRowHeightInPoints()));
        }

        private String getMeetingAssignmentListString(
            List<MeetingAssignment> meetingAssignmentList) {
            StringBuilder commentString = new StringBuilder(meetingAssignmentList.size() * 200);
            for (MeetingAssignment meetingAssignment : meetingAssignmentList) {
                commentString.append("Date and Time: ")
                    .append(meetingAssignment.getStartingTimeGrain().getDateTimeString())
                    .append("\n")
                    .append("Duration: ")
                    .append(meetingAssignment.getMeeting().getDurationInGrains()
                        * TimeGrain.GRAIN_LENGTH_IN_MINUTES)
                    .append(" minutes.\n")
                    .append("Room: ").append(meetingAssignment.getRoom().getName()).append("\n")
                    .append("Scenario Id: ").append(meetingAssignment.getMeeting().getScenarioId()).append("\n");

                Indictment<HardMediumSoftScore> indictment = indictmentMap.get(meetingAssignment);
                if (indictment != null) {
                    commentString.append("\n").append(indictment.getScore().toShortString())
                        .append(" total");
                    Set<ConstraintMatch<HardMediumSoftScore>> constraintMatchSet = indictment
                        .getConstraintMatchSet();
                    List<String> constraintNameList = constraintMatchSet.stream()
                        .map(ConstraintMatch::getConstraintName).distinct().collect(toList());
                    for (String constraintName : constraintNameList) {
                        List<ConstraintMatch<HardMediumSoftScore>> filteredConstraintMatchList = constraintMatchSet
                            .stream()
                            .filter(constraintMatch -> constraintMatch.getConstraintName()
                                .equals(constraintName))
                            .collect(toList());
                        HardMediumSoftScore sum = filteredConstraintMatchList.stream()
                            .map(ConstraintMatch::getScore)
                            .reduce(HardMediumSoftScore::add)
                            .orElse(HardMediumSoftScore.ZERO);
                        String justificationTalkCodes = filteredConstraintMatchList.stream()
                            .flatMap(
                                constraintMatch -> constraintMatch.getJustificationList().stream())
                            .filter(justification -> justification instanceof MeetingAssignment
                                && justification != meetingAssignment)
                            .distinct()
                            .map(o -> Long.toString(((MeetingAssignment) o).getMeeting().getId()))
                            .collect(joining(COMMA_DELIMITER));
                        commentString.append("\n    ").append(sum.toShortString())
                            .append(" for ").append(filteredConstraintMatchList.size())
                            .append(" ").append(constraintName).append("s")
                            .append("\n        ").append(justificationTalkCodes);
                    }
                }
                commentString.append("\n\n");
            }
            return commentString.toString();
        }

        private XSSFCell getXSSFCellOfScore(HardMediumSoftScore score) {
            XSSFCell cell;
            if (!score.isFeasible()) {
                cell = nextCell(hardPenaltyStyle);
            } else if (score.getMediumScore() < 0) {
                cell = nextCell(mediumPenaltyStyle);
            } else if (score.getSoftScore() < 0) {
                cell = nextCell(softPenaltyStyle);
            } else {
                cell = nextCell(wrappedStyle);
            }
            return cell;
        }
    }
}