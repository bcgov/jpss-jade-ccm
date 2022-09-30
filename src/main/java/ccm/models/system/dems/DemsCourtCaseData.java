package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseData;
import ccm.models.business.BusinessCourtCaseMetadataData;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsCourtCaseData {
    public static final String PACIFIC_TIMEZONE = "Pacific Standard Time";
    public static final String TEMPLATE_CASE = "28";
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";
    public static final String SPACE_STRING = " ";

    private String name;
    private String key;
    private String description;
    private String timeZoneId;
    private String templateCase;
    private List<DemsFieldData> fields;

    public DemsCourtCaseData() {
    }

    public DemsCourtCaseData(BusinessCourtCaseData bcc) {

        StringBuilder accused_names = new StringBuilder();
        for (BusinessCourtCaseAccused ba : bcc.getAccused_person()) {
            // Map 87
            if(accused_names.length() > 0) {
                accused_names.append(SEMICOLON_SPACE_STRING);
            }
            if(ba.getSurname() != null && !ba.getSurname().isEmpty()) {
                // JADE-1470 surnames should be in all uppercase.
                accused_names.append(ba.getSurname().toUpperCase());
                accused_names.append(COMMA_STRING);
                accused_names.append(ba.getGiven_1_name());
                if(ba.getGiven_2_name() != null) {
                    accused_names.append(SPACE_STRING);
                    accused_names.append(ba.getGiven_2_name());
                }
                if(ba.getGiven_3_name() != null) {
                    accused_names.append(SPACE_STRING);
                    accused_names.append(ba.getGiven_3_name());
                }
           }
        }
        setName(accused_names.substring(0, accused_names.length() > 255 ? 254 : accused_names.length()));
        setTimeZoneId(PACIFIC_TIMEZONE);
        setKey(bcc.getRcc_id());
        setDescription("");
        setTemplateCase(TEMPLATE_CASE);


        // Map any case flags that exist
        List<DemsListItemFieldData> caseFlagList = new ArrayList<DemsListItemFieldData>();
        for (String caseFlag : bcc.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getId()));
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getId()));
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getId()));
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getId()));
            }
        }
        DemsListItemFieldData caseDecisionValue = null;
        if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.getId());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.getId());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.getId());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.getId());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.getId());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.getId());
        }


        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData agencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getId(), DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(), bcc.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getId(), DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(), bcc.getAgency_file_no());
        DemsFieldData submitDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SUBMIT_DATE.getId(), DemsFieldData.FIELD_MAPPINGS.SUBMIT_DATE.getLabel(), bcc.getRcc_submit_date());
        DemsFieldData assessmentCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSESSMENT_CROWN.getId(), DemsFieldData.FIELD_MAPPINGS.ASSESSMENT_CROWN.getLabel(), bcc.getAssessment_crown_name());
        DemsFieldData caseDecision = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_DECISION.getId(), DemsFieldData.FIELD_MAPPINGS.CASE_DECISION.getLabel(), caseDecisionValue);
        DemsFieldData proposedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CHARGES.getId(), DemsFieldData.FIELD_MAPPINGS.PROPOSED_CHARGES.getLabel(), bcc.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY.getId(), DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY.getLabel(), bcc.getInitiating_agency());
        DemsFieldData investigatingOfficer = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INVESTIGATING_OFFICER.getId(), DemsFieldData.FIELD_MAPPINGS.INVESTIGATING_OFFICER.getLabel(), bcc.getInvestigating_officer());
        DemsFieldData proposedCrownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getId(), DemsFieldData.FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getLabel(), bcc.getProposed_crown_office());
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getId(), DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
        DemsFieldData offenceDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OFFENCE_DATE.getId(), DemsFieldData.FIELD_MAPPINGS.OFFENCE_DATE.getLabel(), bcc.getEarliest_offence_date());
        DemsFieldData proposedAppDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_APP_DATE.getId(), DemsFieldData.FIELD_MAPPINGS.PROPOSED_APP_DATE.getLabel(), bcc.getEarliest_proposed_appearance_date());
        DemsFieldData proposedProcessType = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getId(), DemsFieldData.FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getLabel(), bcc.getProposed_process_type_list());

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

}
