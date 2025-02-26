package ccm.models.common.data;

import ccm.models.system.justin.JustinAgencyFileStatus;

public class ChargeAssessmentStatus {
    
    private String agencyFileStatus ;
    private String message;
    private String rccId;

    public ChargeAssessmentStatus(JustinAgencyFileStatus justinAgencyFileStatus) {
        if (   justinAgencyFileStatus != null ) {
        this.agencyFileStatus = justinAgencyFileStatus.getAgencyFileStatus();
        this.message = justinAgencyFileStatus.getMessage();
        this.rccId = justinAgencyFileStatus.getRccId();
        }
    }
    public ChargeAssessmentStatus() {
        
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
