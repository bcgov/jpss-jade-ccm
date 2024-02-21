package ccm.models.system.justin;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import ccm.models.common.data.CourtCaseData;
import ccm.models.system.dems.DemsApprovedCourtCaseData;

public class JustinCourtFileTest {

    private JustinCourtFile getJustinCourtFile(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinCourtFile courtFile = objectMapper.readValue(reader, JustinCourtFile.class);

            return courtFile;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JustinCourtFile();
    }

    private JustinCourtFile getJustinCourtFile() {
        String fileName = "json/system/justin/justin_court_file.json";
        return getJustinCourtFile(fileName);
    }

    @Test
    public void testCourtFileConversion() {
        try {
            JustinCourtFile courtFile = getJustinCourtFile();

            CourtCaseData businessFile = new CourtCaseData(courtFile);

            // public DemsApprovedCourtCaseData(String key, String name, CourtCaseData primaryCourtCaseData, List<String> existingCaseFlags, List<CourtCaseData> courtCaseDataList) {
            //System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);
            DemsApprovedCourtCaseData demsCaseFile = new DemsApprovedCourtCaseData("1", "Name",businessFile, new ArrayList<String>(), new ArrayList<CourtCaseData>());

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsCaseFile);
            //System.out.println("\n\nExpectedDemsCaseData JSON is\n"+stringFile4);

            assertEquals(demsCaseFile.getFields().get(0).getValue(), "7893251");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}