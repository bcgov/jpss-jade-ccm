package ccm.models.system.dems;

import java.util.List;

public class DemsFieldData {
    private String name;
    private String value;
    private List<String> value_list;

    public DemsFieldData(String name, String value, List<String> valueList) {
        setName(name);

        setValue(value);

        setValue_list(valueList);
    }

    public DemsFieldData(String name, String value) {
        setName(name);

        setValue(value);
    }

    public DemsFieldData(String name, List<String> valueList) {
        setName(name);

        setValue_list(valueList);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getValue() {
        return value;
    }


    public void setValue(String value) {
        this.value = value;
    }


    public List<String> getValue_list() {
        return value_list;
    }


    public void setValue_list(List<String> value_list) {
        this.value_list = value_list;
    }

}   
