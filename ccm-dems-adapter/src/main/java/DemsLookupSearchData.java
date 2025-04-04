package ccm.models.system.dems;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DemsLookupSearchData {

    private Integer id;
    private String key;
    private String status;

    public DemsLookupSearchData (Integer caseId, String caseKey, String caseStatus) {
        id = caseId;
        key = caseKey;
        status = caseStatus;
    }

    public Integer getId() {
        return id;
    }
    public void setId(Integer id) {
        this.id = id;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

}
