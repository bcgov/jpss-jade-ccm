package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.common.data.ApprovedCourtCaseData;
import ccm.utils.DateTimeUtils;

public class DemsApprovedCourtCaseData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsApprovedCourtCaseData() {
    }

    public DemsApprovedCourtCaseData(String key, String name, ApprovedCourtCaseData commonData) {
        setKey(key);
        setName(name);

        // Map any case flags that exist
        List<String> caseFlagList = new ArrayList<String>();
        for (String caseFlag : commonData.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getName());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getName());
            }
        }

        DemsFieldData courtFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), commonData.getCourt_file_id());
        DemsFieldData crownElection = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_ELECTION.getLabel(), commonData.getAnticipated_crown_election());
        DemsFieldData courtFileLevel = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_LEVEL.getLabel(), commonData.getCourt_file_level());
        DemsFieldData fileClass = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CLASS.getLabel(), commonData.getCourt_file_class());
        DemsFieldData designation = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.DESIGNATION.getLabel(), commonData.getCourt_file_designation());
        DemsFieldData swornDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SWORN_DATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(commonData.getCourt_file_sworn_date()));
        DemsFieldData approvedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CHARGES.getLabel(), commonData.getOffence_description_list());
        DemsFieldData courtFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), commonData.getCourt_file_number_seq_type());
        DemsFieldData courtFileDetails = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_DETAILS.getLabel(), commonData.getCourt_home_registry_identifier() + ": " + commonData.getCourt_file_number_seq_type());
        DemsFieldData courtHomeReg = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG.getLabel(), commonData.getCourt_home_registry());
        DemsFieldData courtHomeRegName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG_NAME.getLabel(), commonData.getCourt_home_registry_name());
        
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
        //DemsFieldData rmsProcStatus = new FIELD_MAPPINGS.RMS_PROC_STAT.getLabel(), bccm.get());
        //DemsFieldData assignedLegalStaff = new FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), bccm.get());
        DemsFieldData accusedName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ACCUSED_FULL_NAME.getLabel(), commonData.getAccused_names());
        DemsFieldData crownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_OFFICE.getLabel(), commonData.getApproving_crown_agency_name());

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
