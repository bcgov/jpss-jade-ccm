package ccm.models.system.dems;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DemsCaseGroupMap {
    Map<String,Long> map;

    public DemsCaseGroupMap() {
        map = new HashMap<String,Long>();
    }

    public DemsCaseGroupMap(List<DemsCaseGroupData> groupList) {
        this();
        for (int i = 0; i < groupList.size(); i++) {
            DemsCaseGroupData data = groupList.get(i);
            add(data);
        }
    }

    public Long getIdByName(String name) {
        Long id = (Long)map.get(name);

        return id;
    }

    public void add(DemsCaseGroupData caseGroupData) {
        if (getIdByName(caseGroupData.getName()) == null) {
            map.put(caseGroupData.getName(), caseGroupData.getId());
        }
    }

    public Map<String,Long> getMap() {
        return map;
    }
}
