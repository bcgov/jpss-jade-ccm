package ccm.models.system.dems;

import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.HashSet;

import ccm.models.common.data.CaseAccused;
import ccm.models.common.data.ChargeAssessmentData;
import ccm.utils.DateTimeUtils;

public class DemsChargeAssessmentCaseData {
    public static final String PACIFIC_TIMEZONE = "Pacific Standard Time";
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";
    public static final String SPACE_STRING = " ";

    private String name;
    private String key;
    private String description;
    private String timeZoneId;
    private String templateCase;
    private List<DemsFieldData> fields;

    public DemsChargeAssessmentCaseData() {
    }

    public DemsChargeAssessmentCaseData(String caseTemplateId, ChargeAssessmentData primaryChargeAssessmentData , List<ChargeAssessmentData> chargeAssessmentDataList) 
    {

        StringBuilder case_name = new StringBuilder();
        for (CaseAccused ba : primaryChargeAssessmentData.getAccused_persons()) {
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
        setKey(primaryChargeAssessmentData.getRcc_id());
        setDescription("");
        setTemplateCase(caseTemplateId);


        // Map any case flags that exist
        List<String> caseFlagList = new ArrayList<String>();
        for (String caseFlag : primaryChargeAssessmentData.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getLabel().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel());
                //fix for JADE-2559
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel().equals(caseFlag)){
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel());
            } else {
                System.out.println("DEBUG: Unknown case flag - '" + caseFlag + "'");
                System.out.println("DEBUG: DO_LTO.getName() - '" + DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel() + "'");
                System.out.println("DEBUG: DO_LTO.name() - '" + DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.name() + "'");
                System.out.println("DEBUG: DO_LTO - '" + DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO + "'");
            }
        }
        String caseDesionLabel = null;
        if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.name().equals(primaryChargeAssessmentData.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ADV.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.name().equals(primaryChargeAssessmentData.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACT.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.name().equals(primaryChargeAssessmentData.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.RET.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.name().equals(primaryChargeAssessmentData.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.ACL.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.name().equals(primaryChargeAssessmentData.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.NAC.getName();
        } else if(DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.name().equals(primaryChargeAssessmentData.getCase_decision_cd())) {
            caseDesionLabel = DemsListItemFieldData.CASE_DECISION_FIELD_MAPPINGS.REF.getName();
        }


        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

      
        // added as part of JADE-2594
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        DemsFieldData agencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(), primaryChargeAssessmentData.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(), primaryChargeAssessmentData.getAgency_file());
        String earliestSubmitDate = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getRcc_submit_date());
        String earliestOffenceDate = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getEarliest_offence_date());
        String propAppearanceDate = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getEarliest_proposed_appearance_date());
        String limitationDateStr = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getLimitation_date());
        java.util.Date earliestOffenceDateObj = null;
        java.util.Date propAppDateObj = null;
        java.util.Date limitationDateObj = null;
        java.util.Date earliestSubmitDateObj = null;

        try {
            earliestSubmitDateObj = dateFormat.parse(earliestSubmitDate);
            earliestOffenceDateObj = dateFormat.parse(earliestOffenceDate);
            propAppDateObj = dateFormat.parse(propAppearanceDate);
            limitationDateObj = dateFormat.parse(limitationDateStr);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        List<String> assessmentCrownList = new ArrayList<String>();
        Set<String> assessmentCrownSet = new HashSet<>();
        assessmentCrownSet.add(primaryChargeAssessmentData.getAssessment_crown_name());
        
        List<String> initiatingAgencyNameList = new ArrayList<String>();
        Set<String> initiatingAgencyNameSet = new HashSet<>();
        initiatingAgencyNameSet.add(primaryChargeAssessmentData.getInitiating_agency_name());

        //initiatingAgencyNameList.add(primaryChargeAssessmentData.getInitiating_agency_name());
        List<String> proposedCrownOfficeList = new ArrayList<String>();
        Set<String> proposedCrownOfficeSet = new HashSet<>();
        proposedCrownOfficeSet.add(primaryChargeAssessmentData.getProposed_crown_office());
        //proposedCrownOfficeList.add(primaryChargeAssessmentData.getProposed_crown_office());
        List<String> initiatingAgencyList = new ArrayList<String>();
        Set<String> initiatingAgencySet = new HashSet<>();
        initiatingAgencySet.add(primaryChargeAssessmentData.getInitiating_agency());
        //initiatingAgencyList.add(primaryChargeAssessmentData.getInitiating_agency());

        if (chargeAssessmentDataList != null && !chargeAssessmentDataList.isEmpty()){
            ListIterator<ChargeAssessmentData> chargeAssessmentDateIter = (ListIterator<ChargeAssessmentData>) chargeAssessmentDataList.iterator();
            while(chargeAssessmentDateIter.hasNext()) {
                ChargeAssessmentData data = chargeAssessmentDateIter.next();
                if (!data.getRcc_submit_date().isEmpty()) {
                    
                    java.util.Date currentSubmitDateObj = null;
                    try {
                        currentSubmitDateObj = dateFormat.parse(data.getRcc_submit_date());
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (currentSubmitDateObj != null && earliestSubmitDateObj != null) {
                        if (currentSubmitDateObj.before(earliestSubmitDateObj)){
                            earliestSubmitDate = DateTimeUtils.convertToUtcFromBCDateTimeString(dateFormat.format(currentSubmitDateObj));
                        }
                    }
                }
                if (!data.getEarliest_offence_date().isEmpty()){
                    java.util.Date currentOffenceDate = null;
                    try {
                        currentOffenceDate = dateFormat.parse(data.getEarliest_offence_date());
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (currentOffenceDate.before(earliestOffenceDateObj)){
                        earliestOffenceDate = DateTimeUtils.convertToUtcFromBCDateTimeString(dateFormat.format(currentOffenceDate));
                    }
                }
                if (!data.getEarliest_proposed_appearance_date().isEmpty()){
                     java.util.Date currentPropAppDate = null;
                    try {
                        currentPropAppDate = dateFormat.parse(data.getEarliest_proposed_appearance_date());
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (currentPropAppDate.before(propAppDateObj)){
                        propAppearanceDate = DateTimeUtils.convertToUtcFromBCDateTimeString(dateFormat.format(currentPropAppDate));
                    }
                }
                 if (!data.getLimitation_date().isEmpty()){
                     java.util.Date currentLimDate = null;
                    try {
                        currentLimDate = dateFormat.parse(data.getLimitation_date());
                    } catch (ParseException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    if (currentLimDate.before(limitationDateObj)){
                        limitationDateStr = DateTimeUtils.convertToUtcFromBCDateTimeString(currentLimDate.toString());
                    }
                }
                /*merge
                Assessment Crown (agency file)
                Initiating Agency Name (agency file)
                Proposed Crown Office (agency file)
                Initiating Agency (BCPSDEMS-1086) */
                if (!assessmentCrownSet.contains(data.getAssessment_crown_name())){
                    assessmentCrownSet.add(data.getAssessment_crown_name());
                }
               if (!initiatingAgencyNameSet.contains(data.getInitiating_agency_name())){
                initiatingAgencyNameSet.add(data.getInitiating_agency_name());
               }
               if (!proposedCrownOfficeSet.contains(data.getProposed_crown_office())) {
                    proposedCrownOfficeSet.add(data.getProposed_crown_office());
               }
               if (!initiatingAgencySet.contains(data.getInitiating_agency())) {
                initiatingAgencySet.add(data.getInitiating_agency());
               }
            }
        }
        assessmentCrownList.addAll(assessmentCrownSet);
        initiatingAgencyNameList.addAll(initiatingAgencyNameSet);
        proposedCrownOfficeList.addAll(proposedCrownOfficeSet);
        initiatingAgencyList.addAll(initiatingAgencySet);
        
        DemsFieldData submitDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SUBMIT_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(earliestSubmitDate));
        DemsFieldData assessmentCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSESSMENT_CROWN.getLabel(), assessmentCrownList);
        
        DemsFieldData caseDecision = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_DECISION.getLabel(), caseDesionLabel);
        DemsFieldData proposedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CHARGES.getLabel(), primaryChargeAssessmentData.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY.getLabel(),initiatingAgencyList);
        DemsFieldData initiatingAgencyName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY_NAME.getLabel(), initiatingAgencyNameList);
        DemsFieldData investigatingOfficer = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INVESTIGATING_OFFICER.getLabel(), primaryChargeAssessmentData.getInvestigating_officer());
        
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
      

        DemsFieldData offenceDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OFFENCE_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(earliestOffenceDate));
        DemsFieldData proposedAppDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_APP_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(propAppearanceDate));
        DemsFieldData proposedProcessType = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getLabel(), primaryChargeAssessmentData.getProposed_process_type_list());
        DemsFieldData limitationDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LIMITATION_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(limitationDateStr));
        DemsFieldData rccStatus = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.RCC_STATUS.getLabel(), primaryChargeAssessmentData.getRcc_status_code());
        DemsFieldData proposedCrownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getLabel(), proposedCrownOfficeList);
        DemsFieldData lastJustinUpdate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LAST_JUSTIN_UPDATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        // added as part of JADE-2954
        DemsFieldData primaryAgencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_ID.getLabel(), primaryChargeAssessmentData.getRcc_id());
        DemsFieldData primaryAgencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_NO.getLabel(), primaryChargeAssessmentData.getAgency_file());
        StringBuilder accused_name_list = new StringBuilder();

        if(primaryChargeAssessmentData.getAccused_persons() != null) {
            for (CaseAccused accused : primaryChargeAssessmentData.getAccused_persons()) {
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
        fieldData.add(initiatingAgencyName);
        fieldData.add(investigatingOfficer);
        fieldData.add(caseFlags);
        fieldData.add(offenceDate);
        fieldData.add(proposedAppDate);
        fieldData.add(proposedProcessType);
        fieldData.add(proposedCrownOffice);
        fieldData.add(limitationDate);
        fieldData.add(accusedFullName);
        fieldData.add(rccStatus);
        fieldData.add(lastJustinUpdate);
        fieldData.add(primaryAgencyFileId);
        fieldData.add(primaryAgencyFileNo);

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
