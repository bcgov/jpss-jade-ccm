package ccm.models.system.dems;

public class DemsListItemFieldData {

    public enum CASE_FLAG_FIELD_MAPPINGS {
        // VUL1(9),
        // CHI1(10),
        // K(11),
        // Indigenous(12);

        VUL1(9,"VUL1"),
        CHI1(10,"CHI1"),
        K(11,"K"),
        Indigenous(12,"Indigenous"),
        HROIP(15,"HROIP"),
        DO_LTO(14,"DO/LTO");

        private String name;
        private int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        private CASE_FLAG_FIELD_MAPPINGS(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    public enum CASE_DECISION_FIELD_MAPPINGS {
        // {
        //     "id": 3,
        //     "customColumnId": 14,
        //     "name": "ADV - Alternative Measures",
        //     "delete": false
        // },
        // {
        //     "id": 4,
        //     "customColumnId": 14,
        //     "name": "ACT - Approved to Court",
        //     "delete": false
        // },
        // {
        //     "id": 5,
        //     "customColumnId": 14,
        //     "name": "RET - Return to Enforcement Agency",
        //     "delete": false
        // },
        // {
        //     "id": 6,
        //     "customColumnId": 14,
        //     "name": "ACL - Caution Letter",
        //     "delete": false
        // },
        // {
        //     "id": 7,
        //     "customColumnId": 14,
        //     "name": "NAC - No Action/Charge",
        //     "delete": false
        // },
        // {
        //     "id": 8,
        //     "customColumnId": 14,
        //     "name": "REF - Referred",
        //     "delete": false
        // }

        ADV(3,"ADV - Alternative Measures"),
        ACT(4,"ACT - Approved to Court"),
        RET(5,"RET - Return to Enforcement Agency"),
        ACL(6,"ACL - Caution Letter"),
        NAC(7,"NAC - No Action/Charge"),
        REF(8,"REF - Referred");

        private String name;
        private int id;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        private CASE_DECISION_FIELD_MAPPINGS(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    //private String name;
    private int id;

    // public String getName() {
    //     return name;
    // }

    // public void setName(String name) {
    //     this.name = name;
    // }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    //public DemsListItemFieldData(int id, String name) {
    public DemsListItemFieldData(int id) {
        setId(id);
        //setName(name);
    }

}   
