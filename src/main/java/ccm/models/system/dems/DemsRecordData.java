package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ccm.models.common.event.ReportEvent.REPORT_TYPES;
import ccm.models.common.data.FileNote;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
import ccm.models.common.data.document.CourtCaseDocumentData;
import ccm.models.common.data.document.ImageDocumentData;
import ccm.utils.DateTimeUtils;

public class DemsRecordData {

    private static final String TXT_FILE_EXTENSION = ".txt";
    private String descriptions;
    private String title;
    private String startDate;
    private String originalFileNumber;
    private String dateToCrown;
    private String source;
    private String custodian;
    private String location;
    private String folder;
    private String documentId;
    private String fileExtension;
    private String primaryDateUtc;
    private String type;
    private String lastApiRecordUpdate;
    private List<DemsFieldData> fields;
    private String reportType;
    private int incrementalDocCount = 1;
    private String image_id;
    private String caseNoteCategory;

    public String DescriptipnShortFormValue(String formTypeDescription) {
        Map<String, String> map = new HashMap<>();
        
        map.put("11.1 UTA - POLICE", "UTA P");
        map.put("ACCUSED HISTORY REPORT","AHR");
        map.put("ACCUSED INFORMATION","AI");
        map.put("RECOGNIZANCE AFTER ALLEGATION","RECOG ALL");
        map.put("RECOGNIZANCE AFTER ALLEGATION-YTH","RECOG ALL YTH");
        map.put("APPEARANCE NOTICE","AN");
        map.put("APPEARANCE NOTICE/11.1 UTA","AN UTA");
        map.put("WARRANT FOR ARREST REVIEW OF SENTENCE","ARREST ROS");
        map.put("WARRANT FOR ARREST REVIEW YOUTH SENTENCE","ARREST ROS YTH");
        map.put("BC DV / IPV RISK SUMMARY","BCDV IPV RISK");
        map.put("WARRANT OF COMMITTAL (BREACH OF CSO)","COM BR CSO");
        map.put("WARRANT OF COMMITTAL (JAIL)","COM JAIL");
        map.put("WARRANT OF COMMITTAL TO CUSTODY-YTH","COM YTH");
        map.put("RECOGNIZANCE COMMON LAW PEACE BOND","PEACE BOND");
        map.put("RECOGNIZANCE COMMON LAW PEACE BOND - YTH","PEACE BOND YTH");
        map.put("RESTITUTION COMPENSATION SENTENCING ORDER - YTH","RCSO YTH");
        map.put("ORDER TO COMPLY WITH SOIRA","SOIRA");
        map.put("CONDITIONAL SENTENCE ORDER","CSO");
        map.put("CERTIFICATE OF CONVICTION","CONVICT");
        map.put("CONVICTION LIST-DEFAULT","CL-D");
        map.put("CONVICTION LIST-FILTERED","CL-F");
        map.put("CLIENT HISTORY REPORT - DISPOSITION AND REPORTS","CORNET-DISPO");
        map.put("CLIENT HISTORY REPORT - FULL","CORNET-FULL");
        map.put("CPIC-CR", "CPIC-CR");
        map.put("CUSTODY AND SUPERVISION ORDER","DEF CUS SO YTH");
        map.put("DIGITAL MEDIA RETENTION","MR");
        map.put("DISCHARGE-SENTENCING ORDER","CD SO YTH");
        map.put("FILE SUMMARY REPORT","FSR");
        map.put("IMPOSITION OF-CONVICTION (FED VT)","FINE IOC FED VT");
        map.put("IMPOSITION OF-CONVICTION (OA)","FINE IOC OA");
        map.put("SWORN INFORMATION","INFO");
        map.put("INTENSIVE SUPPORT & SUPERVISION","DISS YTH");
        map.put("NARRATIVE","NAR");
        map.put("OA SUSP SNT PROBATION","RECOG OA SUSP SNT");
        map.put("RESTITUTION ORDER","RES OR");
        map.put("FINE ORDER AND NOTICE OF VICTIM SURCHARGE","FONVS");
        map.put("FIREARMS ORDER NOTICE - PREVENTIVE","FIREARMS OR NOT PRE");
        map.put("PROBATION ORDER-YTH","PO YTH");
        map.put("SUMMONS PERSON REVIEW OF SENTENCE","SUM PR OS");
        map.put("PROBATION ORDER (CONDITIONAL DISCHARGE)","PO CD");
        map.put("PROBATION ORDER (CONDITIONAL SENTENCE)","PO CS");
        map.put("PROBATION ORDER (FINE)","PO FINE");
        map.put("PROBATION ORDER (INTERMITTENT)","PO INT");
        map.put("PROBATION ORDER (OA)(FINE)","PO OA FINE");
        map.put("PROBATION ORDER (OA)(INTERMITTENT)","PO OA INT");
        map.put("PROBATION ORDER (OA)(PRISON)","PO OA PRI");
        map.put("PROBATION ORDER (OA)(SUSPENDED SENTENCE)","PO OA SUS SEN");
        map.put("PROBATION ORDER (PRISON)","PO PRI");
        map.put("PROBATION ORDER (SUSPENDED SENTENCE)","PO SUS SEN");
        map.put("DIVIDED PROBATION ORDER-YTH","DPO YTH");
        map.put("ORDER OF PROHIBITION - SEXUAL OFFENCE","OR PRO SO");
        map.put("ORDER OF PROHIBITION DRIVING - CC","OR PRO DRI CC");
        map.put("ORDER OF PROHIBITION DRIVING - CC-YTH","OR PRO DRI CC YTH");
        map.put("ORDER OF PROHIBITION DRIVING (MVA)-OA","OR PRO DRI MVA OA");
        map.put("ORDER OF PROHIBITION DRIVING MVA(OA)-YTH","OR PRO DRI MVA OA YTH");
        map.put("PROMISE TO APPEAR","PTA");
        map.put("PROMISE TO APPEAR/11.1 UTA","PTA UTA");
        map.put("RECOGNIZANCE - APPEAL","RECOG APPEAL");
        map.put("RECOGNIZANCE - BAIL (OA)","RECOG BAIL OA");
        map.put("RECOGNIZANCE - BAIL (YTH)","RECOG BAIL YTH");
        map.put("RECOGNIZANCE (OA PEACE BOND)","RECOG OA PEACE BOND");
        map.put("RECOGNIZANCE OF BAIL","RECOG BAIL");
        map.put("RECOGNIZANCE OF BAIL (OA) YTH","RECOG BAIL OA YTH");
        map.put("RECOGNIZANCE- OFFICER IN CHARGE","RECOG OIC");
        map.put("RECOGNIZANCE- OFFICER IN CHARGE/11.1 UTA","RECOG OIC UTA");
        map.put("RECORD OF PROCEEDINGS","ROP");
        map.put("INTENSIVE REHABILITATIVE SUPERVISION","IRS YTH");
        map.put("RELEASE ORDER", "RO");
        map.put("RELEASE ORDER - YOUTH","RO YTH");
        map.put("REQUEST FOR PAYMENT","REQ PAY");
        map.put("REQUEST FOR PAYMENT (FINAL)","REQ PAY FIN");
        map.put("NOTICE OF REVIEW OF YOUTH SENTENCE","NRS YTH");
        map.put("SUMMONS REVIEW OF YOUTH SENTENCE (OA-CC)","SUM RS YTH");
        map.put("FINE SENTENCING ORDER -YTH","FINE SO YTH");
        map.put("COMMUNITY SERVICE SENTENCING ORDER -YTH","COM SER SO YTH");
        map.put("STATEMENT-EXPERT","STMT-EXP");
        map.put("STATEMENT-POLICE","STMT-POL");
        map.put("STATEMENT-VICTIM","STMT-VIC");
        map.put("STATEMENT-WITNESS","STMT-WIT");
        map.put("CONDITIONAL SUPERVISION FOLLOW CUSTODY","CON SUP CUS YTH");
        map.put("COMMUNITY SUPERVISION FOLLOWING CUSTODY","COMM SUP CUS YTH");
        map.put("CONDITIONAL SUPERVISION FOLLOWING REVIEW","CON SUP REV YTH");
        map.put("SUPPLEMENTAL","SUPP");
        map.put("INTENSIVE SUPPORT AND SUPERVISION ORDER","ISSO YTH");
        map.put("SYNOPSIS","SYN");
        map.put("UNDERTAKING - FORM 10 - POLICE","UTA POLICE");
        map.put("UNDERTAKING (RESP. PERSON/YTH)","UTA YTH");
        map.put("UNDERTAKING BY APPELLANT (DEFENDANT)","UTA APPEELLANT");
        map.put("UNDERTAKING TO A PEACE OFFICER/OIC - YTH","UTA OIC YTH");
        map.put("UNDERTAKING TO JUDGE/JP","UTA JUDGE JP");
        map.put("UNDERTAKING TO JUDGE/JP (YTH)","UTA JUDGE JP YTH");
        map.put("VEHICLES","VEH");
        map.put("WARRANT OF REMAND (ENFORCEMENT HR) (YTH)","WR YTH");

        return map.get(formTypeDescription);
    }

    public DemsRecordData() {
    }

    public DemsRecordData(ChargeAssessmentDocumentData nrd) {
        // find the report type
        setReportType(nrd.getReport_type());
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        String descriptionShortForm = getDescriptions();
        if(report != null) {
            setDescriptions(report.getDescription());
            if(report.equals(REPORT_TYPES.WITNESS_STATEMENT)) {
                //"If $MAPID21 = Y then ""STATEMENT - VICTIM"";
                //ELSE If $MAPID19 = Y then ""STATEMENT - EXPERT"";
                //ELSE If $MAPID20 = Y then ""STATEMENT - POLICE"";
                //ELSE If $MAPID18 = Y then ""STATEMENT - CIVILIAN"""
                if(nrd.getVictim_yn() != null && nrd.getVictim_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-VIC";
                    setDescriptions(report.getDescription()+"-VICTIM");
                } else if(nrd.getExpert_yn() != null && nrd.getExpert_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-EXP";
                    setDescriptions(report.getDescription()+"-EXPERT");
                } else if(nrd.getPolice_officer_yn() != null && nrd.getPolice_officer_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-POL";
                    setDescriptions(report.getDescription()+"-POLICE");
                } else if(nrd.getWitness_yn() != null && nrd.getWitness_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-WIT";
                    setDescriptions(report.getDescription()+"-WITNESS");
                } else {
                    descriptionShortForm = report.getLabel()+"-CIV";
                }
            } else {
                descriptionShortForm = report.getLabel();
            }

            if(report.equals(REPORT_TYPES.CPIC)) {
                if(nrd.getParticipant_name() != null) {
                    setTitle(nrd.getParticipant_name().toUpperCase());
                } else {
                    setTitle("CRIMINAL RECORD");
                }
            } else if(report.equals(REPORT_TYPES.WITNESS_STATEMENT)) {
                //"""If ($MAPID20 = Y AND $MAPID17 is not empty) then """"$MAPID16 (PIN$MAPID17)""""
                //Else $MAPID16"""
                if(nrd.getPolice_officer_yn() != null && nrd.getPolice_officer_yn().equalsIgnoreCase("Y")
                && nrd.getOfficer_pin_number() != null) {
                    setTitle(nrd.getWitness_name().toUpperCase() + " (PIN" + nrd.getOfficer_pin_number() + ")");
                } else {
                    setTitle(nrd.getWitness_name().toUpperCase());
                }
            } else if(report.equals(REPORT_TYPES.ACCUSED_HISTORY_REPORT)) {
                if(nrd.getParticipant_name() != null) {
                    setTitle(nrd.getParticipant_name().toUpperCase());
                } else {
                    setTitle(report.getDescription());
                }
            } else if(report.equals(REPORT_TYPES.ACCUSED_INFO)) {
                if(nrd.getParticipants() != null) {
                    setTitle(nrd.getParticipants().toUpperCase());
                } else {
                    setTitle(report.getDescription());
                }
            } else {
                setTitle(nrd.getAgency_file_no());
            }
            setOriginalFileNumber(nrd.getAgency_file_no());


            if(report.equals(REPORT_TYPES.NARRATIVE)) {
                setType("OPERATIONAL");
            } else if(report.equals(REPORT_TYPES.SYNOPSIS)) {
                setType("OPERATIONAL");
            } else if(report.equals(REPORT_TYPES.CPIC)) {
                setType("BIOGRAPHICAL");
            } else if(report.equals(REPORT_TYPES.WITNESS_STATEMENT)) {
                setType("STATEMENT");
            } else if(report.equals(REPORT_TYPES.DV_IPV_RISK)) {
                setType("OPERATIONAL");
            } else if(report.equals(REPORT_TYPES.DV_ATTACHMENT)) {
                setType("OPERATIONAL");
            } else if(report.equals(REPORT_TYPES.DM_ATTACHMENT)) {
                setType("ADMINISTRATIVE");
            } else if(report.equals(REPORT_TYPES.VEHICLE)) {
                setType("PARTNER AGENCY");
            } else if(report.equals(REPORT_TYPES.SUPPLEMENTAL)) {
                setType("OPERATIONAL");
            } else if(report.equals(REPORT_TYPES.ACCUSED_INFO)) {
                setType("BIOGRAPHICAL");
            }

        }

        setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getSubmit_date()));
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(getStartDate());
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        String shortendStartDate = DateTimeUtils.shortDateTimeString(getStartDate());

        //BCPSDEMS-1342 - doc id rules
        String fileNo = getOriginalFileNumber();
        if(fileNo != null) {
            fileNo = fileNo.replaceAll(":", ".");
            fileNo = fileNo.replaceAll(" ", "");
        }
        String docId = fileNo+"."+descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate;
        docId=docId.replaceAll("\\(", "");
        docId=docId.replaceAll("\\)", "");
        docId=docId.replaceAll(",", "");
        docId=docId.replaceAll(":", "");
        docId=docId.replaceAll(" ", "-");
        docId=docId.replaceAll("[\\\\/+]", "_");
        docId=docId.replaceAll("\"", "");
        setDocumentId(docId);

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        if(getDocumentId() != null) {
            DemsFieldData documentId = new DemsFieldData("Document ID", getDocumentId());
            fieldData.add(documentId);
        }
        if(getType() != null) {
            DemsFieldData type = new DemsFieldData("Type", getType());
            fieldData.add(type);
        }
        if(getDescriptions() != null) {
            DemsFieldData descriptions = new DemsFieldData("Descriptions", getDescriptions());
            fieldData.add(descriptions);
        }
        if(getTitle() != null) {
            DemsFieldData title = new DemsFieldData("Title", getTitle());
            fieldData.add(title);
        }
        if(getStartDate() != null) {
            DemsFieldData startDate = new DemsFieldData("Start Date", getStartDate());
            fieldData.add(startDate);
        }
        if(getDateToCrown() != null) {
            DemsFieldData dateToCrown = new DemsFieldData("Date To Crown", getDateToCrown());
            fieldData.add(dateToCrown);
        }
        if(getSource() != null) {
            DemsFieldData source = new DemsFieldData("Source", getSource());
            fieldData.add(source);
        }
        if(getFileExtension() != null) {
            DemsFieldData extension = new DemsFieldData("File Extension", getFileExtension());
            fieldData.add(extension);
        }
        if(nrd.getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", nrd.getLocation());
            fieldData.add(location);
        }
        if(getLastApiRecordUpdate() != null) {
            DemsFieldData lastUpdate = new DemsFieldData("Last API Record Update", getLastApiRecordUpdate());
            fieldData.add(lastUpdate);
        }
        /*if(getFolder() != null) {
            DemsFieldData folder = new DemsFieldData("Folder", getFolder());
            fieldData.add(folder);
        }*/
        if(getOriginalFileNumber() != null){
            DemsFieldData originalFileNumber = new DemsFieldData("Original File Number", getOriginalFileNumber());
            fieldData.add(originalFileNumber);
        }
        DemsFieldData ledger = new DemsFieldData("Is Ledger", "false");
        fieldData.add(ledger);

        setFields(fieldData);
    }

    public DemsRecordData(CourtCaseDocumentData nrd) {
        // find the report type
        setReportType(nrd.getReport_type());
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        setDescriptions(nrd.getDocument_type().toUpperCase());
        String descriptionShortForm = getDescriptions();
        if(report != null) {
            setDescriptions(report.getDescription());
            if(report.equals(REPORT_TYPES.CONVICTION_LIST) && nrd.getFiltered_yn() != null && "Y".equals(nrd.getFiltered_yn())) {
                descriptionShortForm = report.getLabel()+"-F";
                setDescriptions(nrd.getDocument_type().toUpperCase()+"-FILTERED");
            } else if(report.equals(REPORT_TYPES.CONVICTION_LIST)) {
                descriptionShortForm = report.getLabel()+"-D";
                setDescriptions(nrd.getDocument_type().toUpperCase()+"-DEFAULT");
            } else {
                descriptionShortForm = report.getLabel();
            }

            if(report.equals(REPORT_TYPES.RECORD_OF_PROCEEDINGS) && nrd.getCourt_file_no() != null) {
                //$MAPID67 $MAPID69
                setTitle(nrd.getParticipant_name().toUpperCase() + " " + nrd.getCourt_file_no());
            } else if(report.equals(REPORT_TYPES.FILE_SUMMARY_REPORT)) {
                setTitle(nrd.getCourt_file_no());
            } else {
                setTitle(nrd.getParticipant_name().toUpperCase());
            }

            if(report.equals(REPORT_TYPES.RECORD_OF_PROCEEDINGS)) {
                setType("COURT RECORD");
            } else if(report.equals(REPORT_TYPES.FILE_SUMMARY_REPORT)) {
                setType("JUSTIN RECORD");
            } else {
                setType("BIOGRAPHICAL");
            }
        }
        setOriginalFileNumber(nrd.getCourt_file_no());
        setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getGeneration_date()));
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(getStartDate());
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        String shortendStartDate = DateTimeUtils.shortDateTimeString(getStartDate());

        //BCPSDEMS-1342 - doc id rules
        String fileNo = getOriginalFileNumber();
        if(fileNo != null) {
            fileNo = fileNo.replaceAll(":", ".");
            fileNo = fileNo.replaceAll(" ", "");
        }
        String docId = fileNo+"."+descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate;
        docId=docId.replaceAll("\\(", "");
        docId=docId.replaceAll("\\)", "");
        docId=docId.replaceAll(",", "");
        docId=docId.replaceAll(":", "");
        docId=docId.replaceAll(" ", "-");
        docId=docId.replaceAll("[\\\\/+]", "_");
        docId=docId.replaceAll("\"", "");
        setDocumentId(docId);

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        if(getDocumentId() != null) {
            DemsFieldData documentId = new DemsFieldData("Document ID", getDocumentId());
            fieldData.add(documentId);
        }
        if(getType() != null) {
            DemsFieldData type = new DemsFieldData("Type", getType());
            fieldData.add(type);
        }
        if(getDescriptions() != null) {
            DemsFieldData descriptions = new DemsFieldData("Descriptions", getDescriptions());
            fieldData.add(descriptions);
        }
        if(getTitle() != null) {
            DemsFieldData title = new DemsFieldData("Title", getTitle());
            fieldData.add(title);
        }
        if(getStartDate() != null) {
            DemsFieldData startDate = new DemsFieldData("Start Date", getStartDate());
            fieldData.add(startDate);
        }
        if(getDateToCrown() != null) {
            DemsFieldData dateToCrown = new DemsFieldData("Date To Crown", getDateToCrown());
            fieldData.add(dateToCrown);
        }
        if(getSource() != null) {
            DemsFieldData source = new DemsFieldData("Source", getSource());
            fieldData.add(source);
        }
        if(getFileExtension() != null) {
            DemsFieldData extension = new DemsFieldData("File Extension", getFileExtension());
            fieldData.add(extension);
        }
        if(nrd.getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", nrd.getLocation());
            fieldData.add(location);
        }
        if(getLastApiRecordUpdate() != null) {
            DemsFieldData lastUpdate = new DemsFieldData("Last API Record Update", getLastApiRecordUpdate());
            fieldData.add(lastUpdate);
        }
        /*if(getFolder() != null) {
            DemsFieldData folder = new DemsFieldData("Folder", getFolder());
            fieldData.add(folder);
        }*/
        if(getOriginalFileNumber() != null){
            DemsFieldData originalFileNumber = new DemsFieldData("Original File Number", getOriginalFileNumber());
            fieldData.add(originalFileNumber);
        }
        DemsFieldData ledger = new DemsFieldData("Is Ledger", "false");
        fieldData.add(ledger);


        setFields(fieldData);
    }

    public DemsRecordData(ImageDocumentData nrd) {
        // find the report type
        setReportType(nrd.getReport_type());
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        setDescriptions(nrd.getForm_type_description().toUpperCase());
        String descriptionShortForm = getDescriptions();
        if(report != null) {
            //descriptionShortForm = report.getLabel();
            descriptionShortForm =  DescriptipnShortFormValue(nrd.getForm_type_description().toUpperCase());
            if(descriptionShortForm == null){
                descriptionShortForm = nrd.getForm_type_description().toUpperCase().replaceAll("[\\\\/:*?\",<>|]", "");
            }
            if(report.equals(REPORT_TYPES.INFORMATION)) {
                setType("COURT RECORD");
            } else if(report.equals(REPORT_TYPES.RELEASE_DOCUMENT)) {
                setType("PROCESS RECORD");
            } else if(report.equals(REPORT_TYPES.SENTENCE_DOCUMENT)) {
                setType("COURT RECORD");
            } else {
                setType("COURT RECORD");
            }

            if(report.equals(REPORT_TYPES.INFORMATION)) {
                setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getSworn_date()));
            } else {
                setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getIssue_date()));
            }
        }

        if(report.equals(REPORT_TYPES.INFORMATION) && nrd.getCourt_file_number() != null) {
            //$MAPID67 $MAPID69
            setTitle(nrd.getCourt_file_number());
        } else {
            setTitle(nrd.getParticipant_name().toUpperCase() + " " + nrd.getCourt_file_number());
        }
        setOriginalFileNumber(nrd.getCourt_file_number());
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(getStartDate());
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        String shortendStartDate = DateTimeUtils.shortDateTimeString(getStartDate());

        //jade-2617
        setImage_id(nrd.getImage_id());
        //BCPSDEMS-1342 - doc id rules
        String fileNo = getOriginalFileNumber();
        if(fileNo != null) {
            fileNo = fileNo.replaceAll(":", ".");
            fileNo = fileNo.replaceAll(" ", "");
        }
        String docId = fileNo+"."+descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate;
        docId=docId.replaceAll("\\(", "");
        docId=docId.replaceAll("\\)", "");
        docId=docId.replaceAll(",", "");
        docId=docId.replaceAll(":", "");
        docId=docId.replaceAll(" ", "-");
        docId=docId.replaceAll("[\\\\/+]", "_");
        docId=docId.replaceAll("\"", "");
        setDocumentId(docId);

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        if(getDocumentId() != null) {
            DemsFieldData documentId = new DemsFieldData("Document ID", getDocumentId());
            fieldData.add(documentId);
        }
        if(getType() != null) {
            DemsFieldData type = new DemsFieldData("Type", getType());
            fieldData.add(type);
        }
        if(getDescriptions() != null) {
            DemsFieldData descriptions = new DemsFieldData("Descriptions", getDescriptions());
            fieldData.add(descriptions);
        }
        if(getTitle() != null) {
            DemsFieldData title = new DemsFieldData("Title", getTitle());
            fieldData.add(title);
        }
        if(getStartDate() != null) {
            DemsFieldData startDate = new DemsFieldData("Start Date", getStartDate());
            fieldData.add(startDate);
        }
        if(getDateToCrown() != null) {
            DemsFieldData dateToCrown = new DemsFieldData("Date To Crown", getDateToCrown());
            fieldData.add(dateToCrown);
        }
        if(getSource() != null) {
            DemsFieldData source = new DemsFieldData("Source", getSource());
            fieldData.add(source);
        }
        if(getFileExtension() != null) {
            DemsFieldData extension = new DemsFieldData("File Extension", getFileExtension());
            fieldData.add(extension);
        }
        if(nrd.getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", nrd.getLocation());
            fieldData.add(location);
        }
        if(getLastApiRecordUpdate() != null) {
            DemsFieldData lastUpdate = new DemsFieldData("Last API Record Update", getLastApiRecordUpdate());
            fieldData.add(lastUpdate);
        }
        /*if(getFolder() != null) {
            DemsFieldData folder = new DemsFieldData("Folder", getFolder());
            fieldData.add(folder);
        }*/
        if(getOriginalFileNumber() != null){
            DemsFieldData originalFileNumber = new DemsFieldData("Original File Number", getOriginalFileNumber());
            fieldData.add(originalFileNumber);
        }
        if(getImage_id() != null){
            DemsFieldData imageID = new DemsFieldData("JUSTIN Image ID", getImage_id());
            fieldData.add(imageID);
        }
        DemsFieldData ledger = new DemsFieldData("Is Ledger", "false");
        fieldData.add(ledger);


        setFields(fieldData);
    }

    public DemsRecordData(FileNote nrd) {
        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        if(nrd.getNote_txt() != null) {
            DemsFieldData notetxt = new DemsFieldData("Notes", nrd.getNote_txt());
            fieldData.add(notetxt);
        }
        if(nrd.getUser_name() != null) {
            DemsFieldData username = new DemsFieldData("Author", nrd.getUser_name());
            fieldData.add(username);
        }
        if(nrd.getEntry_date() != null) {
            DemsFieldData entry_date = new DemsFieldData("Entry Date", DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getEntry_date()));
            fieldData.add(entry_date);
        }
        setOriginalFileNumber(nrd.getMdoc_justin_no());
        if(getOriginalFileNumber() != null) {
            DemsFieldData title = new DemsFieldData("Justin Court File No", getOriginalFileNumber());
            fieldData.add(title);
        }
        setSource("BCPS Work");
        if(getSource() != null) {
            DemsFieldData source = new DemsFieldData("Source", getSource());
            fieldData.add(source);
        }
        setCaseNoteCategory("JUSTIN");
        if(getCaseNoteCategory() != null){
            DemsFieldData caseNoteCategory = new DemsFieldData("Case Note Category", getCaseNoteCategory());
            fieldData.add(caseNoteCategory); 
        }
        setDocumentId(nrd.getFile_note_id());
        if(getDocumentId() != null) {
            DemsFieldData documentId = new DemsFieldData("Document ID", getDocumentId());
            fieldData.add(documentId);
        }
        setType("BCPS-CASE NOTES");
        if(getType() != null) {
            DemsFieldData type = new DemsFieldData("Type", getType());
            fieldData.add(type);
        } 
        StringBuilder notes = new StringBuilder();
        if(nrd.getNote_txt().length() > 256) {
            String truncatedCaseName = nrd.getNote_txt().substring(0, 256);
            notes.append(truncatedCaseName);
            notes.append(" ...");
        }
        setDescriptions(notes.toString());
        if(getDescriptions() != null) {
            DemsFieldData descriptions = new DemsFieldData("Descriptions", getDescriptions());
            fieldData.add(descriptions);
        }
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        if(getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", getLocation());
            fieldData.add(location);
        }
        if(getLastApiRecordUpdate() != null) {
            DemsFieldData lastUpdate = new DemsFieldData("Last API Record Update", getLastApiRecordUpdate());
            fieldData.add(lastUpdate);
        }
        setFileExtension(TXT_FILE_EXTENSION);
        if(getFileExtension() != null) {
            DemsFieldData extension = new DemsFieldData("File Extension", getFileExtension());
            fieldData.add(extension);
        }

        setFields(fieldData);
    }

    public void incrementDocumentId() {
        // update the document id with the next incremental id
        // in case of INFORMATION report type,
        int shortDescIndex = getDocumentId().indexOf("_");
        String trimmedDocId = getDocumentId().substring(0, shortDescIndex);

        String shortendStartDate = DateTimeUtils.shortDateTimeString(getStartDate());

        StringBuffer docId = new StringBuffer(trimmedDocId);
        docId.append("_");
        docId.append(getTitle());
        docId.append("_");
        docId.append(incrementalDocCount++);
        docId.append("_");
        docId.append(shortendStartDate);
        updateDocumentId(docId.toString().replaceAll(":", ""));
    }

    public void updateDocumentId(String newDocumentId) {
        setDocumentId(newDocumentId);

        // Now need to go through the field records, and find the "Document Id"
        for(DemsFieldData fd : getFields()) {
          if(fd.getName().equalsIgnoreCase("Document ID")) {
            fd.setValue(getDocumentId());
          }
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getCustodian() {
        return custodian;
    }

    public void setCustodian(String custodian) {
        this.custodian = custodian;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getPrimaryDateUtc() {
        return primaryDateUtc;
    }

    public void setPrimaryDateUtc(String primaryDateUtc) {
        this.primaryDateUtc = primaryDateUtc;
    }

    public String getDescriptions() {
        return descriptions;
    }

    public void setDescriptions(String descriptions) {
        this.descriptions = descriptions;
    }

    public String getDateToCrown() {
        return dateToCrown;
    }

    public void setDateToCrown(String dateToCrown) {
        this.dateToCrown = dateToCrown;
    }

    public String getOriginalFileNumber() {
        return originalFileNumber;
    }

    public void setOriginalFileNumber(String originalFileNumber) {
        this.originalFileNumber = originalFileNumber;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLastApiRecordUpdate() {
        return lastApiRecordUpdate;
    }

    public void setLastApiRecordUpdate(String lastApiRecordUpdate) {
        this.lastApiRecordUpdate = lastApiRecordUpdate;
    }

    public int getIncrementalDocCount() {
        return incrementalDocCount;
    }

    public void setIncrementalDocCount(int incrementalDocCount) {
        this.incrementalDocCount = incrementalDocCount;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

    public String getImage_id() {
        return image_id;
      }
    
    public void setImage_id(String image_id) {
    this.image_id = image_id;
    }
    
    public String getCaseNoteCategory() {
        return caseNoteCategory;
    }

    public void setCaseNoteCategory(String caseNoteCategory) {
        this.caseNoteCategory = caseNoteCategory;
    }
}
