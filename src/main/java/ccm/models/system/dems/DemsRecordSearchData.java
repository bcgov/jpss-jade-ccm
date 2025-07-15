package ccm.models.system.dems;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DemsRecordSearchData {

    private Integer id;
    private String documentID;
    private String parentID;
    private Integer attachmentCount;
    private Boolean isAttachment;
    private String fileExtOrMsgClass;
    private String cc_Source;
    private String cc_SaveVersion;
    private String cc_JUSTINImageID;
    private String cc_OriginalFileNumber;
    // BCPSDEMS-2196 - EDT 11.8 now has the datatype appended to end of custom fields.
    private String cc_Source_Text;
    private String cc_SaveVersion_Boolean;
    private String cc_JUSTINImageID_Text;
    private String cc_OriginalFileNumber_Text;
    private String kind;
    private String isHit;
    private String imageURL;
    private String edtID;

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getDocumentID() {
        return documentID;
    }
    public void setDocumentID(String documentID) {
        this.documentID = documentID;
    }
    public String getParentID() {
        return parentID;
    }
    public void setParentID(String parentID) {
        this.parentID = parentID;
    }
    public Integer getAttachmentCount() {
        return attachmentCount;
    }
    public void setAttachmentCount(Integer attachmentCount) {
        this.attachmentCount = attachmentCount;
    }
    public Boolean getIsAttachment() {
        return isAttachment;
    }
    public void setIsAttachment(Boolean isAttachment) {
        this.isAttachment = isAttachment;
    }
    public String getFileExtOrMsgClass() {
        return fileExtOrMsgClass;
    }
    public void setFileExtOrMsgClass(String fileExtOrMsgClass) {
        this.fileExtOrMsgClass = fileExtOrMsgClass;
    }

    public String getCc_Source() {
        if(cc_Source_Text != null) {
            return cc_Source_Text;
        }
        return cc_Source;
    }
    public void setCc_Source(String cc_Source) {
        this.cc_Source = cc_Source;
    }
    public String getCc_SaveVersion() {
        if(cc_SaveVersion_Boolean != null) {
            return cc_SaveVersion_Boolean;
        }
        return cc_SaveVersion;
    }
    public void setCc_SaveVersion(String cc_SaveVersion) {
        this.cc_SaveVersion = cc_SaveVersion;
    }
    public String getCc_JUSTINImageID() {
        if(cc_JUSTINImageID_Text != null) {
            return cc_JUSTINImageID_Text;
        }
        return cc_JUSTINImageID;
    }
    public void setCc_JUSTINImageID(String cc_JUSTINImageID) {
        this.cc_JUSTINImageID = cc_JUSTINImageID;
    }
    public String getCc_OriginalFileNumber() {
        if(cc_OriginalFileNumber_Text != null) {
            return cc_OriginalFileNumber_Text;
        }
        return cc_OriginalFileNumber;
    }
    public void setCc_OriginalFileNumber(String cc_OriginalFileNumber) {
        this.cc_OriginalFileNumber = cc_OriginalFileNumber;
    }

    public void setCc_Source_Text(String cc_Source_Text) {
        this.cc_Source_Text = cc_Source_Text;
    }
    public void setCc_SaveVersion_Boolean(String cc_SaveVersion_Boolean) {
        this.cc_SaveVersion_Boolean = cc_SaveVersion_Boolean;
    }
    public void setCc_JUSTINImageID_Text(String cc_JUSTINImageID_Text) {
        this.cc_JUSTINImageID_Text = cc_JUSTINImageID_Text;
    }
    public void setCc_OriginalFileNumber_Text(String cc_OriginalFileNumber_Text) {
        this.cc_OriginalFileNumber_Text = cc_OriginalFileNumber_Text;
    }

    public String getKind() {
        return kind;
    }
    public void setKind(String kind) {
        this.kind = kind;
    }
    public String getIsHit() {
        return isHit;
    }
    public void setIsHit(String isHit) {
        this.isHit = isHit;
    }
    public String getImageURL() {
        return imageURL;
    }
    public void setImageURL(String imageURL) {
        this.imageURL = imageURL;
    }
    public String getEdtID() {
        return edtID;
    }
    public void setEdtID(String edtID) {
        this.edtID = edtID;
    }

}
