package ccm.utils;

import java.util.Iterator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class JsonParseUtils {

  static public String getJsonElementValue(String jsonString, String value) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      JsonNode field = mapper.readTree(jsonString);
      if (field.isObject()) {
        ObjectNode obj = (ObjectNode) field;
        if (obj.has(value)) {
          return obj.get(value).asText("");
        }
      }
      return "";
      
    } catch(Exception ex) {
      // issue converting json response to json. ignore.
    }
    return "";// if not found, then just return blank string.
  }

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
      ex.printStackTrace();
      // issue converting json response to json. ignore.
    }
    return "";
  }

  static public String encodeUrlSensitiveChars(String key) {
    String replacement = key;
    if (replacement != null) {
      replacement = replacement.replace("%","%25");
      replacement = replacement.replace("<","%3C");
      replacement = replacement.replace(">","%3E");
      replacement = replacement.replace("!","%21");
      replacement = replacement.replace("#","%23");
      replacement = replacement.replace("^","%5E");
      replacement = replacement.replace(";","%3B");
      replacement = replacement.replace("@","%40");
      replacement = replacement.replace("=","%3D");
      replacement = replacement.replace("{","%7B");
      replacement = replacement.replace("}","%7D");
      replacement = replacement.replace("~","%7E");
      replacement = replacement.replace("\\","%5C");
      replacement = replacement.replace("$","%24");
      replacement = replacement.replace("&","%26");

//    <>%!#^*;@={}~\\$&
//    %3C%3E%25%21%23%5E*%3B%40%3D%7B%7D%7E%5C%24%26



    }

    return replacement;
  }
}
