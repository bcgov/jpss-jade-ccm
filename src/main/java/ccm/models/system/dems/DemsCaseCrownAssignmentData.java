package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.List;

import ccm.models.common.CommonCaseCrownAssignmentList;

public class DemsCaseCrownAssignmentData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsCaseCrownAssignmentData() {
    }

    public DemsCaseCrownAssignmentData(String key, String name, CommonCaseCrownAssignmentList commonList) {
        setKey(key);
        setName(name);

        DemsFieldData assignedCrown = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSIGNED_CROWN.getLabel(), commonList.getCrownAssignmentList());
        DemsFieldData assignedCrownName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSIGNED_CROWN_NAME.getLabel(), commonList.getCrownAssignmentName());
        DemsFieldData assignedLegalStaff = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), commonList.getLegalStaffAssignmentList());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        fieldData.add(assignedCrownName);
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
