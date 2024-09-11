package ccm.models.system.dems;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private boolean waitForCaseCompletion;
    private boolean createdViaUi;

    public DemsChargeAssessmentCaseData() {
    }

    public DemsChargeAssessmentCaseData(String caseTemplateId, ChargeAssessmentData primaryChargeAssessmentData , List<ChargeAssessmentData> chargeAssessmentDataList) 
    {
        createdViaUi = true;
        StringBuilder case_name = new StringBuilder();
        List<CaseAccused> caseAccusedList = new ArrayList<CaseAccused>();

        if (primaryChargeAssessmentData.getAccused_persons() != null) {
            for (CaseAccused caseAccused : primaryChargeAssessmentData.getAccused_persons()) {
            
                if (!caseAccusedList.contains(caseAccused)){
                    caseAccusedList.add(caseAccused);
                }
            }
        }
        if (chargeAssessmentDataList != null && !chargeAssessmentDataList.isEmpty()) {
            
            for (ChargeAssessmentData chargeData : chargeAssessmentDataList) {
                if (chargeData.getAccused_persons() != null) {
                    for (CaseAccused caseAccused :  chargeData.getAccused_persons()) {
                        if (!caseAccusedList.stream().filter(o -> o.getIdentifier().equals(caseAccused.getIdentifier())).findFirst().isPresent()) {
                            caseAccusedList.add(caseAccused);
                        }
                    }
                }      
            }
        }

        for (CaseAccused ba : caseAccusedList) {
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

        List<String> courtCaseDataCaseFlagList = new ArrayList<String>();
        if(chargeAssessmentDataList != null)
        for (ChargeAssessmentData courtcase : chargeAssessmentDataList) {
            for (String caseFlag : courtcase.getCase_flags()) {
                //dataList.add(caseFlag);
                if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel().equals(caseFlag)) {
                    if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel())) {
                        courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
                    }
                } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel().equals(caseFlag)) {
                    if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel())) {
                        courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
                    }
                } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel().equals(caseFlag)) {
                    if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel())) {
                        courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
                    }
                } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel().equals(caseFlag)) {
                    if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel())) {
                        courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel());
                    }
                } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel().equals(caseFlag)) {
                    if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel())) {
                        courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel());
                    }
                } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel().equals(caseFlag)) {
                    if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel())) {
                        courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel());
                    }
                }
            }
            caseFlagList.addAll(courtCaseDataCaseFlagList);
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
        String earliestSubmitDate = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getRcc_submit_date());
        String earliestOffenceDate = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getEarliest_offence_date());
        String propAppearanceDate = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getEarliest_proposed_appearance_date());
        String limitationDateStr = DateTimeUtils.convertToUtcFromBCDateTimeString(primaryChargeAssessmentData.getLimitation_date());
        ZonedDateTime earliestSubmitDateObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(primaryChargeAssessmentData.getRcc_submit_date());
        ZonedDateTime earliestOffenceDateObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(primaryChargeAssessmentData.getEarliest_offence_date());
        ZonedDateTime propAppDateObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(primaryChargeAssessmentData.getEarliest_proposed_appearance_date());
        ZonedDateTime limitationDateObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(primaryChargeAssessmentData.getLimitation_date());

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
        

        Set<String>agencyFileIdSet = new HashSet<>();
        agencyFileIdSet.add(primaryChargeAssessmentData.getRcc_id());
        Set<String>agencyFileNumberSet = new HashSet<>();
        agencyFileNumberSet.add(primaryChargeAssessmentData.getAgency_file());

        Set<String>investigatingOfficerSet = new HashSet<>();
        investigatingOfficerSet.add(primaryChargeAssessmentData.getInvestigating_officer());

        Set<String>proposedProcessTypeSet = new HashSet<>();
        proposedProcessTypeSet.add(primaryChargeAssessmentData.getProposed_process_type_list());
        if (chargeAssessmentDataList != null && !chargeAssessmentDataList.isEmpty()){
            for(ChargeAssessmentData data : chargeAssessmentDataList) {
                if (data.getRcc_submit_date() != null && !data.getRcc_submit_date().isEmpty()) {
                    ZonedDateTime currentSubmitDateObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(data.getRcc_submit_date());
                    if (currentSubmitDateObj != null && earliestSubmitDateObj != null) {
                        if (currentSubmitDateObj.isBefore(earliestSubmitDateObj)) {
                            earliestSubmitDate = DateTimeUtils.convertToUtcFromZonedDateTime(currentSubmitDateObj);
                        }
                    }
                }
                if (data.getEarliest_offence_date() != null && !data.getEarliest_offence_date().isEmpty()){
                    ZonedDateTime currentOffenceDate = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(data.getEarliest_offence_date());
                    if (currentOffenceDate != null && earliestOffenceDateObj != null) {
                        if (currentOffenceDate.isBefore(earliestOffenceDateObj)) {
                            earliestOffenceDate = DateTimeUtils.convertToUtcFromZonedDateTime(currentOffenceDate);
                        }
                    }
                }
                if (data.getEarliest_proposed_appearance_date() != null && !data.getEarliest_proposed_appearance_date().isEmpty()){
                    ZonedDateTime currentPropAppDate = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(data.getEarliest_proposed_appearance_date());
                    if (currentPropAppDate != null && propAppDateObj != null) {
                        if (currentPropAppDate.isBefore(propAppDateObj)) {
                            propAppearanceDate = DateTimeUtils.convertToUtcFromZonedDateTime(currentPropAppDate);
                        }
                    }
                }
                 if (data.getLimitation_date() != null && !data.getLimitation_date().isEmpty()){
                    ZonedDateTime currentLimDate = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(data.getLimitation_date());
                    if (currentLimDate != null && limitationDateObj != null) {
                        if (currentLimDate.isBefore(limitationDateObj)){
                            limitationDateStr = DateTimeUtils.convertToUtcFromZonedDateTime(currentLimDate);
                        }
                    }
                }
                /*merge
                Assessment Crown (agency file)
                Initiating Agency Name (agency file)
                Proposed Crown Office (agency file)
                Initiating Agency (BCPSDEMS-1086) */
                if (data.getAssessment_crown_name() != null && !assessmentCrownSet.contains(data.getAssessment_crown_name())){
                    assessmentCrownSet.add(data.getAssessment_crown_name());
                }
               if (data.getInitiating_agency_name() != null && !initiatingAgencyNameSet.contains(data.getInitiating_agency_name())){
                initiatingAgencyNameSet.add(data.getInitiating_agency_name());
               }
               if (data.getProposed_crown_office() != null && !proposedCrownOfficeSet.contains(data.getProposed_crown_office())) {
                    proposedCrownOfficeSet.add(data.getProposed_crown_office());
               }
               if (data.getInitiating_agency() != null && !initiatingAgencySet.contains(data.getInitiating_agency())) {
                initiatingAgencySet.add(data.getInitiating_agency());
               }
               if (data.getRcc_id() != null && !agencyFileIdSet.contains(data.getRcc_id())) {
                    agencyFileIdSet.add(data.getRcc_id());
               }
               if (data.getAgency_file() != null && !agencyFileNumberSet.contains(data.getAgency_file())){
                agencyFileNumberSet.add(data.getAgency_file());
               }
               if (data.getInvestigating_officer() != null && !investigatingOfficerSet.contains(data.getInvestigating_officer())){
                investigatingOfficerSet.add(data.getInvestigating_officer());
               }
               if (data.getProposed_process_type_list() != null && !proposedProcessTypeSet.contains(data.getProposed_process_type_list())){
                proposedProcessTypeSet.add(data.getProposed_process_type_list());
               }
            }
        }
        assessmentCrownList.addAll(assessmentCrownSet);
        initiatingAgencyNameList.addAll(initiatingAgencyNameSet);
        proposedCrownOfficeList.addAll(proposedCrownOfficeSet);
        initiatingAgencyList.addAll(initiatingAgencySet);
        
        DemsFieldData submitDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SUBMIT_DATE.getLabel(), earliestSubmitDate);
        DemsFieldData assessmentCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSESSMENT_CROWN.getLabel(), assessmentCrownList);
        
        DemsFieldData caseDecision = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_DECISION.getLabel(), caseDesionLabel);
        DemsFieldData proposedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CHARGES.getLabel(), primaryChargeAssessmentData.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY.getLabel(),initiatingAgencyList);
        DemsFieldData initiatingAgencyName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIATING_AGENCY_NAME.getLabel(), initiatingAgencyNameList);
        
        
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
      

        DemsFieldData offenceDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OFFENCE_DATE.getLabel(), earliestOffenceDate);
        DemsFieldData proposedAppDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_APP_DATE.getLabel(), propAppearanceDate);
        
        DemsFieldData limitationDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LIMITATION_DATE.getLabel(), limitationDateStr);
        DemsFieldData rccStatus = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.RCC_STATUS.getLabel(), primaryChargeAssessmentData.getRcc_status_code());
        DemsFieldData proposedCrownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getLabel(), proposedCrownOfficeList);
        DemsFieldData lastJustinUpdate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LAST_JUSTIN_UPDATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        // added as part of JADE-2954
        DemsFieldData primaryAgencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_ID.getLabel(), primaryChargeAssessmentData.getRcc_id());
        DemsFieldData primaryAgencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_NO.getLabel(), primaryChargeAssessmentData.getAgency_file());
        StringBuilder accused_name_list = new StringBuilder();

        /*if(primaryChargeAssessmentData.getAccused_persons() != null) {
            for (CaseAccused accused : primaryChargeAssessmentData.getAccused_persons()) {
                // Map 101
                if(accused_name_list.length() > 0) {
                    accused_name_list.append(", ");
                }

                String concatenated_name_string = DemsPersonData.generateFullGivenNamesAndLastNameFromAccused(accused);
                accused_name_list.append(concatenated_name_string);
            }
        }*/
        for (CaseAccused accused : caseAccusedList) {
                // Map 101
                if(accused_name_list.length() > 0) {
                    accused_name_list.append(", ");
                }

                String concatenated_name_string = DemsPersonData.generateFullGivenNamesAndLastNameFromAccused(accused);
                accused_name_list.append(concatenated_name_string);
        }
        DemsFieldData accusedFullName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ACCUSED_FULL_NAME.getLabel(), accused_name_list.toString());

        // JADE-2954
        StringBuffer distinctAgencyFileIdBuffer = new StringBuffer("");
        StringBuilder agencyFileNumberBuilder = new StringBuilder("");
        StringBuilder investigatingOfficerBuilder = new StringBuilder("");
        StringBuilder proposedProcessTypeBuilder = new StringBuilder("");
        
        if (!agencyFileIdSet.isEmpty()) {
            int elementCounter =0;
            for (String element : agencyFileIdSet) {
                if (elementCounter > 0) {
                    distinctAgencyFileIdBuffer.append(";");
                }
                distinctAgencyFileIdBuffer.append(element);    
                elementCounter++;
            }
        }
        if (!agencyFileNumberSet.isEmpty()) {
            int elementCounter =0;
            for (String element : agencyFileNumberSet) {
                if (elementCounter > 0) {
                    agencyFileNumberBuilder.append(";");
                }
                agencyFileNumberBuilder.append(element);    
                elementCounter++;
            }
        }
        if (!investigatingOfficerSet.isEmpty()) {
            int elementCounter =0;
            for (String element : investigatingOfficerSet) {
                if (elementCounter > 0) {
                    investigatingOfficerBuilder.append(";");
                }
                investigatingOfficerBuilder.append(element);    
                elementCounter++;
            }
        }
          if (!proposedProcessTypeSet.isEmpty()) {
            int elementCounter =0;
            for (String element : proposedProcessTypeSet) {
                if (elementCounter > 0) {
                    proposedProcessTypeBuilder.append(";");
                }
                proposedProcessTypeBuilder.append(element);    
                elementCounter++;
            }
        }
        DemsFieldData agencyFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(), distinctAgencyFileIdBuffer.toString());
        DemsFieldData agencyFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(), agencyFileNumberBuilder.toString());
        DemsFieldData investigatingOfficer = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INVESTIGATING_OFFICER.getLabel(), investigatingOfficerBuilder.toString());
        DemsFieldData proposedProcessType = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getLabel(), proposedProcessTypeBuilder.toString());

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
       
        fieldData.add(primaryAgencyFileId);
        fieldData.add(primaryAgencyFileNo);
        fieldData.add(lastJustinUpdate);

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

    public boolean isWaitForCaseCompletion() {
        return waitForCaseCompletion;
    }

    public void setWaitForCaseCompletion(boolean waitForCaseCompletion) {
        this.waitForCaseCompletion = waitForCaseCompletion;
    }

    public boolean isCreatedViaUi() {
        return createdViaUi;
    }

    public void setCreatedViaUi(boolean createdViaUi) {
        this.createdViaUi = createdViaUi;
    }

}
