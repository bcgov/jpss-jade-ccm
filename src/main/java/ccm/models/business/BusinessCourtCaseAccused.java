package ccm.models.business;

import ccm.models.system.justin.JustinAccused;

public class BusinessCourtCaseAccused {
    public static final String COMMA_STRING = ",";

    private String identifier;
    private String full_name;
    private String first_name;
    private String surname;
    private String proposed_process_type;
    private String proposed_appearance_date;
    private String crown_decision_code;
    private String offence_date;
    private Boolean indigenous_accused_yn;
    private String name_and_proposed_process_type;
    private String birth_date;

    public BusinessCourtCaseAccused() {
    }

    public BusinessCourtCaseAccused(JustinAccused ja) {
        setIdentifier(ja.getPart_id());
        setFull_name(ja.getAccused_name());
        setProposed_process_type(ja.getProposed_process_type());
        setProposed_appearance_date(ja.getProposed_appr_date());
        setCrown_decision_code(ja.getCrown_decision());
        setOffence_date(ja.getOffence_date());
        setBirth_date(ja.getBirth_date());

        if(ja.getAccused_name() != null && !ja.getAccused_name().isEmpty()) {
            String[] names = ja.getAccused_name().split(COMMA_STRING, 2);
            if(names.length > 1) {
                setSurname(names[0]);
                setFirst_name(names[1]);
            } else {
                // in case there's no "surname, given name" set-up, just assume the entire name is a surname.
                setSurname(ja.getAccused_name());
            }
        }

        // Map 78
        if ("Y" == ja.getIndigenous_yn()) {
            setIndigenous_accused_yn(true);
       } else {
            setIndigenous_accused_yn(false);
       }

        StringBuilder name_process = new StringBuilder();
        if(ja.getProposed_process_type() != null) {
            name_process.append(ja.getProposed_process_type());
        }
        if(ja.getAccused_name() != null) {
            name_process.append(" ");
            name_process.append(ja.getAccused_name());
        }
        setName_and_proposed_process_type(name_process.toString());
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String first_name) {
        this.first_name = first_name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getProposed_process_type() {
        return proposed_process_type;
    }

    public void setProposed_process_type(String proposed_process_type) {
        this.proposed_process_type = proposed_process_type;
    }

    public String getProposed_appearance_date() {
        return proposed_appearance_date;
    }

    public void setProposed_appearance_date(String proposed_appearance_date) {
        this.proposed_appearance_date = proposed_appearance_date;
    }

    public String getCrown_decision_code() {
        return crown_decision_code;
    }

    public void setCrown_decision_code(String crown_decision_code) {
        this.crown_decision_code = crown_decision_code;
    }

    public String getOffence_date() {
        return offence_date;
    }

    public void setOffence_date(String offence_date) {
        this.offence_date = offence_date;
    }

    public Boolean getIndigenous_accused_yn() {
        return indigenous_accused_yn;
    }

    public void setIndigenous_accused_yn(Boolean indigenous_accused_yn) {
        this.indigenous_accused_yn = indigenous_accused_yn;
    }

    public String getName_and_proposed_process_type() {
        return name_and_proposed_process_type;
    }

    public void setName_and_proposed_process_type(String name_and_proposed_process_type) {
        this.name_and_proposed_process_type = name_and_proposed_process_type;
    }

    public String getBirth_date() {
        return birth_date;
    }

    public void setBirth_date(String birth_date) {
        this.birth_date = birth_date;
    }

}
