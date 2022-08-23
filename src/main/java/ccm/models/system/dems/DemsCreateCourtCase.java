package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseAccused;
import ccm.models.business.BusinessCourtCaseData;

public class DemsCreateCourtCase {
    private String name;
    private String key;
    private String description;
    private String timeZone;
    private String templateCase;
    private List<DemsFieldData> fields;

    private List<DemsCreateCourtCasePerson> people;


    public DemsCreateCourtCase(BusinessCourtCaseData bcc) {
        setName(bcc.getDems_case_name());
        //setKey(key);
        //setDescription(description);
        //setTimeZone(timeZone);
        //setTemplateCase(templateCase);
        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData agencyFileId = new DemsFieldData("Agency File ID", bcc.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData("Agency File No.", bcc.getAgency_file_no());
        DemsFieldData submitDate = new DemsFieldData("Submit Date", bcc.getRcc_submit_date());
        DemsFieldData assessmentCrown = new DemsFieldData("Assessment Crown", bcc.getAssessment_crown_name());
        DemsFieldData caseDecision = new DemsFieldData("Case Decision", bcc.getCase_decision_cd());
        DemsFieldData proposedCharges = new DemsFieldData("Proposed Charges", bcc.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData("Initiating Agency", bcc.getInitiating_agency());
        DemsFieldData investigatingOfficer = new DemsFieldData("Investigating Officer", bcc.getInvestigating_officer());
        DemsFieldData proposedCrownOffice = new DemsFieldData("Proposed Crown Office", bcc.getProposed_crown_office());
        DemsFieldData caseFlags = new DemsFieldData("Case Flags", bcc.getCase_flags());
        DemsFieldData offenceDate = new DemsFieldData("Offence Date (earliest)", bcc.getEarliest_offence_date());
        DemsFieldData proposedAppDate = new DemsFieldData("Proposed App. Date (earliest)", bcc.getEarliest_proposed_appearance_date());
        DemsFieldData proposedProcessType = new DemsFieldData("Proposed Process Type", bcc.getProposed_process_type_list());

        fieldData.add(agencyFileId);
        fieldData.add(agencyFileNo);
        fieldData.add(submitDate);
        fieldData.add(assessmentCrown);
        fieldData.add(caseDecision);
        fieldData.add(proposedCharges);
        fieldData.add(initiatingAgency);
        fieldData.add(investigatingOfficer);
        fieldData.add(proposedCrownOffice);
        fieldData.add(caseFlags);
        fieldData.add(offenceDate);
        fieldData.add(proposedAppDate);
        fieldData.add(proposedProcessType);
        setFields(fields);

        List<DemsCreateCourtCasePerson> personList = new ArrayList<DemsCreateCourtCasePerson>();

        for (BusinessCourtCaseAccused ba : bcc.getAccused_person()) {

            DemsCreateCourtCasePerson person = new DemsCreateCourtCasePerson(ba);
            personList.add(person);

        }
        setPeople(personList);

    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public String getTemplateCase() {
        return templateCase;
    }

    public void setTemplateCase(String templateCase) {
        this.templateCase = templateCase;
    }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

    public List<DemsCreateCourtCasePerson> getPeople() {
        return people;
    }
    public void setPeople(List<DemsCreateCourtCasePerson> people) {
        this.people = people;
    }

}   
