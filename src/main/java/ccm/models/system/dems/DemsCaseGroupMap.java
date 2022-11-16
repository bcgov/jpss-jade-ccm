package ccm.models.system.dems;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DemsCaseGroupMap {
    Map<String,Long> map;

    public DemsCaseGroupMap() {
        map = new HashMap<String,Long>();
    }

    public DemsCaseGroupMap(Object demsCaseGroupJson) {
        this();

        /*
            Example expected DEMS case group json structure
            [
                {id=0, name=Legal Assistant, isUserGroup=true}
                {id=1, name=Submitting Agency, isUserGroup=true}
                {id=3, name=Paralegal, isUserGroup=true}
                {id=4, name=System Support, isUserGroup=true}
                {id=7, name=Lawyer, isUserGroup=true}
                {id=8, name=Administrator, isUserGroup=true}
            ]
        */

        try {
            @SuppressWarnings("unchecked")
            List<LinkedHashMap<String,Object>> demsCaseGroupLinkedHashMap = (List<LinkedHashMap<String,Object>>)demsCaseGroupJson;

            if (demsCaseGroupLinkedHashMap == null || demsCaseGroupLinkedHashMap.isEmpty()) {
                // null or empty list; do nothing.
                return;
            }
    
            for (LinkedHashMap<String,Object> element : demsCaseGroupLinkedHashMap) {
                //DemsCaseGroupData caseGroup = new DemsCaseGroupData();
    
                System.out.println("element (" + element.getClass() + ") = '" + element.toString() + "'");
                System.out.println("element.keySet = '" + element.keySet().toString() + "'");
                System.out.println("element.values = '" + element.values().toString() + "'");

                map.put((String)element.get("name"), (Long)element.get("id"));
            }
        } catch (ClassCastException e) {
            // conversion error; do nothing.
        }
    }

    public Long getIdByName(String name) {
        Long id = (Long)map.get(name);

        return id;
    }
}
