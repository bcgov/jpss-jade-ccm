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

        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        private CASE_FLAG_FIELD_MAPPINGS(String name) {
            this.label = name;
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

    public enum CASE_GROUP_FIELD_MAPPINGS {
        SYSTEM_SUPPORT("JRS_SYSTEM_SUPPORT","System Support"),
        ADMINISTRATOR("JRS_ADMINISTRATOR","Administrator"),
        LAWYER("JRS_BASE_LAWYER","Lawyer"),
        LEGAL_ASSISTANT("JRS_BASE_LEGAL_ASST","Legal Assistant"),
        PARALEGAL("JRS_BASE_PARALEGAL","Paralegal"),
        DEMS_SYSTEM_SUPPORT("JRS_DEMS_SYSTEM_SUPPORT","DEMS System Support");

        private String justin_name;
        private String dems_name;

        private CASE_GROUP_FIELD_MAPPINGS(String justin_name, String dems_name) {
            this.justin_name = justin_name;
            this.dems_name = dems_name;
        }

        public String getJustin_name() {
            return justin_name;
        }

        public void setJustin_name(String justin_name) {
            this.justin_name = justin_name;
        }

        public String getDems_name() {
            return dems_name;
        }

        public void setDems_name(String dems_name) {
            this.dems_name = dems_name;
        }

        public static CASE_GROUP_FIELD_MAPPINGS findCaseGroupByJustinName(String justin_name) {
            CASE_GROUP_FIELD_MAPPINGS caseGroup = null;

            if (SYSTEM_SUPPORT.getJustin_name().equals(justin_name)) {
                caseGroup = SYSTEM_SUPPORT;
            } else if (ADMINISTRATOR.getJustin_name().equals(justin_name)) {
                caseGroup = ADMINISTRATOR;
            } else if (LAWYER.getJustin_name().equals(justin_name)) {
                caseGroup = LAWYER;
            } else if (LEGAL_ASSISTANT.getJustin_name().equals(justin_name)) {
                caseGroup = LEGAL_ASSISTANT;
            } else if (PARALEGAL.getJustin_name().equals(justin_name)) {
                caseGroup = PARALEGAL;
            } else if (DEMS_SYSTEM_SUPPORT.getJustin_name().equals(justin_name)) {
                caseGroup = DEMS_SYSTEM_SUPPORT;
            }
            
            return caseGroup;
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
