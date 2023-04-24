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

    public DemsRecordData() {
    }

    public DemsRecordData(ChargeAssessmentDocumentData nrd) {
        // find the report type
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        setDescriptions(nrd.getReport_type());
        String descriptionShortForm = getDescriptions();
        if(report != null) {
            if(report.equals(REPORT_TYPES.WITNESS_STATEMENT)) {
                //"If $MAPID21 = Y then ""STATEMENT - VICTIM"";
                //ELSE If $MAPID19 = Y then ""STATEMENT - EXPERT"";
                //ELSE If $MAPID20 = Y then ""STATEMENT - POLICE"";
                //ELSE If $MAPID18 = Y then ""STATEMENT - CIVILIAN"""
                if(nrd.getVictim_yn() != null && nrd.getVictim_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-VIC";
                } else if(nrd.getExpert_yn() != null && nrd.getExpert_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-EXP";
                } else if(nrd.getPolice_officer_yn() != null && nrd.getPolice_officer_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-POL";
                } else if(nrd.getWitness_yn() != null && nrd.getWitness_yn().equalsIgnoreCase("Y")) {
                    descriptionShortForm = report.getLabel()+"-WIT";
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
                    setTitle(nrd.getWitness_name() + "(" + nrd.getOfficer_pin_number() + ")");
                } else {
                    setTitle(nrd.getWitness_name());
                }

            } else {
                setTitle(nrd.getAgency_file_no());
            }
            setOriginalFileNumber(nrd.getAgency_file_no());


            if(report.equals(REPORT_TYPES.CPIC)) {
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

        }

        setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getSubmit_date()));
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        String shortendStartDate = DateTimeUtils.shortDateTimeString(nrd.getSubmit_date());
        setDocumentId(descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate);

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
        if(getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", nrd.getLocation());
            fieldData.add(location);
        }
        /*if(getFolder() != null) {
            DemsFieldData folder = new DemsFieldData("Folder", getFolder());
            fieldData.add(folder);
        }*/

        setFields(fieldData);
    }

    public DemsRecordData(CourtCaseDocumentData nrd) {
        // find the report type
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        setDescriptions(nrd.getReport_type());
        String descriptionShortForm = getDescriptions();
        if(report != null) {
            if(report.equals(REPORT_TYPES.CONVICTION_LIST) && nrd.getFiltered_yn() != null && nrd.getFiltered_yn() == "Y") {
                descriptionShortForm = report.getLabel()+"-F";
            } else if(report.equals(REPORT_TYPES.CONVICTION_LIST)) {
                descriptionShortForm = report.getLabel()+"-D";
            } else {
                descriptionShortForm = report.getLabel();
            }

            if(report.equals(REPORT_TYPES.RECORD_OF_PROCEEDINGS) && nrd.getCourt_file_no() != null) {
                //$MAPID67 $MAPID69
                setTitle(nrd.getParticipant_name().toUpperCase() + nrd.getCourt_file_no());
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

        setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getGeneration_date()));
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        String shortendStartDate = DateTimeUtils.shortDateTimeString(nrd.getGeneration_date());
        setDocumentId(descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate);

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
        if(getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", nrd.getLocation());
            fieldData.add(location);
        }
        /*if(getFolder() != null) {
            DemsFieldData folder = new DemsFieldData("Folder", getFolder());
            fieldData.add(folder);
        }*/

        setFields(fieldData);
    }

    public DemsRecordData(ImageDocumentData nrd) {
        // find the report type
        REPORT_TYPES report = REPORT_TYPES.valueOf(nrd.getReport_type().toUpperCase());
        setDescriptions(nrd.getReport_type());
        String descriptionShortForm = getDescriptions();
        if(report != null) {
            if(nrd.getForm_type_description() != null) {
                descriptionShortForm = nrd.getForm_type_description().toUpperCase();

                if(report.equals(REPORT_TYPES.INFORMATION)) {
                    setType("COURT RECORD");
                } else if(report.equals(REPORT_TYPES.DOCUMENT)) {
                    if(nrd.getForm_type_description().contains("Release")) {
                        setType("STATEMENT");
                    } else {
                        setType("COURT RECORD");
                    }
                }
            }

            if(report.equals(REPORT_TYPES.INFORMATION)) {
                setStartDate(nrd.getSworn_date());
            } else {
                setStartDate(nrd.getIssue_date());
            }
        }

        if(report.equals(REPORT_TYPES.INFORMATION) && nrd.getCourt_file_number() != null) {
            //$MAPID67 $MAPID69
            setTitle(nrd.getCourt_file_number());
        } else {
            setTitle(nrd.getParticipant_name() + " " + nrd.getCourt_file_number());
        }

        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setLastApiRecordUpdate(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        String shortendStartDate = DateTimeUtils.shortDateTimeString(getStartDate());
        setDocumentId(descriptionShortForm+"_"+getTitle()+"_"+shortendStartDate);

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
        if(getLocation() != null) {
            DemsFieldData location = new DemsFieldData("ISL Event", nrd.getLocation());
            fieldData.add(location);
        }
        /*if(getFolder() != null) {
            DemsFieldData folder = new DemsFieldData("Folder", getFolder());
            fieldData.add(folder);
        }*/

        setFields(fieldData);
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

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }


}
