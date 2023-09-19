package ccm.models.common.data;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import ccm.models.system.justin.JustinRccCaseList;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CommonCaseList {
    List<String> keys;

    public CommonCaseList() {
    }
    public CommonCaseList(JustinRccCaseList jdl) {
      List<String> rccList = new ArrayList<String>();

    if(jdl.getRcc_ids() != null) {
     // ObjectMapper objectMapper = new ObjectMapper();
     // try {
     //   objectMapper.writeValueAsString(Arrays.asList(jdl.getRcc_ids()));
     // } catch (JsonProcessingException e) {
     //   e.printStackTrace();
     // }
        rccList.addAll(jdl.getRcc_ids());
      }
      setKeys(rccList);
    }

    public List<String> getKeys() {
        return keys;
    }
    public void setKeys(List<String> keys) {
        this.keys = keys;
    }
}