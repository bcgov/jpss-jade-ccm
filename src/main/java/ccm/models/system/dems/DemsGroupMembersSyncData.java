package ccm.models.system.dems;

import java.util.List;

public class DemsGroupMembersSyncData {
    private String keyField;
    private List<String> values;

    public DemsGroupMembersSyncData() {
    }

    public DemsGroupMembersSyncData(DemsAuthUsersList d) {
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
  