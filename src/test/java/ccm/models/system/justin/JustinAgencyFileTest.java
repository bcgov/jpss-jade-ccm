package ccm.models.system.justin;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ccm.models.system.justin.JustinAgencyFile;
import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinCourtFile;
import ccm.utils.JsonParseUtils;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.models.system.dems.DemsChargeAssessmentCaseData;
import ccm.models.system.dems.DemsFieldData;
import ccm.models.system.dems.DemsPersonData;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JustinAgencyFileTest {

    private JustinAgencyFile getJustinFile(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinAgencyFile agencyFile = objectMapper.readValue(reader, JustinAgencyFile.class);

            return agencyFile;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JustinAgencyFile();
    }

    private JustinAgencyFile getTestJustinFile() {
        String fileName = "json/system/justin/justin_agency_file.json";
        return getJustinFile(fileName);
    }

    private JustinAgencyFile getTestJustinFilePrimary() {
        String fileName = "json/system/justin/justin_agency_file_merge_primary.json";
        return getJustinFile(fileName);
    }

    private JustinAgencyFile getTestJustinFileSecond() {
        String fileName = "json/system/justin/justin_agency_file_merge_second.json";
        return getJustinFile(fileName);
    }

    private DemsChargeAssessmentCaseData getDemsFile(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            DemsChargeAssessmentCaseData agencyFile = objectMapper.readValue(reader, DemsChargeAssessmentCaseData.class);

            return agencyFile;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new DemsChargeAssessmentCaseData();
    }

    private DemsChargeAssessmentCaseData getTestDemsFile() {
        String fileName = "json/system/dems/dems_agency_file.json";
        return getDemsFile(fileName);
    }

    private DemsChargeAssessmentCaseData getTestDemsFileMerged() {
        String fileName = "json/system/dems/dems_agency_file_merged.json";
        return getDemsFile(fileName);
    }

    @Test
    public void testInitiatingAgency() {
        JustinAgencyFile agencyFile = getTestJustinFile();

        ChargeAssessmentData businessFile = new ChargeAssessmentData(agencyFile);
        // Initiating Agency: "$MAPID8: $MAPID7" INITIATING_AGENCY_IDENTIFIER: INITIATING_AGENCY_NAME
        
        assertEquals("105: Kelowna Municipal RCMP", businessFile.getInitiating_agency());

        // Investigating Officer: "$MAPID9 $MAPID10" INVESTIGATING_OFFICER_NAME INVESTIGATING_OFFICER_PIN
        assertEquals("Rhodes, Christopher 1001", businessFile.getInvestigating_officer());

        // Proposed Crown Office: "$MAPID14: $MAPID15" and Strip " Crown Counsel" from data  CRN_DECISION_AGENCY_IDENTIFIER: CRN_DECISION_AGENCY_NAME
        assertEquals("C402: Kelowna", businessFile.getProposed_crown_office());

        // ??? "$MAPID24 - $MAPID23"  ACCUSED\ PROPOSED_PROCESS_TYPE - ACCUSED\ACCUSED_NAME
        //assertEquals(businessFile., "AN - Thomasffffffffffffffffffffffff, Kenjjjjjjjjjjjjjjjjjjjjjjjjjjj Anthonyeeeeeeee Frankhhhhhhhhhhhh");

        // Earliest Offence Date Earliest: Offence date of the Accused Person array "$MAPID27" ACCUSED\OFFENCE_DATE
        assertEquals(agencyFile.getMin_offence_date(), businessFile.getEarliest_offence_date());

        // Earliest Proposed Appearance Date: Earliest Proposed Appearance Date of the Accused Person array "$MAPID25" ACCUSED\PROPOSED_APPR_DATE

        // Proposed Process Type List: Roll up of $MAPID70, data elements seperated by a semi colon ACCUSED\ PROPOSED_PROCESS_TYPE - ACCUSED\ACCUSED_NAME' ACCUSED\ PROPOSED_PROCESS_TYPE - ACCUSED\ACCUSED_NAME

        // Intimate partner violence Y/N: KFILE_YN IF Y then True, ELSE False

        // DEMS Case Name: Roll up of $MAPID23, data elements seperated by a semi colon ACCUSED\ACCUSED_NAME; ACCUSED\ACCUSED_NAME
        // Accused Full Name: Roll up of $MAPID119 Concatenate $MAPID85, $MAPID86 $MAPID118 and $MAPID84 with a space seperator if the $MAPIDX is not null.
        // Agency File: $MAPID8: $MAPID2 INITIATING_AGENCY_IDENTIFIER: AGENCY_FILE_NO
        // "If $MAPID113 = ""ACT"" then ""Received"" or 
        // $MAPID113 = ""CLS""  then  ""Close"" or
        // $MAPID113 = ""FIN""  then  ""Finish"" or
        // $MAPID113 = ""RET""  then  ""Return""" RCC_STATE_CD
        



        //System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);
        //DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData("1",businessFile);


    }

    @Test
    public void testAgencyFileConversion() {

        try {
            JustinAgencyFile agencyFile = getTestJustinFile();

            DemsChargeAssessmentCaseData expectedDemsCaseData = getTestDemsFile();

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

            ChargeAssessmentData businessFile = new ChargeAssessmentData(agencyFile);
            DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData("28", businessFile, null);

            // since Last JUSTIN Update is current date, set it to blank, so test doesn't fail
            for(DemsFieldData fieldData : demsCaseFile.getFields()) {
                if(fieldData.getName().equalsIgnoreCase("Last JUSTIN Update")) {
                    fieldData.setValue(null);
                }
            }
            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, agencyFile);
            //System.out.println("JustinAgencyFile JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, businessFile);
            //System.out.println("\n\nCommonChargeAssessmentCaseData JSON is\n"+stringFile2);

            StringWriter stringFile3 = new StringWriter();
            objectMapper.writeValue(stringFile3, demsCaseFile);
            //System.out.println("\n\nDemsChargeAssessmentCaseData JSON is\n"+stringFile3);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, expectedDemsCaseData);
            //System.out.println("\n\nExpectedDemsChargeAssessmentCaseData JSON is\n"+stringFile4);

            assertEquals(stringFile3.toString(), stringFile4.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testJsonParsing() {
        String fileName = "json/system/dems/dems_case.json";
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            //System.out.println("\nJSON is\n"+stringBuilder.toString());
            String id = JsonParseUtils.getJsonElementValue(stringBuilder.toString(), "id");
            String key = JsonParseUtils.getJsonElementValue(stringBuilder.toString(), "key");
            String status = JsonParseUtils.getJsonElementValue(stringBuilder.toString(), "status");
            //System.out.println("ID: " + id);
            //System.out.println("KEY: " + key);
            //System.out.println("STATUS: " + status);
            String extractedValue = JsonParseUtils.getJsonArrayElementValue(stringBuilder.toString(), "/fields", "/name", "Court File Unique ID", "/value");
            String kFileValue = JsonParseUtils.readJsonElementKeyValue(JsonParseUtils.getJsonArrayElement(stringBuilder.toString(), "/fields", "/name", "Case Flags", "/value")
                                                                     , "", "", "11", "");
            JsonNode node = JsonParseUtils.getJsonArrayElement(stringBuilder.toString(), "/fields", "/name", "Case Flag New", "/value");
            String kFileValueNew = JsonParseUtils.readJsonElementKeyValue(node, "", "/name", "K", "/id");
            JsonNode node2 = JsonParseUtils.getJsonArrayElement(stringBuilder.toString(), "/fields", "/name", "Case Flag Empty", "/value");
            String kFileValueEmpty = JsonParseUtils.readJsonElementKeyValue(node2, "", "/name", "K", "/id");
            //System.out.println(extractedValue);
            //System.out.println(kFileValue);
            //System.out.println(kFileValueNew);
            assertEquals("11", kFileValue);
            assertEquals("39137", extractedValue);
            assertEquals("11", kFileValueNew);
            assertEquals("", kFileValueEmpty);
            assertEquals("163", id);
            assertEquals("49408.0734", key);
            assertEquals("Active", status);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void testJsonCustFieldParsing() {
        String fileName = "json/system/dems/dems_custom_fields.json";
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            StringBuilder stringBuilder = new StringBuilder();
            String line = null;
            String ls = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append(ls);
            }
            //System.out.println("\nJSON is\n"+stringBuilder.toString());
            String value = JsonParseUtils.readJsonElementKeyValue(JsonParseUtils.getJsonArrayElement(stringBuilder.toString(), "", "/name", "Case Flags", "/listItems")
                                                 , "", "/name", "K", "/id");
            //System.out.println("\nJSON element value is:\n"+value);
            assertEquals("11", value);

        } catch (IOException e) {
            e.printStackTrace();
        }



    }


    @Test
    public void testMergeFileAgency() {
        try {
            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        
            JustinAgencyFile primaryAgencyFile = getTestJustinFilePrimary();
            JustinAgencyFile secondaryAgencyFile = getTestJustinFileSecond();

            ChargeAssessmentData businessPrimaryFile = new ChargeAssessmentData(primaryAgencyFile);
            ChargeAssessmentData businessSecondaryFile = new ChargeAssessmentData(secondaryAgencyFile);
            List<ChargeAssessmentData> secondaries = new ArrayList<ChargeAssessmentData>();
            secondaries.add(businessSecondaryFile);

            DemsChargeAssessmentCaseData demsCaseFile = new DemsChargeAssessmentCaseData("1",businessPrimaryFile, secondaries);

            DemsChargeAssessmentCaseData expectedDemsCaseData = getTestDemsFileMerged();

            // since Last JUSTIN Update is current date, set it to blank, so test doesn't fail
            for(DemsFieldData fieldData : demsCaseFile.getFields()) {
                if(fieldData.getName().equalsIgnoreCase("Last JUSTIN Update")) {
                    fieldData.setValue(null);
                }
            }

            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, demsCaseFile);
            //System.out.println("DemsAgencyFile JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, expectedDemsCaseData);
            //System.out.println("\n\nExpectedDemsChargeAssessmentCaseData JSON is\n"+stringFile2);

            assertEquals(stringFile.toString(), stringFile2.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }





}