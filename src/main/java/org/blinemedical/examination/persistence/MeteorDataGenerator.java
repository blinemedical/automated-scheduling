package org.blinemedical.examination.persistence;

import static org.blinemedical.examination.domain.TimeGrain.GRAIN_LENGTH_IN_MINUTES;

import java.io.File;
import java.io.IOException;
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
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MeteorDataGenerator {

    private static final Logger logger = LogManager.getLogger(MeteorDataGenerator.class);
    static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static JsonFactory JSON_FACTORY = new JacksonFactory();

    public static void main(String[] args) throws IOException {
        MeteorDataGenerator generator = new MeteorDataGenerator();
        //HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();

        Instant startTime = Instant.parse("2020-12-18T08:00:00.00Z");
        Instant endTime = Instant.parse("2020-12-18T16:00:00.00Z");
        Duration meetingDuration = Duration.ofHours(1L);
        int meetingDurationInGrains = (int) (meetingDuration.toMinutes() / GRAIN_LENGTH_IN_MINUTES);

        HttpRequestFactory requestFactory
          = HTTP_TRANSPORT.createRequestFactory(
            (HttpRequest request) -> {
              request.setParser(new JsonObjectParser(JSON_FACTORY));
          });
        String token = "ba90383942681f06cd2f9da85c67cd1a73072fe1dd274c8fb3da27c863b77d5be918b1a8d9033d5543ea20e9e8e548d0da87584bda9f4c01419ca3f9ab7349e82fd6632c0abc548fca7d5f063806fe43ea9d07156168e8bc9c4e76bb847a50f883d438b762065b9a33d1bae3b9779a2b:%242b%2410%24Lo9e7x7wSTb94l6smLY5N.UF.WlJVRh4QuBl78gfifbc2mjc3WIdC";
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        // all users
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl("http://localhost:8080/api/sample-data")).setHeaders(headers);
        HttpResponse response = request.execute();

        JsonElement el = JsonParser.parseString(response.parseAsString());
        JsonArray rooms = el.getAsJsonObject().get("rooms").getAsJsonArray();
        System.out.print("size ".concat(String.valueOf(rooms.size())));

        JsonArray learners = el.getAsJsonObject().get("learners").getAsJsonArray();
        JsonArray patients = el.getAsJsonObject().get("patients").getAsJsonArray();
        JsonArray scenarios = el.getAsJsonObject().get("scenarios").getAsJsonArray();

        generator.writeMeetingSchedule(learners, patients, rooms, scenarios, startTime, endTime, meetingDurationInGrains);


        // course only
        HttpRequest request2 = requestFactory.buildGetRequest(new GenericUrl("http://localhost:8080/api/sample-data?courseId=7d401fab-428e-4795-8480-76702f96f128")).setHeaders(headers);
        HttpResponse response2 = request2.execute();

        JsonElement el2 = JsonParser.parseString(response2.parseAsString());
        JsonArray courseRooms = el2.getAsJsonObject().get("rooms").getAsJsonArray();
        JsonArray courseLearners = el2.getAsJsonObject().get("learners").getAsJsonArray();
        JsonArray coursePatients = el2.getAsJsonObject().get("patients").getAsJsonArray();
        JsonArray courseScenarios = el2.getAsJsonObject().get("scenarios").getAsJsonArray();
        generator.writeMeetingSchedule(courseLearners, coursePatients, courseRooms, courseScenarios, startTime, endTime, meetingDurationInGrains);
    }

    protected final SolutionFileIO<MeetingSchedule> solutionFileIO;
    protected final File outputDir;

    protected Random random;

    public MeteorDataGenerator() {
        solutionFileIO = new MeetingSchedulingXlsxFileIO();
        outputDir = new File(CommonApp.determineDataDir(ExaminationApp.DATA_DIR_NAME), "unsolved");
    }

    private void writeMeetingSchedule(JsonArray learners, JsonArray patients, JsonArray rooms, JsonArray scenarios, Instant startTime,
        Instant endTime, int durationInGrains) {
        int roomListSize = rooms.size();
        int learnersListSize = learners.size();
        int patientsListSize = patients.size();
        int scenarioListSize = scenarios.size();
        Duration meetingDuration = Duration.between(startTime, endTime);
        int timeGrainListSize = (int) meetingDuration.dividedBy(GRAIN_LENGTH_IN_MINUTES)
            .toMinutes();

        String fileName = determineFileName(
            learnersListSize,
            patientsListSize,
            roomListSize,
            scenarioListSize);
        File outputFile = new File(outputDir,
            fileName + "." + solutionFileIO.getOutputFileExtension());
        MeetingSchedule meetingSchedule = createMeetingSchedule(
            fileName,
            learners,
            patients,
            startTime,
            timeGrainListSize,
            rooms,
            scenarios,
            durationInGrains);
        solutionFileIO.write(meetingSchedule, outputFile);
        logger.info("Saved: {}", outputFile);
    }

    private String determineFileName(int learnersListSize, int patientsListSize, int roomListSize, int scenarioListSize) {
        return learnersListSize + "L-"
            + scenarioListSize + "SC-"
            + patientsListSize + "SP-"
            + roomListSize + "R";
    }

    public MeetingSchedule createMeetingSchedule(
        String fileName,
        JsonArray learners,
        JsonArray patients,
        Instant startTime,
        int timeGrainListSize,
        JsonArray rooms,
        JsonArray scenarios,
        int durationInGrains) {

        int roomListSize = rooms.size();
        int learnersListSize = learners.size();
        int patientsListSize = patients.size();
        int numScenarios = scenarios.size();

        random = new Random(37);
        MeetingSchedule meetingSchedule = new MeetingSchedule();
        meetingSchedule.setId(0L);
        MeetingConstraintConfiguration constraintConfiguration = new MeetingConstraintConfiguration();
        constraintConfiguration.setId(0L);
        meetingSchedule.setConstraintConfiguration(constraintConfiguration);

        meetingSchedule.setAttendanceList(new ArrayList<>());
        meetingSchedule.setPersonList(new ArrayList<>());

        List<Attendance> learnerList = createLearners(meetingSchedule, learners);
        createScenariosAndPatients(meetingSchedule, learnersListSize, scenarios);
        createMeetingListAndAttendanceList(meetingSchedule, learnerList,
            durationInGrains);
        createTimeGrainList(meetingSchedule, startTime, timeGrainListSize);
        createRoomList(meetingSchedule, rooms);
        createMeetingAssignmentList(meetingSchedule);

        BigInteger possibleSolutionSize = BigInteger
            .valueOf((long) timeGrainListSize * roomListSize)
            .pow(meetingSchedule.getMeetingAssignmentList().size());
        logger.info(
            "MeetingSchedule {} has {} learners, {} scenarios, {} total patients, {} timeGrains and {} rooms with a search space of {}.",
            fileName,
            learnersListSize,
            numScenarios,
            patientsListSize,
            timeGrainListSize,
            roomListSize,
            AbstractSolutionImporter.getFlooredPossibleSolutionSize(possibleSolutionSize));
        return meetingSchedule;
    }

    private List<Attendance> createLearners(MeetingSchedule meetingSchedule, JsonArray learners) {
        int learnersListSize = learners.size();

        List<Attendance> learnerList = new ArrayList<>(learnersListSize);
        List<Person> personList = new ArrayList<>();

        long attendanceId = 0L;
        long personId = 0L;

        for (int learnerIdx = 0; learnerIdx < learnersListSize; learnerIdx++) {
            Attendance learner = new Attendance();
            learner.setId(attendanceId);
            attendanceId++;

            Person person = createPerson(personId++, learners.get(learnerIdx).getAsJsonObject());
            person.setPatient(false);
            learner.setPerson(person);
            personList.add(person);

            // person is filled in later
            learnerList.add(learner);
        }

        meetingSchedule.getAttendanceList().addAll(learnerList);
        meetingSchedule.getPersonList().addAll(personList);

        return learnerList;
    }

    private void createScenariosAndPatients(
        MeetingSchedule meetingSchedule,
        int learnersListSize,
        JsonArray scenarios) {
        int numScenarios = scenarios.size();

        List<Scenario> scenarioList = new ArrayList<>(numScenarios);
        List<Attendance> patientList = new ArrayList<>();
        List<Person> personList = new ArrayList<>();

        long scenarioId = 0L;
        long attendanceId = learnersListSize;
        long personId = learnersListSize;

        for (int scenarioIdx = 0; scenarioIdx < numScenarios; scenarioIdx++) {
            JsonObject scenarioData = scenarios.get(scenarioIdx).getAsJsonObject();
            Scenario scenario = new Scenario();
            scenario.setId(scenarioId);
            String scenarioName = scenarioData.get("privateTitle").getAsString();
            scenario.setName(scenarioName);
            scenarioId++;

            JsonArray patients = scenarioData.get("patients").getAsJsonArray();
            int patientsPerScenario = patients.size();
            patientList = new ArrayList<>(numScenarios * patientsPerScenario);
            scenario.setPatients(new ArrayList<>());

            for (int patientIdx = 0; patientIdx < patientsPerScenario; patientIdx++) {
                Attendance patient = new Attendance();
                patient.setId(attendanceId++);

                Person person = createPerson(personId++, patients.get(patientIdx).getAsJsonObject());
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

    private void createMeetingListAndAttendanceList(MeetingSchedule meetingSchedule,
        List<Attendance> learnerList, int durationInGrains) {

        List<Meeting> meetingList = new ArrayList<>();
        long meetingId = 0L;

        for(Attendance learner : learnerList) {
            for(Scenario scenario : meetingSchedule.getScenarioList()) {
                for(Attendance patient : scenario.getPatients()) {
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
        }

        meetingSchedule.setMeetingList(meetingList);
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

    private void createRoomList(MeetingSchedule meetingSchedule, JsonArray rooms) {
        int roomListSize = rooms.size();
        List<Room> roomList = new ArrayList<>(roomListSize);
        for (int i = 0; i < roomListSize; i++) {
            JsonObject roomData = rooms.get(i).getAsJsonObject();
            Room room = new Room();
            room.setId((long) i);
            String name = roomData.get("name").getAsString();
            room.setName(name);
            int capacity = roomData.get("capacity").getAsInt();
            logger.trace("Created room with name ({}).", name);
            roomList.add(room);
        }
        meetingSchedule.setRoomList(roomList);
    }

    private Person createPerson(long id, JsonObject personData) {
        Person person = new Person();
        person.setId(id);
        String fullName = personData.get("name").getAsString();
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
