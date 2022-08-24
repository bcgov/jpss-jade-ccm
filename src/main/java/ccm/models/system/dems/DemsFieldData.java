package ccm.models.system.dems;

public class DemsFieldData {
    private int id;
    private String name;
    private Object value;

    public DemsFieldData(String name, Object value) {
        setName(name);
        setValue(value);
    }

    public DemsFieldData(int id, Object value) {
        setId(id);
        setValue(value);
    }

    public DemsFieldData(int id, String name, Object value) {
        setId(id);
        setName(name);
        setValue(value);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

/*
    public String getName() {
        return name;
    }
*/
    public void setName(String name) {
        this.name = name;
    }

    public Object getValue() {
        return value;
    }


    public void setValue(Object value) {
        this.value = value;
    }

}   
