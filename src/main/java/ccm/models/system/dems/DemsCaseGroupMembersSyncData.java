package ccm.models.system.dems;

import java.util.List;

public class DemsCaseGroupMembersSyncData {
    private String keyField;
    private List<String> values;

    public DemsCaseGroupMembersSyncData() {
    }

    public DemsCaseGroupMembersSyncData(DemsAuthUsersList d) {
      setKeyField("key");
      setValues(d.getUserKeys());
    }

    public String getKeyField() {
      return keyField;
    }

    public void setKeyField(String keyField) {
      this.keyField = keyField;
    }

    public List<String> getValues() {
      return values;
    }

    public void setValues(List<String> values) {
      this.values = values;
    }
  }
  