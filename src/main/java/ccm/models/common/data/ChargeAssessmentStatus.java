package ccm.models.common.data;

import ccm.models.system.justin.JustinAgencyFileStatus;

public class ChargeAssessmentStatus {
    
    private String initiatingAgency ;
    private String message;
    private String rccId;

    public ChargeAssessmentStatus(JustinAgencyFileStatus justinAgencyFileStatus) {
        if (   justinAgencyFileStatus != null ) {
            setInitiatingAgency(justinAgencyFileStatus.getAgencyFileStatus());
            setRccId(justinAgencyFileStatus.getRccId());
        }
    }
    public ChargeAssessmentStatus() {
        
    }

    public String getInitiatingAgency() {
        return initiatingAgency;
    }
    public void setInitiatingAgency(String initiatingAgency) {
        this.initiatingAgency = initiatingAgency;
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
