package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseData;
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
        if(accused_names.length() > 251) {
            String truncatedCaseName = accused_names.substring(0, 251);
            accused_names = new StringBuilder();
            accused_names.append(truncatedCaseName);
            accused_names.append(" ...");
        }
        setName(accused_names.toString());
        setTimeZoneId(PACIFIC_TIMEZONE);
        setKey(bcc.getRcc_id());
        setDescription("");
        setTemplateCase(TEMPLATE_CASE);


        // Map any case flags that exist
        List<DemsListItemFieldData> caseFlagList = new ArrayList<DemsListItemFieldData>();
        for (String caseFlag : bcc.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getName()));
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getName()));
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getName()));
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getName()));
            }
        }
        DemsListItemFieldData caseDecisionValue = null;
        if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.getName());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.getName());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.getName());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.getName());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.getName());
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.getName());
        }


        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData agencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(), bcc.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(), bcc.getAgency_file_no());
        DemsFieldData submitDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SUBMIT_DATE.getLabel(), bcc.getRcc_submit_date());
        DemsFieldData assessmentCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSESSMENT_CROWN.getLabel(), bcc.getAssessment_crown_name());
        DemsFieldData caseDecision = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_DECISION.getLabel(), caseDecisionValue);
        DemsFieldData proposedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CHARGES.getLabel(), bcc.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY.getLabel(), bcc.getInitiating_agency());
        DemsFieldData investigatingOfficer = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INVESTIGATING_OFFICER.getLabel(), bcc.getInvestigating_officer());
        DemsFieldData proposedCrownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getLabel(), bcc.getProposed_crown_office());
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
        DemsFieldData offenceDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OFFENCE_DATE.getLabel(), bcc.getEarliest_offence_date());
        DemsFieldData proposedAppDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_APP_DATE.getLabel(), bcc.getEarliest_proposed_appearance_date());
        DemsFieldData proposedProcessType = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getLabel(), bcc.getProposed_process_type_list());
        DemsFieldData limitationDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LIMITATION_DATE.getLabel(), bcc.getLimitation_date());
        DemsFieldData accusedName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ACCUSED_FULL_NAME.getLabel(), bcc.getAccused_names());

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
        fieldData.add(limitationDate);
        fieldData.add(accusedName);
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
