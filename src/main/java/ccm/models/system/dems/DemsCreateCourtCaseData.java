package ccm.models.system.dems;

import java.util.List;
import java.util.StringJoiner;

import ccm.models.business.BusinessCourtCaseAccused;
import ccm.models.business.BusinessCourtCaseData;

public class DemsCreateCourtCaseData {
    private String rcc_id;
    private String agency_file_no;
    private String security_clearance_level;
    private String synopsis;
    private String initiating_agency;
    private String investigating_officer;
    private String rcc_submit_date;
    private String vul1;
    private String chi1;
    private String crn_decision_agency_identifier;
    private String crn_decision_agency_name;
    
    private String assessment_crown_name;
    private String assessment_crown_part_id;
    private String case_decision_cd;

    private String charge;
    private String limitation_date;
    private String min_offence_date;
    private List<BusinessCourtCaseAccused> accused;

    private List<String> case_flags;

    private String dems_case_name;

    public DemsCreateCourtCaseData(BusinessCourtCaseData bcc) {
        setRcc_id(bcc.getRcc_id());
        setAgency_file_no(bcc.getAgency_file_no());
        setSecurity_clearance_level(bcc.getSecurity_clearance_level());
        setSynopsis(bcc.getSynopsis());
        setInitiating_agency(bcc.getInitiating_agency());
        setInvestigating_officer(bcc.getInvestigating_officer());
        setRcc_submit_date(bcc.getRcc_submit_date());
        setCase_flags(bcc.getCase_flags());
        setCrn_decision_agency_identifier(bcc.getCrn_decision_agency_identifier());
        setCrn_decision_agency_name(bcc.getCrn_decision_agency_name());

        setAssessment_crown_name(bcc.getAssessment_crown_name());
        setCase_decision_cd(bcc.getCase_decision_cd());
        setCharge(bcc.getCharge());
        setLimitation_date(bcc.getLimitation_date());
        setMin_offence_date(bcc.getMin_offence_date());

        setCase_flags(bcc.getCase_flags());

        // determine DEMS court case name
        StringJoiner joiner = new StringJoiner("; ");
        for(BusinessCourtCaseAccused accused: bcc.getAccused()) {
            joiner.add(accused.getFull_name());
        }
        String truncated_case_name = (joiner.toString().length() > 255 ? joiner.toString().substring(0, 255) : joiner.toString());
        setDems_case_name(truncated_case_name);
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
    public String getSecurity_clearance_level() {
        return security_clearance_level;
    }
    public void setSecurity_clearance_level(String security_clearance_level) {
        this.security_clearance_level = security_clearance_level;
    }
    public String getSynopsis() {
        return synopsis;
    }
    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }
    public String getInitiating_agency() {
        return initiating_agency;
    }
    public void setInitiating_agency(String initiating_agency) {
        this.initiating_agency = initiating_agency;
    }
    public String getInvestigating_officer() {
        return investigating_officer;
    }
    public void setInvestigating_officer(String investigating_officer) {
        this.investigating_officer = investigating_officer;
    }
    public String getRcc_submit_date() {
        return rcc_submit_date;
    }
    public void setRcc_submit_date(String rcc_submit_date) {
        this.rcc_submit_date = rcc_submit_date;
    }
    public String getVul1() {
        return vul1;
    }
    public void setVul1(String vul1) {
        this.vul1 = vul1;
    }
    public String getChi1() {
        return chi1;
    }
    public void setChi1(String chi1) {
        this.chi1 = chi1;
    }
    public String getCrn_decision_agency_identifier() {
        return crn_decision_agency_identifier;
    }
    public void setCrn_decision_agency_identifier(String crn_decision_agency_identifier) {
        this.crn_decision_agency_identifier = crn_decision_agency_identifier;
    }
    public String getCrn_decision_agency_name() {
        return crn_decision_agency_name;
    }
    public void setCrn_decision_agency_name(String crn_decision_agency_name) {
        this.crn_decision_agency_name = crn_decision_agency_name;
    }
    public String getAssessment_crown_name() {
        return assessment_crown_name;
    }
    public void setAssessment_crown_name(String assessment_crown_name) {
        this.assessment_crown_name = assessment_crown_name;
    }
    public String getAssessment_crown_part_id() {
        return assessment_crown_part_id;
    }
    public void setAssessment_crown_part_id(String assessment_crown_part_id) {
        this.assessment_crown_part_id = assessment_crown_part_id;
    }
    public String getCase_decision_cd() {
        return case_decision_cd;
    }
    public void setCase_decision_cd(String case_decision_cd) {
        this.case_decision_cd = case_decision_cd;
    }
    public String getCharge() {
        return charge;
    }
    public void setCharge(String charge) {
        this.charge = charge;
    }
    public String getLimitation_date() {
        return limitation_date;
    }
    public void setLimitation_date(String limitation_date) {
        this.limitation_date = limitation_date;
    }
    public String getMin_offence_date() {
        return min_offence_date;
    }
    public void setMin_offence_date(String min_offence_date) {
        this.min_offence_date = min_offence_date;
    }
    public List<BusinessCourtCaseAccused> getAccused() {
        return accused;
    }
    public void setAccused(List<BusinessCourtCaseAccused> accused) {
        this.accused = accused;
    }

    public List<String> getCase_flags() {
        return case_flags;
    }

    public void setCase_flags(List<String> case_flags) {
        this.case_flags = case_flags;
    }

    public String getDems_case_name() {
        return dems_case_name;
    }

    public void setDems_case_name(String dems_case_name) {
        this.dems_case_name = dems_case_name;
    }

    
    
}   
