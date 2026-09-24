package ccm.models.common.data;

import ccm.models.system.justin.JustinAgencyFileStatus;

public class ChargeAssessmentStatus {
    
    private String chargeAssessmentStatus;
    private String message;
    private String rccId;

    public ChargeAssessmentStatus(JustinAgencyFileStatus justinAgencyFileStatus) {
        if (   justinAgencyFileStatus != null ) {
            setChargeAssessmentStatus(justinAgencyFileStatus.getAgencyFileStatus());
            setRccId(justinAgencyFileStatus.getRccId());
        }
    }
    public ChargeAssessmentStatus() {
        
    }

    public String getChargeAssessmentStatus() {
        return chargeAssessmentStatus;
    }
    public void setChargeAssessmentStatus(String chargeAssessmentStatus) {
        this.chargeAssessmentStatus = chargeAssessmentStatus;
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
