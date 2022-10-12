package ccm.models.system.dems;

public class DemsListItemFieldData {

    public enum CASE_FLAG_FIELD_MAPPINGS {
        // VUL1(9),
        // CHI1(10),
        // K(11),
        // Indigenous(12);

        VUL1("VUL1"),
        CHI1("CHI1"),
        K("K"),
        Indigenous("Indigenous");

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private CASE_FLAG_FIELD_MAPPINGS(String name) {
            this.name = name;
        }
    }

    public enum CASE_DECISION_FIELD_MAPPINGS {
        ADV("ADV"),
        ACT("ACT"),
        RET("RET"),
        ACL("ACL"),
        NAC("NAC"),
        REF("REF");

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private CASE_DECISION_FIELD_MAPPINGS(String name) {
            this.name = name;
        }
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DemsListItemFieldData(String name) {
        setName(name);
    }

}   
