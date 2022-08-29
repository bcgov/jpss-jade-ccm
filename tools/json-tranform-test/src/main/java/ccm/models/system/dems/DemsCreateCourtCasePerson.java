package ccm.models.system.dems;

import ccm.models.business.BusinessCourtCaseAccused;

public class DemsCreateCourtCasePerson {
    private String unique_identifier;
    private String full_name;


    public DemsCreateCourtCasePerson(BusinessCourtCaseAccused ba) {
        setUnique_identifier(ba.getIdentifier());
        setFull_name(ba.getFull_name());
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

    
}
