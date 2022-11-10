package ccm.models.common.data;

import ccm.models.system.justin.JustinAgencyFileRef;

public class ChargeAssessmentCaseDataRef {
    private String rcc_id;
    private String agency_file_no;
    private Boolean primary_rcc_yn;

    public ChargeAssessmentCaseDataRef() {
    }

    public ChargeAssessmentCaseDataRef(JustinAgencyFileRef jafr) {
        setRcc_id(jafr.getRcc_id());
        setAgency_file_no(jafr.getAgency_file_no());
    }

    public String getRcc_id() {
        return rcc_id;
    }
    public void setRcc_id(String rcc_id) {
        this.rcc_id = rcc_id;
    }
    public String getAgency_file_no() {
        return agency_file_no;
    }
    public void setAgency_file_no(String agency_file_no) {
        this.agency_file_no = agency_file_no;
    }

    public Boolean getPrimary_rcc_yn() {
        return primary_rcc_yn;
    }

    public void setPrimary_rcc_yn(Boolean primary_rcc_yn) {
        this.primary_rcc_yn = primary_rcc_yn;
    }
}
