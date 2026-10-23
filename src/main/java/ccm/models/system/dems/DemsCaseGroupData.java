package ccm.models.system.dems;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DemsCaseGroupData {
    private Long id;
    private String name;
    private Boolean isUserGroup;

    public DemsCaseGroupData() {
    }

    public DemsCaseGroupData(Long id, String name, Boolean isUserGroup) {
        setId(id);
        setName(name);
        setIsUserGroup(isUserGroup);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getIsUserGroup() {
        return isUserGroup;
    }

    public void setIsUserGroup(Boolean isUserGroup) {
        this.isUserGroup = isUserGroup;
    }
}