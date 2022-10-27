package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.common.CommonCourtCaseAccused;
import ccm.models.common.CommonCourtCaseData;
import ccm.utils.DateTimeUtils;

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

    public DemsCourtCaseData(CommonCourtCaseData bcc) {

        StringBuilder case_name = new StringBuilder();
        for (CommonCourtCaseAccused ba : bcc.getAccused_persons()) {
            // Map 87
            if(case_name.length() > 0) {
                case_name.append(SEMICOLON_SPACE_STRING);
            }
            if(ba.getSurname() != null && !ba.getSurname().isEmpty()) {
                // JADE-1470 surnames should be in all uppercase.
                case_name.append(ba.getSurname().toUpperCase());
                case_name.append(COMMA_STRING + " ");
                case_name.append(ba.getGiven_1_name());
                if(ba.getGiven_2_name() != null) {
                    case_name.append(SPACE_STRING);
                    case_name.append(ba.getGiven_2_name());
                }
                if(ba.getGiven_3_name() != null) {
                    case_name.append(SPACE_STRING);
                    case_name.append(ba.getGiven_3_name());
                }
           }
        }
        if(case_name.length() > 251) {
            String truncatedCaseName = case_name.substring(0, 251);
            case_name = new StringBuilder();
            case_name.append(truncatedCaseName);
            case_name.append(" ...");
        }
        setName(case_name.toString());
        setTimeZoneId(PACIFIC_TIMEZONE);
        setKey(bcc.getRcc_id());
        setDescription("");
        setTemplateCase(TEMPLATE_CASE);


        // Map any case flags that exist
        List<String> caseFlagList = new ArrayList<String>();
        for (String caseFlag : bcc.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getName());
            }
        }
        String caseDesionLabel = null;
        if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.name().equals(bcc.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.name().equals(bcc.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.name().equals(bcc.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.name().equals(bcc.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.name().equals(bcc.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.name().equals(bcc.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.getName();
        }


        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData agencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(), bcc.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(), bcc.getAgency_file());
        DemsFieldData submitDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SUBMIT_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(bcc.getRcc_submit_date()));
        DemsFieldData assessmentCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSESSMENT_CROWN.getLabel(), bcc.getAssessment_crown_name());
        
        DemsFieldData caseDecision = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_DECISION.getLabel(), caseDesionLabel);
        DemsFieldData proposedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CHARGES.getLabel(), bcc.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY.getLabel(), bcc.getInitiating_agency());
        DemsFieldData investigatingOfficer = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INVESTIGATING_OFFICER.getLabel(), bcc.getInvestigating_officer());
        
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
        DemsFieldData offenceDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OFFENCE_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(bcc.getEarliest_offence_date()));
        DemsFieldData proposedAppDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_APP_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(bcc.getEarliest_proposed_appearance_date()));
        DemsFieldData proposedProcessType = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getLabel(), bcc.getProposed_process_type_list());
        DemsFieldData limitationDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LIMITATION_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(bcc.getLimitation_date()));
        DemsFieldData rccStatus = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.RCC_STATUS.getLabel(), bcc.getRcc_status_code());
        DemsFieldData proposedCrownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getLabel(), bcc.getProposed_crown_office());

        StringBuilder accused_name_list = new StringBuilder();

        if(bcc.getAccused_persons() != null) {
            for (CommonCourtCaseAccused accused : bcc.getAccused_persons()) {
                // Map 101
                if(accused_name_list.length() > 0) {
                    accused_name_list.append(", ");
                }

                String concatenated_name_string = DemsPersonData.generateFullGivenNamesAndLastNameFromAccused(accused);
                accused_name_list.append(concatenated_name_string);
            }
        }
        DemsFieldData accusedFullName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ACCUSED_FULL_NAME.getLabel(), accused_name_list.toString());

        fieldData.add(agencyFileId);
        fieldData.add(agencyFileNo);
        fieldData.add(submitDate);
        fieldData.add(assessmentCrown);
        fieldData.add(caseDecision);
        fieldData.add(proposedCharges);
        fieldData.add(initiatingAgency);
        fieldData.add(investigatingOfficer);
        fieldData.add(caseFlags);
        fieldData.add(offenceDate);
        fieldData.add(proposedAppDate);
        fieldData.add(proposedProcessType);
        fieldData.add(proposedCrownOffice);
        fieldData.add(limitationDate);
        fieldData.add(accusedFullName);
        fieldData.add(rccStatus);

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
