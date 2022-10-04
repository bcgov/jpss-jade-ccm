package ccm.models.system.dems;

public class DemsListItemFieldData {

    public enum CASE_FLAG_FIELD_MAPPINGS {
        VUL1(9),
        CHI1(10),
        K(11),
        Indigenous(12);

        private int id;

        private CASE_FLAG_FIELD_MAPPINGS(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }

    public enum CASE_DECISION_FIELD_MAPPINGS {
        ADV(3),
        ACT(4),
        RET(5),
        ACL(6),
        NAC(7),
        REF(8);

        private int id;

        private CASE_DECISION_FIELD_MAPPINGS(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }
    }
    private int id;

    public DemsListItemFieldData(int id) {
        setId(id);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

}   
