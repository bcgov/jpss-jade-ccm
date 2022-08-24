package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseAccused;
import ccm.models.business.BusinessCourtCaseData;

public class DemsCreateCourtCaseData {
    private String agency_file_id;
    private String agency_file_no;
    private String submit_date;
    private String assessment_crown;
    private String case_decision;
    private String proposed_charges;
    private String initiating_agency;
    private String investigating_officer;
    private String proposed_crown_office;

    private String limitation_date;
    private String offence_date;
    private List<DemsCreateCourtCasePerson> people;

    private List<String> case_flags;
    private String proposed_app_date;
    private String proposed_process_type;
    private String name;

    public DemsCreateCourtCaseData(BusinessCourtCaseData bcc) {
        setAgency_file_id(bcc.getRcc_id());
        setAgency_file_no(bcc.getAgency_file_no());
        setSubmit_date(bcc.getRcc_submit_date());

        setAssessment_crown(bcc.getAssessment_crown_name());
        setCase_decision(bcc.getCase_decision_cd());
        setProposed_charges(bcc.getCharge());

        setInitiating_agency(bcc.getInitiating_agency());
        setInvestigating_officer(bcc.getInvestigating_officer());
        setProposed_crown_office(bcc.getProposed_crown_office());
        setLimitation_date(bcc.getLimitation_date());
        setOffence_date(bcc.getEarliest_offence_date());
        setCase_flags(bcc.getCase_flags());

        setProposed_app_date(bcc.getEarliest_proposed_appearance_date());
        setProposed_process_type(bcc.getProposed_process_type_list());
        setName(bcc.getDems_case_name());

        List<DemsCreateCourtCasePerson> personList = new ArrayList<DemsCreateCourtCasePerson>();

        for (BusinessCourtCaseAccused ba : bcc.getAccused_person()) {

            DemsCreateCourtCasePerson person = new DemsCreateCourtCasePerson(ba);
            personList.add(person);

        }
        setPeople(personList);

    }

    public String getAgency_file_id() {
        return agency_file_id;
    }
    public void setAgency_file_id(String agency_file_id) {
        this.agency_file_id = agency_file_id;
    }
    public String getAgency_file_no() {
        return agency_file_no;
    }
    public void setAgency_file_no(String agency_file_no) {
        this.agency_file_no = agency_file_no;
    }
    public String getSubmit_date() {
        return submit_date;
    }
    public void setSubmit_date(String rcc_submit_date) {
        this.submit_date = rcc_submit_date;
    }
    public String getAssessment_crown() {
        return assessment_crown;
    }

    public void setAssessment_crown(String assessment_crown) {
        this.assessment_crown = assessment_crown;
    }
    public String getCase_decision() {
        return case_decision;
    }
    public void setCase_decision(String case_decision) {
        this.case_decision = case_decision;
    }
    public String getProposed_charges() {
        return proposed_charges;
    }

    public void setProposed_charges(String proposed_charges) {
        this.proposed_charges = proposed_charges;
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

    public String getProposed_crown_office() {
        return proposed_crown_office;
    }
    public void setProposed_crown_office(String proposed_crown_office) {
        this.proposed_crown_office = proposed_crown_office;
    }
    public List<String> getCase_flags() {
        return case_flags;
    }

    public void setCase_flags(List<String> case_flags) {
        this.case_flags = case_flags;
    }
    public String getOffence_date() {
        return offence_date;
    }
    public void setOffence_date(String offence_date) {
        this.offence_date = offence_date;
    }

    public String getProposed_app_date() {
        return proposed_app_date;
    }

    public void setProposed_app_date(String proposed_app_date) {
        this.proposed_app_date = proposed_app_date;
    }

    public String getProposed_process_type() {
        return proposed_process_type;
    }

    public void setProposed_process_type(String proposed_process_type) {
        this.proposed_process_type = proposed_process_type;
    }
    public String getLimitation_date() {
        return limitation_date;
    }
    public void setLimitation_date(String limitation_date) {
        this.limitation_date = limitation_date;
    }
    public List<DemsCreateCourtCasePerson> getPeople() {
        return people;
    }
    public void setPeople(List<DemsCreateCourtCasePerson> people) {
        this.people = people;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}   
