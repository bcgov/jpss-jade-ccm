package ccm.models.business;

import java.util.List;
import java.util.ArrayList;

import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinAgencyFile;

public class BusinessCourtCaseData {
    private String rcc_id;
    private String agency_file_no;
    private String security_clearance_level;
    private String synopsis;
    private String initiating_agency;
    private String investigating_officer;
    private String rcc_submit_date;
    private String crn_decision_agency_identifier;
    private String crn_decision_agency_name;
    
    private String assessment_crown_name;
    private String case_decision_cd;

    private String charge;
    private String limitation_date;
    private String min_offence_date;
    private List<BusinessCourtCaseAccused> accused;

    private String earliest_proposed_appearance_date;

    private List<String> case_flags;

    public BusinessCourtCaseData() {
    }

    public BusinessCourtCaseData(JustinAgencyFile jaf) {
        setRcc_id(jaf.getRcc_id());
        setAgency_file_no(jaf.getAgency_file_no());
        setSecurity_clearance_level(jaf.getSecurity_clearance_level());
        setSynopsis(jaf.getSynopsis());
        setInitiating_agency(jaf.getInitiating_agency_identifier() + ": " + jaf.getInitiating_agency_name());
        setInvestigating_officer(jaf.getInvestigating_officer_name() + " " + jaf.getInvestigating_officer_pin());
        setRcc_submit_date(jaf.getRcc_submit_date());
        setCrn_decision_agency_identifier(jaf.getCrn_decision_agency_identifier());
        setCrn_decision_agency_name(jaf.getCrn_decision_agency_name());

        setAssessment_crown_name(jaf.getAssessment_crown_name());
        setCase_decision_cd(jaf.getCase_decision_cd());
        setCharge(jaf.getCharge());
        setLimitation_date(jaf.getLimitation_date());
        setMin_offence_date(jaf.getMin_offence_date());

        case_flags = new ArrayList<String>();

        if ("Y" == jaf.getVul1()) { case_flags.add("VUL1"); };
        if ("Y" == jaf.getChi1()) { case_flags.add("CHI1"); };
        if ("Y" == jaf.getVul1()) { case_flags.add("VUL1"); };

        List<BusinessCourtCaseAccused> accusedList = new ArrayList<BusinessCourtCaseAccused>();
        String earliest_proposed_appearance_date = jaf.getAccused().get(0).getProposed_appr_date();    

        // TODO MAPID 71
        String proposed_process_type_list;

        for (JustinAccused ja: jaf.getAccused()) {

            BusinessCourtCaseAccused accused = new BusinessCourtCaseAccused(ja);
            accusedList.add(accused);

            if (earliest_proposed_appearance_date.compareTo(ja.getProposed_appr_date()) > 0) {
                earliest_proposed_appearance_date = ja.getProposed_appr_date();
            }
        }
        setAccused(accusedList);
        setEarliest_proposed_appearance_date(earliest_proposed_appearance_date);
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
    public String getRcc_submit_date() {
        return rcc_submit_date;
    }
    public void setRcc_submit_date(String rcc_submit_date) {
        this.rcc_submit_date = rcc_submit_date;
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

    public String getEarliest_proposed_appearance_date() {
        return earliest_proposed_appearance_date;
    }

    public void setEarliest_proposed_appearance_date(String earliest_proposed_appearance_date) {
        this.earliest_proposed_appearance_date = earliest_proposed_appearance_date;
    }

    
    
}   
