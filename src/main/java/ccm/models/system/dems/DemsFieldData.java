package ccm.models.system.dems;

public class DemsFieldData {
    public enum FIELD_MAPPINGS {
        AGENCY_FILE_ID(12, "Agency File ID"),
        AGENCY_FILE_NO(3, "Agency File No."),
        SUBMIT_DATE(13, "Submit Date"),
        ASSESSMENT_CROWN(9, "Assessment Crown"),
        CASE_DECISION(14, "Case Decision"),
        PROPOSED_CHARGES(18, "Proposed Charges"),
        INITIATING_AGENCY(24, "Initiating Agency"),
        INVESTIGATING_OFFICER(25, "Investigating Officer"),
        PROPOSED_CROWN_OFFICE(26, "Proposed Crown Office"),
        CASE_FLAGS(28, "Case Flags"),
        OFFENCE_DATE(29, "Offence Date (earliest)"),
        PROPOSED_APP_DATE(30, "Proposed App. Date (earliest)"),
        PROPOSED_PROCESS_TYPE(31, "Proposed Process Type"),
        MDOC_JUSTIN_NO(15, "Court File Unique ID"),
        CROWN_ELECTION(16, "Crown Election"),
        COURT_FILE_LEVEL(17, "Court File Level"),
        CLASS(19, "Class"),
        DESIGNATION(20, "Designation"),
        SWORN_DATE(21, "Sworn Date"),
        APPROVED_CHARGES(22, "Approved Charges"),
        COURT_FILE_NO(23, "Court File No."),
        COURT_HOME_REG(27, "Court Home Registry"),
        RMS_PROC_STAT(32, "RMS Processing Status"),
        ASSIGNED_LEGAL_STAFF(33, "Assigned Legal Staff"),
        ASSIGNED_CROWN(17, "Assigned Crown"),
        INITIAL_APP_DT(37, "Initial App. Date"),
        INITIAL_APP_REASON(38, "Initial App. Date Reason"),
        NEXT_APP_DT(39, "Next App. Date"),
        NEXT_APP_REASON(40, "Next App. Date Reason"),
        FIRST_TRIAL_DT(41, "First Trial Date"),
        FIRST_TRIAL_REASON(42, "First Trial Date Reason"),
        ASSIGNED_CROWN_NAME(43, "Assigned Crown Name");

        private int id;
        private String label;

        private FIELD_MAPPINGS(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int getId() {
            return id;
        }
        public String getLabel() {
            return label;
        }
    }

    private int id;
    private String name;
    private Object value;

    public DemsFieldData() {
    }

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


    public String getName() {
        return name;
    }

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
