package ccm.models.system.dems;

public class DemsCaseStatus {
    private String id = "";
    private String key = "";
    private String name = "";
    private String caseState = "";
    private String primaryAgencyFileId = "";
    private String primaryAgencyFileNo = "";
    private String agencyFileId = "";
    private String agencyFileNo = "";
    private String courtFileId = "";
    private String courtFileNo = "";
    private String status = "";
    private String rccStatus = "";

    public DemsCaseStatus() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaseState() {
        return caseState;
    }

    public void setCaseState(String caseState) {
        this.caseState = caseState;
    }

    public String getPrimaryAgencyFileId() {
        return primaryAgencyFileId;
    }

    public void setPrimaryAgencyFileId(String primaryAgencyFileId) {
        this.primaryAgencyFileId = primaryAgencyFileId;
    }

    public String getPrimaryAgencyFileNo() {
        return primaryAgencyFileNo;
    }

    public void setPrimaryAgencyFileNo(String primaryAgencyFileNo) {
        this.primaryAgencyFileNo = primaryAgencyFileNo;
    }

    public String getAgencyFileId() {
        return agencyFileId;
    }

    public void setAgencyFileId(String agencyFileId) {
        this.agencyFileId = agencyFileId;
    }

    public String getAgencyFileNo() {
        return agencyFileNo;
    }

    public void setAgencyFileNo(String agencyFileNo) {
        this.agencyFileNo = agencyFileNo;
    }

    public String getCourtFileId() {
        return courtFileId;
    }

    public void setCourtFileId(String courtFileId) {
        this.courtFileId = courtFileId;
    }

    public String getCourtFileNo() {
        return courtFileNo;
    }

    public void setCourtFileNo(String courtFileNo) {
        this.courtFileNo = courtFileNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRccStatus() {
        return rccStatus;
    }

    public void setRccStatus(String rccStatus) {
        this.rccStatus = rccStatus;
    }

}