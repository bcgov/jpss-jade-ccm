package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.List;

import ccm.models.business.BusinessCourtCaseCrownAssignmentList;

public class DemsCourtCaseCrownAssignmentData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsCourtCaseCrownAssignmentData() {
    }

    public DemsCourtCaseCrownAssignmentData(String key, String name, BusinessCourtCaseCrownAssignmentList bccca) {
        setKey(key);
        setName(name);

        DemsFieldData assignedCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSIGNED_CROWN_NAME.getId(), DemsFieldData.FIELD_MAPPINGS.ASSIGNED_CROWN_NAME.getLabel(), bccca.getCrownAssignmentList());
        DemsFieldData assignedLegalStaff = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getId(), DemsFieldData.FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), bccca.getLegalStaffAssignmentList());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        fieldData.add(assignedCrown);
        fieldData.add(assignedLegalStaff);
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
