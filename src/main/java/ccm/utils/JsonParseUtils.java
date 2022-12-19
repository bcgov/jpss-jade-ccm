package ccm.utils;

import java.util.Iterator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParseUtils {

  static public String getJsonArrayElementValue(String jsonString, String arrayPath, String key, String keyValue, String valueKey) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      Iterator<JsonNode> fieldsIterator = mapper.readTree(jsonString).at(arrayPath).iterator();
      while (fieldsIterator.hasNext()) {
        JsonNode node = fieldsIterator.next();
        String fieldName = node.at(key).asText("");
        if(fieldName.equalsIgnoreCase(keyValue)) {
          return node.at(valueKey).asText("");
        }
      }
    } catch(Exception ex) {
      // issue converting json response to json. ignore.
    }
    return "";// if not found, then just return blank string.
  }

  static public JsonNode getJsonArrayElement(String jsonString, String arrayPath, String key, String keyValue, String valueKey) {
    ObjectMapper mapper = new ObjectMapper();
    //System.out.println("arrayPath: "+arrayPath);
    //System.out.println("key: "+key);
    //System.out.println("keyValue: "+keyValue);
    //System.out.println("valueKey: "+valueKey);
    try {
      Iterator<JsonNode> fieldsIterator = mapper.readTree(jsonString).at(arrayPath).iterator();
      while (fieldsIterator.hasNext()) {
        //System.out.println("get array");
        JsonNode node = fieldsIterator.next();
        String fieldName = node.at(key).asText("");
        //System.out.println("fieldName: "+fieldName);
        if(fieldName.equalsIgnoreCase(keyValue)) {
          //System.out.println("The key fieldName found: "+fieldName);
          return node.at(valueKey);
        }
      }
    } catch(Exception ex) {
      // issue converting json response to json. ignore.
    }
    return null;
  }

  static public String readJsonElementKeyValue(JsonNode node, String arrayPath, String key, String keyValue, String valueKey) {
    //System.out.println("key: "+key);
    //System.out.println("keyValue: "+keyValue);
    //System.out.println("valueKey: "+valueKey);
    try {
      Iterator<JsonNode> fieldsIterator = node.at(arrayPath).iterator();
      while (fieldsIterator.hasNext()) {
        JsonNode objNode = fieldsIterator.next();
        String fieldName = objNode.at(key).asText("");
        //System.out.println(":fieldName:"+fieldName);
        if(fieldName.equalsIgnoreCase(keyValue)) {
          //System.out.println(objNode.at(valueKey).asText(""));
          return objNode.at(valueKey).asText("");
        }
      }
    } catch(Exception ex) {
      // issue converting json response to json. ignore.
    }
    return "";
  }
}
