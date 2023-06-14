package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.common.data.CourtCaseData;
import ccm.utils.DateTimeUtils;

public class DemsApprovedCourtCaseData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsApprovedCourtCaseData() {
    }

    public DemsApprovedCourtCaseData(String key, String name, CourtCaseData commonData, List<String> existingCaseFlags) {
        setKey(key);
        setName(name);

        // Map any case flags that exist
        List<String> caseFlagList = new ArrayList<String>();
        for (String caseFlag : commonData.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getLabel());
            }
        }
        // Note: intentionally left-out the K-file flag check in following code, as the metadata overrides that value.
        for(String caseFlag : existingCaseFlags) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel())) {
                  caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel());
                }
            }
        }
        List<String> courtFileLevelList = new ArrayList<String>();
        courtFileLevelList.add(commonData.getCourt_file_level());
        List<String> fileClassList = new ArrayList<String>();
        fileClassList.add(commonData.getCourt_file_class());
        List<String> designationList = new ArrayList<String>();
        designationList.add(commonData.getCourt_file_designation());
        List<String> courtHomeRegList = new ArrayList<String>();
        courtHomeRegList.add(commonData.getCourt_home_registry());
        List<String> courtHomeRegNameList = new ArrayList<String>();
        courtHomeRegNameList.add(commonData.getCourt_home_registry_name());
        List<String> crownElectionList = new ArrayList<String>();
        crownElectionList.add(commonData.getAnticipated_crown_election());
        List<String> crownOfficeList = new ArrayList<String>();
        crownOfficeList.add(commonData.getApproving_crown_agency_name());

        DemsFieldData courtFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), commonData.getCourt_file_id());
        DemsFieldData crownElection = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_ELECTION.getLabel(), crownElectionList);
        DemsFieldData courtFileLevel = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_LEVEL.getLabel(), courtFileLevelList);
        DemsFieldData fileClass = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CLASS.getLabel(), fileClassList);
        DemsFieldData designation = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.DESIGNATION.getLabel(), designationList);
        DemsFieldData swornDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SWORN_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(commonData.getCourt_file_sworn_date()));
        DemsFieldData approvedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CHARGES.getLabel(), commonData.getOffence_description_list());
        DemsFieldData courtFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), commonData.getCourt_file_number_seq_type());
        DemsFieldData courtFileDetails = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_DETAILS.getLabel(), commonData.getCourt_home_registry_identifier() + ": " + commonData.getCourt_file_number_seq_type());
        DemsFieldData courtHomeReg = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG.getLabel(), courtHomeRegList);
        DemsFieldData courtHomeRegName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG_NAME.getLabel(), courtHomeRegNameList);
        
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
        //DemsFieldData rmsProcStatus = new FIELD_MAPPINGS.RMS_PROC_STAT.getLabel(), bccm.get());
        //DemsFieldData assignedLegalStaff = new FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), bccm.get());
        DemsFieldData accusedName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ACCUSED_FULL_NAME.getLabel(), commonData.getAccused_names());
        DemsFieldData crownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_OFFICE.getLabel(), crownOfficeList);
        DemsFieldData lastJustinUpdate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LAST_JUSTIN_UPDATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        fieldData.add(courtFileId);
        fieldData.add(crownElection);
        fieldData.add(courtFileLevel);
        fieldData.add(fileClass);
        fieldData.add(designation);
        fieldData.add(swornDate);
        fieldData.add(approvedCharges);
        fieldData.add(courtFileNo);
        fieldData.add(courtFileDetails);
        fieldData.add(courtHomeReg);
        fieldData.add(courtHomeRegName);
        fieldData.add(caseFlags);
        fieldData.add(accusedName);
        fieldData.add(crownOffice);
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

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

}
