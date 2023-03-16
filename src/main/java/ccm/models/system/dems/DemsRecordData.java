package ccm.models.system.dems;

import ccm.models.common.event.ReportEvent.REPORT_TYPES;
import ccm.models.common.data.document.ChargeAssessmentDocumentData;
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

    public DemsRecordData() {
    }

    public DemsRecordData(ChargeAssessmentDocumentData nrd) {
        // find the report type
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        if(report.equals(REPORT_TYPES.WITNESS_STATEMENT)) {
            //"If $MAPID21 = Y then ""STATEMENT - VICTIM"";
            //ELSE If $MAPID19 = Y then ""STATEMENT - EXPERT"";
            //ELSE If $MAPID20 = Y then ""STATEMENT - POLICE"";
            //ELSE If $MAPID18 = Y then ""STATEMENT - CIVILIAN"""
            if(nrd.getVictim_yn() != null && nrd.getVictim_yn().equalsIgnoreCase("Y")) {
                setDescriptions("STATEMENT - VICTIM");
            } else if(nrd.getExpert_yn() != null && nrd.getExpert_yn().equalsIgnoreCase("Y")) {
                setDescriptions("STATEMENT - EXPERT");
            } else if(nrd.getPolice_officer_yn() != null && nrd.getPolice_officer_yn().equalsIgnoreCase("Y")) {
                setDescriptions("STATEMENT - POLICE");
            } else {
                setDescriptions("STATEMENT - CIVILIAN");
            }
        } else {
            setDescriptions(report.getLabel());
        }

        if(report.equals(REPORT_TYPES.CIPC)) {
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
                setTitle(nrd.getWitness_name() + "(" + nrd.getOfficer_pin_number() + ")");
            } else {
                setTitle(nrd.getWitness_name());
            }

        } else {
            setTitle(nrd.getAgency_file_no());
        }
        setOriginalFileNumber(nrd.getAgency_file_no());


        if(report.equals(REPORT_TYPES.CIPC)) {
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
        }


        setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getSubmit_date()));
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setDocumentId(getDescriptions()+getTitle()+getPrimaryDateUtc());
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



}
