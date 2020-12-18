package org.blinemedical.examination.persistence;

import static org.blinemedical.examination.domain.TimeGrain.GRAIN_LENGTH_IN_MINUTES;
import static org.blinemedical.examination.persistence.MeetingSchedulingGenerator.createMeetingAssignmentList;
import static org.blinemedical.examination.persistence.MeetingSchedulingGenerator.createMeetingListAndAttendanceList;
import static org.blinemedical.examination.persistence.MeetingSchedulingGenerator.createTimeGrainList;

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Scanner;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.blinemedical.examination.app.ExaminationApp;
import org.blinemedical.examination.domain.Attendance;
import org.blinemedical.examination.domain.Meeting;
import org.blinemedical.examination.domain.MeetingAssignment;
import org.blinemedical.examination.domain.MeetingConstraintConfiguration;
import org.blinemedical.examination.domain.MeetingSchedule;
import org.blinemedical.examination.domain.Person;
import org.blinemedical.examination.domain.Room;
import org.blinemedical.examination.domain.Scenario;
import org.optaplanner.examples.common.app.CommonApp;
import org.optaplanner.examples.common.persistence.AbstractSolutionImporter;
import org.optaplanner.persistence.common.api.domain.solution.SolutionFileIO;

public class MeteorDataGenerator {

    private static final Logger logger = LogManager.getLogger(MeteorDataGenerator.class);
    static HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    static JsonFactory JSON_FACTORY = new JacksonFactory();

    public static void main(String[] args) throws IOException {
        //String DATA_DIR_SYSTEM_PROPERTY = "org.optaplanner.examples.dataDir";
        //File configFile = new File(System.getProperty(DATA_DIR_SYSTEM_PROPERTY, "/"), ".config");
        File configFile = new File(".config");
        String courseId = "";
        String token = "";
        try {
            Scanner myReader = new Scanner(configFile);
            while (myReader.hasNextLine()) {
                String data = myReader.nextLine();
                logger.debug("data line: ({})", data);
                if (data.contains("token=")) {
                    token = data.replace("token=", "");
                } else if(data.contains("courseId=")) {
                    courseId = data.replace("courseId=", "");
                }
            }
            myReader.close();
            if (token == "") {
                throw new Exception("missing token");
            }
        } catch (FileNotFoundException e) {
            logger.debug("Config not found ({})", configFile.toString());
        } catch (Exception e) {
            logger.debug("Error ({})", e.getMessage());
        }
        if (token == "") {
            return; //failed to get token from config, exit
        }

        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        System.out.println("Enter optional courseId:");

        String optionalCourseId = myObj.nextLine();  // Read user input
        if (optionalCourseId != "") {
            courseId = optionalCourseId;
        }
        logger.debug("Using courseId: ({})", courseId);
        
        
        MeteorDataGenerator generator = new MeteorDataGenerator();

        Instant startTime = Instant.parse("2020-12-18T08:00:00.00Z");
        Instant endTime = Instant.parse("2020-12-18T16:00:00.00Z");
        Duration meetingDuration = Duration.ofHours(1L);
        int meetingDurationInGrains = (int) (meetingDuration.toMinutes() / GRAIN_LENGTH_IN_MINUTES);

        HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(
            (HttpRequest request) -> request.setParser(new JsonObjectParser(JSON_FACTORY)));
        HttpHeaders headers = new HttpHeaders();
        headers.set("token", token);

        // all users
        HttpRequest request = requestFactory
            .buildGetRequest(new GenericUrl("http://localhost:8080/api/sample-data"))
            .setHeaders(headers);
        HttpResponse response = request.execute();

        JsonElement el = JsonParser.parseString(response.parseAsString());
        JsonArray rooms = el.getAsJsonObject().get("rooms").getAsJsonArray();
        logger.trace("Found ({}) rooms from meteor", rooms.size());

        JsonArray learners = el.getAsJsonObject().get("learners").getAsJsonArray();
        JsonArray patients = el.getAsJsonObject().get("patients").getAsJsonArray();
        JsonArray scenarios = el.getAsJsonObject().get("scenarios").getAsJsonArray();

        generator.writeMeetingSchedule(learners, patients, rooms, scenarios, startTime, endTime,
            meetingDurationInGrains, "allCourses");

        if (courseId != "") {
            // course only
            HttpRequest request2 = requestFactory.buildGetRequest(new GenericUrl(
                "http://localhost:8080/api/sample-data?courseId=" + courseId))
                .setHeaders(headers);
            HttpResponse response2 = request2.execute();

            JsonElement el2 = JsonParser.parseString(response2.parseAsString());
            JsonArray courseRooms = el2.getAsJsonObject().get("rooms").getAsJsonArray();
            JsonArray courseLearners = el2.getAsJsonObject().get("learners").getAsJsonArray();
            JsonArray coursePatients = el2.getAsJsonObject().get("patients").getAsJsonArray();
            JsonArray courseScenarios = el2.getAsJsonObject().get("scenarios").getAsJsonArray();
            generator.writeMeetingSchedule(courseLearners, coursePatients, courseRooms, courseScenarios,
                startTime, endTime, meetingDurationInGrains, "course-" + courseId);
        }
    }

    protected final SolutionFileIO<MeetingSchedule> solutionFileIO;
    protected final File outputDir;

    protected Random random;

    public MeteorDataGenerator() {
        solutionFileIO = new MeetingSchedulingXlsxFileIO();
        outputDir = new File(CommonApp.determineDataDir(ExaminationApp.DATA_DIR_NAME), "unsolved");
    }

    private void writeMeetingSchedule(JsonArray learners, JsonArray patients, JsonArray rooms,
        JsonArray scenarios, Instant startTime,
        Instant endTime, int durationInGrains,
        String fileNameSuffix) {
        int roomListSize = rooms.size();
        int learnersListSize = learners.size();
        int patientsListSize = patients.size();
        int scenarioListSize = scenarios.size();
        Duration meetingDuration = Duration.between(startTime, endTime);
        int timeGrainListSize = (int) (meetingDuration.toMinutes() / GRAIN_LENGTH_IN_MINUTES);

        String fileName = determineFileName(
            learnersListSize,
            patientsListSize,
            roomListSize,
            scenarioListSize,
            fileNameSuffix);
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

    private String determineFileName(int learnersListSize, int patientsListSize, int roomListSize,
        int scenarioListSize, String suffix) {
        return "MET-"
            + learnersListSize + "L-"
            + scenarioListSize + "SC-"
            + patientsListSize + "SP-"
            + roomListSize + "R"
            + "-" + suffix;
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
        List<Attendance> patientList = createPatients(meetingSchedule, patients, learnersListSize);
        createScenariosAndAddPatients(meetingSchedule, scenarios, patientList);
        createMeetingListAndAttendanceList(meetingSchedule, learnerList,
            durationInGrains);
        createTimeGrainList(meetingSchedule, startTime, timeGrainListSize);
        createRoomList(meetingSchedule, rooms);
        createMeetingAssignmentList(meetingSchedule);

        BigInteger possibleSolutionSize = BigInteger
            .valueOf((long) timeGrainListSize * roomListSize)
            .pow(meetingSchedule.getMeetingAssignmentList().size());
        logger.info(
            "MeetingSchedule {} has {} learners, {} scenarios, {} total patients, "
                + "{} timeGrains and {} rooms "
                + "with a search space of (#timeSlots * #rooms)^(#meetingAssignments)={}.",
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

            learnerList.add(learner);

            logger.trace("Created learner ({})", learner);
        }

        meetingSchedule.getAttendanceList().addAll(learnerList);
        meetingSchedule.getPersonList().addAll(personList);

        return learnerList;
    }

    private List<Attendance> createPatients(MeetingSchedule meetingSchedule, JsonArray patients,
        int learnersListSize) {
        int patientsListSize = patients.size();

        List<Attendance> patientList = new ArrayList<>(patientsListSize);
        List<Person> personList = new ArrayList<>();

        long attendanceId = learnersListSize;
        long personId = learnersListSize;

        for (int patientIdx = 0; patientIdx < patientsListSize; patientIdx++) {
            Attendance patient = new Attendance();
            patient.setId(attendanceId);
            attendanceId++;

            Person person = createPerson(personId++, patients.get(patientIdx).getAsJsonObject());
            person.setPatient(true);
            patient.setPerson(person);
            personList.add(person);

            patientList.add(patient);

            logger.trace("Created Patient ({})", patient);
        }

        meetingSchedule.getAttendanceList().addAll(patientList);
        meetingSchedule.getPersonList().addAll(personList);

        return patientList;
    }

    private void createScenariosAndAddPatients(
        MeetingSchedule meetingSchedule,
        JsonArray scenarios,
        List<Attendance> patientList) {
        int numScenarios = scenarios.size();

        List<Scenario> scenarioList = new ArrayList<>(numScenarios);

        long scenarioId = 0L;

        for (int scenarioIdx = 0; scenarioIdx < numScenarios; scenarioIdx++) {
            JsonObject scenarioData = scenarios.get(scenarioIdx).getAsJsonObject();
            Scenario scenario = new Scenario();
            scenario.setId(scenarioId);
            String scenarioName = scenarioData.get("privateTitle").getAsString();
            scenario.setName(scenarioName);
            scenarioId++;

            JsonArray patientsData = scenarioData.get("patients").getAsJsonArray();
            int patientsPerScenario = patientsData.size();
            scenario.setPatients(new ArrayList<>());

            for (int patientIdx = 0; patientIdx < patientsPerScenario; patientIdx++) {
                JsonObject patientData = patientsData.get(patientIdx).getAsJsonObject();
                String patientDataId = patientData.get("userId").getAsString();
                String patientDataName = patientData.get("name").getAsString();

                logger.trace("Looking for name ({}), id ({}), in scenario ({}).",
                    patientDataName, patientDataId, scenario.getName());
                Optional<Attendance> patientToAdd = patientList.stream()
                    .filter(patient -> patient.getPerson().getPersonId()
                        .equalsIgnoreCase(patientDataId))
                    .findAny();
                if (patientToAdd.isPresent()) {
                    Attendance patient = patientToAdd.get();
                    scenario.getPatients().add(patient);
                    logger.trace("Found patient ({}), adding them to scenario ({}).",
                        patient.getPerson().getFullName(), scenario.getName());
                } else {
                    logger.error("Did not find patient ({}) in scenario ({}).",
                        patientDataName, scenario.getName());
                }
            }

            scenarioList.add(scenario);
            logger.trace("Created scenario with name ({}) and ({}) patients.",
                scenario.getName(), scenario.getPatients().size());
        }

        meetingSchedule.setScenarioList(scenarioList);
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
            //int capacity = roomData.get("capacity").getAsInt();
            logger.trace("Created room with name ({}).", name);
            roomList.add(room);
        }
        meetingSchedule.setRoomList(roomList);
    }

    private Person createPerson(long id, JsonObject personData) {
        Person person = new Person();
        person.setId(id);
        person.setPersonId(personData.get("userId").getAsString());
        String fullName = personData.get("name").getAsString();
        person.setFullName(fullName);
        logger.trace("Created person with fullName ({}).",
            fullName);
        return person;
    }
}
