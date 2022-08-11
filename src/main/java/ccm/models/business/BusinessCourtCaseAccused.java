package ccm.models.business;

import ccm.models.system.justin.JustinAccused;

public class BusinessCourtCaseAccused {
    private String unique_identifier;
    private String full_name;
    private String proposed_process_type_id;

    public BusinessCourtCaseAccused(JustinAccused ja) {
        setUnique_identifier(ja.getPart_id());
        setFull_name(ja.getAccused_name());
    }

    public String getUnique_identifier() {
        return unique_identifier;
    }

    public void setUnique_identifier(String unique_identifier) {
        this.unique_identifier = unique_identifier;
    }

    public String getFull_name() {
        return full_name;
    }

    public void setFull_name(String full_name) {
        this.full_name = full_name;
    }

    public String getProposed_process_type_id() {
        return proposed_process_type_id;
    }

    public void setProposed_process_type_id(String proposed_process_type_id) {
        this.proposed_process_type_id = proposed_process_type_id;
    }

    
}
