package ccm.models.system.dems;

import ccm.models.common.data.document.NarrativeDocumentData;
import ccm.utils.DateTimeUtils;

public class DemsRecordData {

    private String description;
    private String title;
    private String startDate;
    private String dateToCrown;
    private String source;
    private String custodian;
    private String location;
    private String folder;
    private String documentId;
    private String fileExtension;
    private String primaryDateUtc;

    public DemsRecordData() {
    }

    public DemsRecordData(NarrativeDocumentData nrd) {
        setDescription(nrd.getReport_type());
        setTitle(nrd.getAgency_file_no());
        setStartDate(DateTimeUtils.convertToUtcFromBCDateTimeString(nrd.getSubmit_date()));
        setDateToCrown(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        setSource("JUSTIN");
        setFolder("JUSTIN");
        //setLocation(nrd.getLocation());
        setFileExtension(".pdf");
        setPrimaryDateUtc(DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateToCrown() {
        return dateToCrown;
    }

    public void setDateToCrown(String dateToCrown) {
        this.dateToCrown = dateToCrown;
    }



}
