package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseData;

public class DemsCreateCourtCaseData {
	public static final String PACIFIC_TIMEZONE = "Pacific Standard Time";

    private String name;
    private String key;
    private String description;
    private String timeZoneId;
    //private String templateCase;
    private List<DemsFieldData> fields;

    public enum FIELD_MAPPINGS {
        AGENCY_FILE_ID(12),
        AGENCY_FILE_NO(3),
        SUBMIT_DATE(13),
        ASSESSMENT_CROWN(9),
        CASE_DECISION(14),
        PROPOSED_CHARGES(18),
        INITIATING_AGENCY(24),
        INVESTIGATING_OFFICER(25),
        PROPOSED_CROWN_OFFICE(26),
        CASE_FLAGS(28),
        OFFENCE_DATE(29),
        PROPOSED_APP_DATE(30),
        PROPOSED_PROCESS_TYPE(31);

		private int id;
		
		private FIELD_MAPPINGS(int id) {
			this.id = id;
		}
		
        public int getId() {
            return id;
        }
    }

    public DemsCreateCourtCaseData() {
    }

    public DemsCreateCourtCaseData(BusinessCourtCaseData bcc) {
        setKey(bcc.getRcc_id());
        setDescription(bcc.getDems_case_name());
        setName(bcc.getDems_case_name());
        setTimeZoneId(PACIFIC_TIMEZONE);
        //setKey(key);
        //setDescription(description);
        //setTemplateCase(templateCase);
        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData agencyFileId = new DemsFieldData(FIELD_MAPPINGS.AGENCY_FILE_ID.getId(), bcc.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData(FIELD_MAPPINGS.AGENCY_FILE_NO.getId(), bcc.getAgency_file_no());
        DemsFieldData submitDate = new DemsFieldData(FIELD_MAPPINGS.SUBMIT_DATE.getId(), bcc.getRcc_submit_date());
        DemsFieldData assessmentCrown = new DemsFieldData(FIELD_MAPPINGS.ASSESSMENT_CROWN.getId(), bcc.getAssessment_crown_name());
        DemsFieldData caseDecision = new DemsFieldData(FIELD_MAPPINGS.CASE_DECISION.getId(), bcc.getCase_decision_cd());
        DemsFieldData proposedCharges = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_CHARGES.getId(), bcc.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(FIELD_MAPPINGS.INITIATING_AGENCY.getId(), bcc.getInitiating_agency());
        DemsFieldData investigatingOfficer = new DemsFieldData(FIELD_MAPPINGS.INVESTIGATING_OFFICER.getId(), bcc.getInvestigating_officer());
        DemsFieldData proposedCrownOffice = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getId(), bcc.getProposed_crown_office());
        DemsFieldData caseFlags = new DemsFieldData(FIELD_MAPPINGS.CASE_FLAGS.getId(), bcc.getCase_flags());
        DemsFieldData offenceDate = new DemsFieldData(FIELD_MAPPINGS.OFFENCE_DATE.getId(), bcc.getEarliest_offence_date());
        DemsFieldData proposedAppDate = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_APP_DATE.getId(), bcc.getEarliest_proposed_appearance_date());
        DemsFieldData proposedProcessType = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getId(), bcc.getProposed_process_type_list());

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
        setFields(fieldData);

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

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    // public String getTemplateCase() {
    //     return templateCase;
    // }

    // public void setTemplateCase(String templateCase) {
    //     this.templateCase = templateCase;
    // }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

}   
