package ccm.models.common.data;

import ccm.models.system.dems.DemsCaseRef;
import ccm.models.system.justin.JustinAgencyFileRef;

public class ChargeAssessmentDataRef {
    private String rcc_id;
    private String agency_file_no;
    private String primary_yn;

    public ChargeAssessmentDataRef() {
    }

    public ChargeAssessmentDataRef(JustinAgencyFileRef jafr) {
        setRcc_id(jafr.getRcc_id());
        setAgency_file_no(jafr.getAgency_file_no());
        setPrimary_yn(jafr.getPrimary_yn());
    }

    public ChargeAssessmentDataRef(DemsCaseRef demsCaseRef) {
        setRcc_id(demsCaseRef.getKey());
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

    public String getPrimary_yn() {
        return primary_yn;
    }

    public void setPrimary_yn(String primary_yn) {
        this.primary_yn = primary_yn;
    }
}
