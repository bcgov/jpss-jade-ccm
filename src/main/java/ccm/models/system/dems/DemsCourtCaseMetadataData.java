package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseData;
import ccm.models.business.BusinessCourtCaseMetadataData;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsCourtCaseMetadataData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsCourtCaseMetadataData() {
    }

    public DemsCourtCaseMetadataData(String key, String name, BusinessCourtCaseMetadataData bccm) {
        setKey(key);
        setName(name);

        DemsFieldData courtFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getId(), DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), bccm.getCourt_file_id());
        DemsFieldData crownElection = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_ELECTION.getId(), DemsFieldData.FIELD_MAPPINGS.CROWN_ELECTION.getLabel(), bccm.getAnticipated_crown_election());
        DemsFieldData courtFileLevel = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_LEVEL.getId(), DemsFieldData.FIELD_MAPPINGS.COURT_FILE_LEVEL.getLabel(), bccm.getCourt_file_level());
        DemsFieldData fileClass = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CLASS.getId(), DemsFieldData.FIELD_MAPPINGS.CLASS.getLabel(), bccm.getCourt_file_class());
        DemsFieldData designation = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.DESIGNATION.getId(), DemsFieldData.FIELD_MAPPINGS.DESIGNATION.getLabel(), bccm.getCourt_file_designation());
        DemsFieldData swornDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SWORN_DATE.getId(), DemsFieldData.FIELD_MAPPINGS.SWORN_DATE.getLabel(), bccm.getCourt_file_sworn_date());
        DemsFieldData approvedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.APPROVED_CHARGES.getId(), DemsFieldData.FIELD_MAPPINGS.APPROVED_CHARGES.getLabel(), bccm.getOffence_description_list());
        DemsFieldData courtFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getId(), DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), bccm.getCourt_file_number_seq_type());
        DemsFieldData courtHomeReg = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG.getId(), DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG.getLabel(), bccm.getCourt_home_registry());
        //DemsFieldData rmsProcStatus = new DemsFieldData(FIELD_MAPPINGS.RMS_PROC_STAT.getId(), FIELD_MAPPINGS.RMS_PROC_STAT.getLabel(), bccm.get());
        //DemsFieldData assignedLegalStaff = new DemsFieldData(FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getId(), FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), bccm.get());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        fieldData.add(courtFileId);
        fieldData.add(crownElection);
        fieldData.add(courtFileLevel);
        fieldData.add(fileClass);
        fieldData.add(designation);
        fieldData.add(swornDate);
        fieldData.add(approvedCharges);
        fieldData.add(courtFileNo);
        fieldData.add(courtHomeReg);

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
