package ccm.models.system.dems;

public class DemsRecordDocumentData {

    private String caseId;
    private String recordId;
    private String data;

    public DemsRecordDocumentData() {
    }

    public DemsRecordDocumentData(String caseId, String recordId, String data) {
        setCaseId(caseId);
        setRecordId(recordId);
        setData(data);
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

}
