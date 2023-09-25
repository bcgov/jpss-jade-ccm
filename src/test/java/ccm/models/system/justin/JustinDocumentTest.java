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
    public void testInitiatingAgency() {
        JustinDocumentList documentList = getTestJustinFile();
        ReportDocumentList rd = new ReportDocumentList(documentList);

        JustinDocument document = documentList.getDocuments().get(0);
        ReportDocument commonDocument = new ReportDocument(document);

        ChargeAssessmentDocumentData businessFile = new ChargeAssessmentDocumentData("123456", documentList.getCreate_date(), commonDocument);
        // Initiating Agency: "$MAPID8: $MAPID7" INITIATING_AGENCY_IDENTIFIER: INITIATING_AGENCY_NAME

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

            DemsRecordData demsFile = new DemsRecordData(businessFile);
            StringWriter stringFile4 = new StringWriter();
            objectMapper.writeValue(stringFile4, demsFile);
            //System.out.println("\n\nDemsRecordData JSON is\n"+stringFile4);

        } catch (IOException e) {
            e.printStackTrace();
        }

        //assertEquals("105: Kelowna Municipal RCMP", businessFile.getInitiating_agency());

        // Investigating Officer: "$MAPID9 $MAPID10" INVESTIGATING_OFFICER_NAME INVESTIGATING_OFFICER_PIN
        //assertEquals("Rhodes, Christopher 1001", businessFile.getInvestigating_officer());

        // Proposed Crown Office: "$MAPID14: $MAPID15" and Strip " Crown Counsel" from data  CRN_DECISION_AGENCY_IDENTIFIER: CRN_DECISION_AGENCY_NAME
        //assertEquals("C402: Kelowna", businessFile.getProposed_crown_office());

        // ??? "$MAPID24 - $MAPID23"  ACCUSED\ PROPOSED_PROCESS_TYPE - ACCUSED\ACCUSED_NAME
        //assertEquals(businessFile., "AN - Thomasffffffffffffffffffffffff, Kenjjjjjjjjjjjjjjjjjjjjjjjjjjj Anthonyeeeeeeee Frankhhhhhhhhhhhh");

        // Earliest Offence Date Earliest: Offence date of the Accused Person array "$MAPID27" ACCUSED\OFFENCE_DATE
        //assertEquals(agencyFile.getMin_offence_date(), businessFile.getEarliest_offence_date());

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


}