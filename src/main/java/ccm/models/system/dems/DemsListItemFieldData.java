package ccm.models.system.dems;

public class DemsListItemFieldData {

    public enum CASE_FLAG_FIELD_MAPPINGS {
        // VUL1(9,"VUL1"),
        // CHI1(10,"CHI1"),
        // K(11,"K"),
        // Indigenous(12,"Indigenous"),
        // HROIP(15,"HROIP"),
        // DO_LTO(14,"DO/LTO");

        VUL1("VUL1"),
        CHI1("CHI1"),
        K("K"),
        Indigenous("Indigenous"),
        HROIP("HROIP"),
        DO_LTO("DO/LTO");

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
        // ADV(3,"ADV - Alternative Measures"),
        // ACT(4,"ACT - Approved to Court"),
        // RET(5,"RET - Return to Enforcement Agency"),
        // ACL(6,"ACL - Caution Letter"),
        // NAC(7,"NAC - No Action/Charge"),
        // REF(8,"REF - Referred");

        ADV("ADV - Alternative Measures"),
        ACT("ACT - Approved to Court"),
        RET("RET - Return to Enforcement Agency"),
        ACL("ACL - Caution Letter"),
        NAC("NAC - No Action/Charge"),
        REF("REF - Referred");

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
