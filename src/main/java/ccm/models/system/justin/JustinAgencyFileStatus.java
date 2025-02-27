package ccm.models.system.justin;

public class JustinAgencyFileStatus {
    private String agencyFileStatus ;
    private String message;
    private String rccId;

    public JustinAgencyFileStatus(String agencyFileStatus, String message, String rccId) {
        this.agencyFileStatus = agencyFileStatus;
        this.message = message;
        this.rccId = rccId;
    }

    public String getAgencyFileStatus() {
        return agencyFileStatus;
    }


    public void setAgencyFileStatus(String agencyFileStatus) {
        this.agencyFileStatus = agencyFileStatus;
    }

    public String getMessage() {
        return message;
    }


    public void setMessage(String message) {
        this.message = message;
    }

    public String getRccId() {
        return rccId;
    }


    public void setRccId(String rccId) {
        this.rccId = rccId;
    }

}
