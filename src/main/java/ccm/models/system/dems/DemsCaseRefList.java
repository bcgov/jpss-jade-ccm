package ccm.models.system.dems;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.json.JsonObject;
import javax.json.Json;
import javax.json.JsonArray;

public class DemsCaseRefList {
    List<DemsCaseRef> list;

    public DemsCaseRefList() {
        list = new ArrayList<DemsCaseRef>();
    }

    public DemsCaseRefList(String jsonString) {
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
         *          "id": 123,
         *          "key": "23432.3345"
         *      },
         *      {
         *          "id": 456,
         *          "key": "55434."1123:
         *      }
         * ]
         */

        for (int i = 0; i < array.size(); i++) {
            JsonObject o = array.getJsonObject(i);
            //jade -2157 java.lang.ClassCastException: class javax.json.JsonValueImpl cannot be cast to class javax.json.JsonString

            String key = null;
            String key2 = null;
            String key3 = null;
            JsonValue v = o.get("key");
            if (v == JsonValue.NULL) {
                key = null;
            } else {
                key = o.getString("key");
                key2 = o.getString("key");
                key3 = o.getJsonString("key").getString();
            }
            System.out.println("o = '" + o.toString() + "'");
            System.out.println("key2 = '" + key2 + "'");
            System.out.println("key3 = '" + key3 + "'");
            Long id = o.getJsonNumber("id").longValue();
            //System.out.println("DEBUG: JsonArray: id of key (" + key + ") = " + id + ".");
            DemsCaseRef data = new DemsCaseRef();
            data.setId(id);
            data.setKey(key);
            list.add(data);
        }
    }

    public List<DemsCaseRef> getList() {
        return list;
    }

    public void setList(List<DemsCaseRef> list) {
        this.list = list;
    }
}
