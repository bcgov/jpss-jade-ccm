package ccm.models.business;

import java.util.List;
import java.util.ArrayList;

import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinAgencyFile;

public class BusinessCourtCaseData {
    private String rcc_id;
    private String agency_file;
    private String security_clearance_level;
    private String synopsis;
    private String initiating_agency;
    private String investigating_officer;
    private String proposed_crown_office;
    private String rcc_submit_date;

    private String assessment_crown_name;
    private String case_status_code;
    private String rcc_status_code;
    private String file_note;

    private String charge;
    private String limitation_date;
    private String earliest_offence_date;

    private String earliest_proposed_appearance_date;
    private String proposed_process_type_list;

    private List<String> case_flags;

    private List<BusinessCourtCaseAccused> accused_persons;

    public BusinessCourtCaseData() {
    }

    public BusinessCourtCaseData(JustinAgencyFile jaf) {
        setRcc_id(jaf.getRcc_id());
        setAgency_file(jaf.getInitiating_agency_identifier() + ":" + jaf.getAgency_file_no());
        setSecurity_clearance_level(jaf.getSecurity_clearance_level());
        setSynopsis(jaf.getSynopsis());
        if(jaf.getInitiating_agency_identifier() != null) {
            setInitiating_agency(jaf.getInitiating_agency_identifier() + ": " + jaf.getInitiating_agency_name());
        }
        if(jaf.getInvestigating_officer_name() != null) {
            setInvestigating_officer(jaf.getInvestigating_officer_name() + " " + jaf.getInvestigating_officer_pin());
        }
        setRcc_submit_date(jaf.getRcc_submit_date());
        if(jaf.getCrn_decision_agency_identifier() != null) {
            String crn_decision_agency_name = jaf.getCrn_decision_agency_name();

            int index_crown_consel = crn_decision_agency_name.indexOf("Crown Counsel");

            // MAP 69
            if (index_crown_consel >= 0) {
                // removing the suffix string
                crn_decision_agency_name = crn_decision_agency_name.substring(0, index_crown_consel);
            }

            setProposed_crown_office(jaf.getCrn_decision_agency_identifier() + ": " + crn_decision_agency_name);
        }

        setAssessment_crown_name(jaf.getAssessment_crown_name());
        setCase_decision_cd(jaf.getCase_decision_cd());
        setCharge(jaf.getCharge());
        setLimitation_date(jaf.getLimitation_date());
        setEarliest_offence_date(jaf.getMin_offence_date());

        case_flags = new ArrayList<String>();

        // Map 69
        if ("Y".equalsIgnoreCase(jaf.getVul1())) {
            case_flags.add("VUL1");
        }
        if ("Y".equalsIgnoreCase(jaf.getChi1())) {
            case_flags.add("CHI1");
        }
        if ("Y".equalsIgnoreCase(jaf.getKfile_yn())) {
            case_flags.add("K");
        }

        // TODO: need definition of intimate partner violence (MAP 74)
        //if ("Y".equalsIgnoreCase(jaf.getIPV1())) { case_flags.add("K"); };

        List<BusinessCourtCaseAccused> accusedList = new ArrayList<BusinessCourtCaseAccused>();
        String earliest_proposed_appearance_date = null;


        StringBuilder proposed_process_type_builder = new StringBuilder();

        if(jaf.getAccused() != null) {
            boolean hasIndigenous = false;
            boolean hasHroip = false;
            boolean hasDoLto = false;

            for (JustinAccused ja : jaf.getAccused()) {

                BusinessCourtCaseAccused accused = new BusinessCourtCaseAccused(ja);
                accusedList.add(accused);

                // Map 73
                if(accused.getName_and_proposed_process_type() != null) {
                    if(proposed_process_type_builder.length() > 0) {
                        proposed_process_type_builder.append("; ");
                    }
                    proposed_process_type_builder.append(accused.getName_and_proposed_process_type());
                }

                // Map 72
                if (earliest_proposed_appearance_date == null) {
                    earliest_proposed_appearance_date = ja.getProposed_appr_date();
                } else if (earliest_proposed_appearance_date != null && ja.getProposed_appr_date() != null && earliest_proposed_appearance_date.compareTo(ja.getProposed_appr_date()) > 0) {
                    earliest_proposed_appearance_date = ja.getProposed_appr_date();
                }

                if (!hasIndigenous && accused.getIndigenous_accused_yn()) {
                    case_flags.add("Indigenous");
                    hasIndigenous = true;
                }

                if (!hasHroip && accused.getHroip_yn()) {
                    case_flags.add("HROIP");
                    hasHroip = true;
                }

                if (!hasDoLto && accused.getDo_lto_yn()) {
                    case_flags.add("DO/LTO");
                    hasDoLto = true;
                }
            }
        }
        setProposed_process_type_list(proposed_process_type_builder.toString());
        setAccused_persons(accusedList);
        setEarliest_proposed_appearance_date(earliest_proposed_appearance_date);

        switch (jaf.getRcc_state_cd()) {
        case "ACT":
            setRcc_status_code("Received");
            break;
        case "CLS":
            setRcc_status_code("Close");
            break;
        case "FIN":
            setRcc_status_code("Finish");
            break;
        case "RET":
            setRcc_status_code("Return");
            break;    
        }
    }

    public String getRcc_id() {
        return rcc_id;
    }
    public void setRcc_id(String rcc_id) {
        this.rcc_id = rcc_id;
    }
    public String getAgency_file() {
        return agency_file;
    }
    public void setAgency_file(String agency_file) {
        this.agency_file = agency_file;
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

    public String getAssessment_crown_name() {
        return assessment_crown_name;
    }
    public void setAssessment_crown_name(String assessment_crown_name) {
        this.assessment_crown_name = assessment_crown_name;
    }
    public String getCase_decision_cd() {
        return case_status_code;
    }
    public void setCase_decision_cd(String case_decision_cd) {
        this.case_status_code = case_decision_cd;
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
    public String getEarliest_offence_date() {
        return earliest_offence_date;
    }
    public void setEarliest_offence_date(String earliest_offence_date) {
        this.earliest_offence_date = earliest_offence_date;
    }

    public String getCase_status_code() {
        return case_status_code;
    }

    public void setCase_status_code(String case_status_code) {
        this.case_status_code = case_status_code;
    }

    public String getFile_note() {
        return file_note;
    }

    public void setFile_note(String file_note) {
        this.file_note = file_note;
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

    public String getProposed_crown_office() {
        return proposed_crown_office;
    }

    public void setProposed_crown_office(String proposed_crown_office) {
        this.proposed_crown_office = proposed_crown_office;
    }

    public String getEarliest_proposed_appearance_date() {
        return earliest_proposed_appearance_date;
    }

    public void setEarliest_proposed_appearance_date(String earliest_proposed_appearance_date) {
        this.earliest_proposed_appearance_date = earliest_proposed_appearance_date;
    }

    public String getProposed_process_type_list() {
        return proposed_process_type_list;
    }

    public void setProposed_process_type_list(String proposed_process_type_list) {
        this.proposed_process_type_list = proposed_process_type_list;
    }

    public List<BusinessCourtCaseAccused> getAccused_persons() {
        return accused_persons;
    }

    public void setAccused_persons(List<BusinessCourtCaseAccused> accused_persons) {
        this.accused_persons = accused_persons;
    }

    public String getRcc_status_code() {
        return rcc_status_code;
    }

    public void setRcc_status_code(String rcc_status_code) {
        this.rcc_status_code = rcc_status_code;
    }


}
