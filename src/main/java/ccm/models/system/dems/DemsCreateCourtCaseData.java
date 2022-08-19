package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import ccm.models.business.BusinessCourtCaseAccused;
import ccm.models.business.BusinessCourtCaseData;

public class DemsCreateCourtCaseData {
    private String description;
    private String timeZone;
    private String templateCase;
    private List<DemsDataField> fields;

    //private List<String> case_flags;

    private String name;

    public DemsCreateCourtCaseData() {
    }

    public DemsCreateCourtCaseData(BusinessCourtCaseData bcc) {
        fields = new ArrayList<DemsDataField>();

        setTimeZone("Pacific Standard Time");
        setTemplateCase("28");
        setName(bcc.getRcc_id());
        addField("Agency File ID", bcc.getRcc_id());
        addField("Agency File No.", bcc.getAgency_file_no());


        //addField("Approved Charges","");
        //addField("Assessment Crown","105: Kelowna Municipal RCMP");
        //addField("Assigned Crown","Rhodes, Christopher 1001");
        //addField("Assigned Legal Staff","");
        List<String> cd = new ArrayList<String>();
        cd.add("ACT");
        addField("Case Decision",cd);
        List<String> cf = new ArrayList<String>();
        cf.add("VUL1");
        addField("Case Fields",cf);
        //addField("Class", "");
        addField("Court File Level","9");
        //addField("Court File Unique ID","");
        //addField("Court Home Registry","");

        // setSecurity_clearance_level(bcc.getSecurity_clearance_level());
        // setSynopsis(bcc.getSynopsis());
        // setInitiating_agency(bcc.getInitiating_agency());
        // setInvestigating_officer(bcc.getInvestigating_officer());
        // setRcc_submit_date(bcc.getRcc_submit_date());
        // setCase_flags(bcc.getCase_flags());
        // setCrn_decision_agency_identifier(bcc.getCrn_decision_agency_identifier());
        // setCrn_decision_agency_name(bcc.getCrn_decision_agency_name());

        // setAssessment_crown_name(bcc.getAssessment_crown_name());
        // setCase_decision_cd(bcc.getCase_decision_cd());
        // setCharge(bcc.getCharge());
        // setLimitation_date(bcc.getLimitation_date());
        // setMin_offence_date(bcc.getMin_offence_date());

        //setCase_flags(bcc.getCase_flags());

        // determine DEMS court case name
        StringJoiner joiner = new StringJoiner("; ");
        for(BusinessCourtCaseAccused accused: bcc.getAccused()) {
            joiner.add(accused.getFull_name());
        }
        String truncated_case_name = (joiner.toString().length() > 255 ? joiner.toString().substring(0, 255) : joiner.toString());
        setName(truncated_case_name);
        setDescription(truncated_case_name);
    }

    public void addField(String fieldId, Object fieldValue) {
        DemsDataField df = new DemsDataField();

        df.setId(fieldId);
        df.setValue(fieldValue);

        fields.add(df);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    // public List<String> getCase_flags() {
    //     return case_flags;
    // }

    // public void setCase_flags(List<String> case_flags) {
    //     this.case_flags = case_flags;
    // }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplateCase() {
        return templateCase;
    }

    public void setTemplateCase(String templateCase) {
        this.templateCase = templateCase;
    }

    public List<DemsDataField> getFields() {
        return fields;
    }

    public void setFields(List<DemsDataField> fields) {
        this.fields = fields;
    }

    
    
}   
