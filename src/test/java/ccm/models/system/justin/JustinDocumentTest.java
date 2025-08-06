package ccm.models.system.justin;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
import ccm.models.common.data.document.ReportDocument;
import ccm.models.common.data.document.ReportDocumentList;
import ccm.models.system.dems.DemsRecordData;
import ccm.utils.JsonParseUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JustinDocumentTest {

    private JustinDocumentList getTestJustinFile() {
        String fileName = "json/system/justin/justin_document.json";
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinDocumentList document = objectMapper.readValue(reader, JustinDocumentList.class);

            return document;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JustinDocumentList();
    }

    private JustinDocumentList getTestJustinWitnessFile() {
        String fileName = "json/system/justin/justin_document_witness_statement.json";
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinDocumentList document = objectMapper.readValue(reader, JustinDocumentList.class);

            return document;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JustinDocumentList();
    }

    private JustinDocumentList getTestJustinSynopsisFile() {
        String fileName = "json/system/justin/justin_document_synopsis.json";
        ClassLoader classLoader = getClass().getClassLoader();

        try (InputStream inputStream = classLoader.getResourceAsStream(fileName);
             InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(streamReader)) {

            //create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            JustinDocumentList document = objectMapper.readValue(reader, JustinDocumentList.class);

            return document;

        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JustinDocumentList();
    }
    /*
    private DemsChargeAssessmentCaseData getTestDemsFile() {
        String fileName = "json/system/dems/dems_agency_file.json";
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
*/
    @Test
    public void testDocumentId() {
        JustinDocumentList documentList = getTestJustinFile();
        ReportDocumentList rd = new ReportDocumentList(documentList);

        JustinDocument document = documentList.getDocuments().get(0);
        ReportDocument commonDocument = new ReportDocument(document);

        ChargeAssessmentDocumentData businessFile = new ChargeAssessmentDocumentData("123456", documentList.getCreate_date(), commonDocument);
        // Initiating Agency: "$MAPID8: $MAPID7" INITIATING_AGENCY_IDENTIFIER: INITIATING_AGENCY_NAME

        DemsRecordData demsFile = new DemsRecordData(businessFile);
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        try {

            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, document);
            //System.out.println("JustinDocument JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, commonDocument);
            //System.out.println("\n\nReportDocument JSON is\n"+stringFile2);

            StringWriter stringFile3 = new StringWriter();
            objectMapper.writeValue(stringFile3, businessFile);
            //System.out.println("\n\nCourtCaseDocumentData JSON is\n"+stringFile3);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsFile);
            //System.out.println("\n\nDemsRecordData JSON is\n"+stringFile4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("703.23-1007.AI_SMITH-JOHNATHAN_230831", demsFile.getDocumentId());

    }

    @Test
    public void testWitnessStatement() {
        JustinDocumentList documentList = getTestJustinWitnessFile();
        ReportDocumentList rd = new ReportDocumentList(documentList);

        JustinDocument document = documentList.getDocuments().get(0);
        ReportDocument commonDocument = new ReportDocument(document);
        ChargeAssessmentDocumentData businessFile = new ChargeAssessmentDocumentData("123456", documentList.getCreate_date(), commonDocument);

        DemsRecordData demsFile = new DemsRecordData(businessFile);
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        try {

            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, document);
            //System.out.println("JustinDocument JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, commonDocument);
            //System.out.println("\n\nReportDocument JSON is\n"+stringFile2);

            StringWriter stringFile3 = new StringWriter();
            objectMapper.writeValue(stringFile3, businessFile);
            //System.out.println("\n\nCourtCaseDocumentData JSON is\n"+stringFile3);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsFile);
            //System.out.println("\n\nDemsRecordData JSON is\n"+stringFile4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("909.23-12345.STMT-POL_SMITH-JASON-JAY-PIN123456_240203", demsFile.getDocumentId());

    }

    @Test
    public void testWitnessStatement2() {
        JustinDocumentList documentList = getTestJustinWitnessFile();
        ReportDocumentList rd = new ReportDocumentList(documentList);

        ReportDocument commonDocument = rd.getDocuments().get(1);
        ChargeAssessmentDocumentData businessFile = new ChargeAssessmentDocumentData("123456", documentList.getCreate_date(), commonDocument);

        DemsRecordData demsFile = new DemsRecordData(businessFile);
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        try {

            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, documentList.getDocuments().get(1));
            //System.out.println("JustinDocument JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, commonDocument);
            //System.out.println("\n\nReportDocument JSON is\n"+stringFile2);

            StringWriter stringFile3 = new StringWriter();
            objectMapper.writeValue(stringFile3, businessFile);
            //System.out.println("\n\nCourtCaseDocumentData JSON is\n"+stringFile3);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsFile);
            //System.out.println("\n\nDemsRecordData JSON is\n"+stringFile4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String titleEncoded = JsonParseUtils.encodeUrlSensitiveChars(demsFile.getTitle());
            //System.out.println("Pre-Title:"+ demsFile.getTitle());
            //System.out.println("Post-Title:"+ titleEncoded);
            assertEquals("THE STORY CAFE - EATERY %26 BAR", titleEncoded);
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    @Test
    public void testWitnessStatement3() {
        JustinDocumentList documentList = getTestJustinWitnessFile();
        ReportDocumentList rd = new ReportDocumentList(documentList);

        ReportDocument commonDocument = rd.getDocuments().get(2);
        ChargeAssessmentDocumentData businessFile = new ChargeAssessmentDocumentData("123456", documentList.getCreate_date(), commonDocument);

        DemsRecordData demsFile = new DemsRecordData(businessFile);
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        try {

            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, documentList.getDocuments().get(2));
            //System.out.println("JustinDocument JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, commonDocument);
            //System.out.println("\n\nReportDocument JSON is\n"+stringFile2);

            StringWriter stringFile3 = new StringWriter();
            objectMapper.writeValue(stringFile3, businessFile);
            //System.out.println("\n\nCourtCaseDocumentData JSON is\n"+stringFile3);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsFile);
            //System.out.println("\n\nDemsRecordData JSON is\n"+stringFile4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String titleEncoded = JsonParseUtils.encodeUrlSensitiveChars(demsFile.getTitle());
            //System.out.println("Pre-Title:"+ demsFile.getTitle());
            //System.out.println("Post-Title:"+ titleEncoded);
            assertEquals("%3C%3E%25%21%23%5E*%3B%40%3D%7B%7D%7E%5C%24%26", titleEncoded);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    @Test
    public void testSynopsis() {
        JustinDocumentList documentList = getTestJustinSynopsisFile();
        ReportDocumentList rd = new ReportDocumentList(documentList);

        JustinDocument document = documentList.getDocuments().get(0);
        ReportDocument commonDocument = new ReportDocument(document);
        ChargeAssessmentDocumentData businessFile = new ChargeAssessmentDocumentData("123456", documentList.getCreate_date(), commonDocument);

        DemsRecordData demsFile = new DemsRecordData(businessFile);
        //create ObjectMapper instance
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        try {

            StringWriter stringFile = new StringWriter();
            objectMapper.writeValue(stringFile, document);
            //System.out.println("JustinDocument JSON is\n"+stringFile);

            StringWriter stringFile2 = new StringWriter();
            objectMapper.writeValue(stringFile2, commonDocument);
            //System.out.println("\n\nReportDocument JSON is\n"+stringFile2);

            StringWriter stringFile3 = new StringWriter();
            objectMapper.writeValue(stringFile3, businessFile);
            //System.out.println("\n\nCourtCaseDocumentData JSON is\n"+stringFile3);

            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsFile);
            //System.out.println("\n\nDemsRecordData JSON is\n"+stringFile4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        assertEquals("BURY.23-91538-3_.SYN_BURY-23-91538-3__240205", demsFile.getDocumentId());

    }


}