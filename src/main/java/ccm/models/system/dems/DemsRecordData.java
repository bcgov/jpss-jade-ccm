package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.List;
import ccm.models.common.event.ReportEvent.REPORT_TYPES;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
import ccm.models.common.data.document.CourtCaseDocumentData;
import ccm.models.common.data.document.ImageDocumentData;
import ccm.utils.DateTimeUtils;

public class DemsRecordData {

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
        String docId = descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate;
        setDocumentId(docId.replaceAll(":", ""));

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
        String docId = descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate;
        setDocumentId(docId.replaceAll(":", ""));

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
            descriptionShortForm = report.getLabel();
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
        String docId = descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate;
        setDocumentId(docId.replaceAll(":", ""));

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
        setDocumentId(docId.toString().replaceAll(":", ""));

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


}
