package ccm.models.system.dems;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonString;
import javax.json.JsonNumber;
import javax.json.JsonReader;
import javax.json.JsonObject;
import javax.json.Json;
import javax.json.JsonArray;

public class DemsCaseGroupMap {
    Map<String,Long> map;

    public DemsCaseGroupMap() {
        map = new HashMap<String,Long>();
    }

    public DemsCaseGroupMap(String jsonString) {
        this();

        // https://stackoverflow.com/questions/59528817/split-a-jsonarray
        //
        // alternatively (via Camel Java DSL unmarshal() method),
        // https://www.baeldung.com/java-camel-jackson-json-array

        JsonReader reader = Json.createReader(new StringReader(jsonString));
        JsonArray array = reader.readArray();

        /*
         * Expected example json format
         * [
         *      {
         *          "name": "abc",
         *          "id": 123,
         *          "isUserGroup": true
         *      },
         *      {
         *          "name": "xyz",
         *          "id": 456,
         *          "isUserGroup": true
         *      }
         * ]
         */

        for (int i = 0; i < array.size(); i++) {
            JsonObject o = array.getJsonObject(i);
            String name = o.getJsonString("name").getString();
            Long id = o.getJsonNumber("id").longValue();
            Boolean isUserGroup = o.getBoolean("isUserGroup");
            System.out.println("DEBUG: JsonArray: id of name (" + name + ") = " + id + ". isUserGroup = " + isUserGroup);
            DemsCaseGroupData data = new DemsCaseGroupData();
            data.setId(id);
            data.setName(name);
            data.setIsUserGroup(isUserGroup);
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
