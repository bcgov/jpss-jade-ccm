package ccm.models.system.dems;

public class DemsFieldData {
    public enum FIELD_MAPPINGS {
        // PART_ID(2, "PartID"),
        // AGENCY_FILE_ID(12, "Agency File ID"),
        // AGENCY_FILE_NO(3, "Agency File No."),
        // SUBMIT_DATE(13, "Submit Date"),
        // ASSESSMENT_CROWN(9, "Assessment Crown"),
        // CASE_DECISION(14, "Case Decision"),
        // PROPOSED_CHARGES(18, "Proposed Charges"),
        // INITIATING_AGENCY(24, "Initiating Agency"),
        // INVESTIGATING_OFFICER(25, "Investigating Officer"),
        // PROPOSED_CROWN_OFFICE(26, "Proposed Crown Office"),
        // CASE_FLAGS(28, "Case Flags"),
        // OFFENCE_DATE(29, "Offence Date (earliest)"),
        // PROPOSED_APP_DATE(30, "Proposed App. Date (earliest)"),
        // PROPOSED_PROCESS_TYPE(31, "Proposed Process Type"),
        // MDOC_JUSTIN_NO(15, "Court File Unique ID"),
        // CROWN_ELECTION(16, "Crown Election"),
        // COURT_FILE_LEVEL(17, "Court File Level"),
        // CLASS(19, "Class"),
        // DESIGNATION(20, "Designation"),
        // SWORN_DATE(21, "Sworn Date"),
        // CHARGES(45, "Charges"),
        // COURT_FILE_NO(23, "Court File No."),
        // COURT_HOME_REG(27, "Court Home Registry"),
        // COURT_HOME_REG_NAME(44, "Court Home Registry Name"),
        // RMS_PROC_STAT(32, "RMS Processing Status"),
        // ASSIGNED_LEGAL_STAFF(33, "Assigned Legal Staff"),
        // ASSIGNED_CROWN(34, "Assigned Crown"),
        // INITIAL_APP_DT(37, "Initial App. Date"),
        // INITIAL_APP_REASON(38, "Initial App. Date Reason"),
        // NEXT_APP_DT(39, "Next App. Date"),
        // NEXT_APP_REASON(40, "Next App. Date Reason"),
        // FIRST_TRIAL_DT(41, "First Trial Date"),
        // FIRST_TRIAL_REASON(42, "First Trial Date Reason"),
        // ASSIGNED_CROWN_NAME(43, "Assigned Crown Name"),
        // PERSON_DATE_OF_BIRTH(35, "Date Of Birth"),
        // PERSON_GIVEN_NAME_2(51, "Given Name 2"),
        // PERSON_GIVEN_NAME_3(52, "Given Name 3"),
        // PERSON_FULL_NAME(54, "Full Name"),
        // ACCUSED_FULL_NAME(47, "Accused Full Name"),
        // LIMITATION_DATE(55, "Limitation Date");

        PART_ID("PartID"),
        AGENCY_FILE_ID("Agency File ID"),
        AGENCY_FILE_NO("Agency File No."),
        SUBMIT_DATE("Submit Date"),
        ASSESSMENT_CROWN("Assessment Crown"),
        CASE_DECISION("Case Decision"),
        PROPOSED_CHARGES("Proposed Charges"),
        INITIATING_AGENCY("Initiating Agency"),
        INVESTIGATING_OFFICER("Investigating Officer"),
        PROPOSED_CROWN_OFFICE("Proposed Crown Office"),
        CASE_FLAGS("Case Flags"),
        OFFENCE_DATE("Offence Date (earliest)"),
        PROPOSED_APP_DATE("Proposed Appr. Date (earliest)"),
        PROPOSED_PROCESS_TYPE("Proposed Process Type"),
        MDOC_JUSTIN_NO("Court File Unique ID"),
        CROWN_ELECTION("Crown Election"),
        COURT_FILE_LEVEL("Court File Level"),
        CLASS("Class"),
        DESIGNATION("Designation"),
        SWORN_DATE("Sworn Date"),
        CHARGES("Charges"),
        COURT_FILE_NO("Court File No."),
        COURT_FILE_DETAILS("Court File Details"),
        COURT_HOME_REG("Court Home Registry"),
        COURT_HOME_REG_NAME("Court Home Registry Name"),
        RMS_PROC_STAT("RMS Processing Status"),
        ASSIGNED_LEGAL_STAFF("Assigned Legal Staff"),
        ASSIGNED_CROWN("Assigned Crown"),
        INITIAL_APP_DT("Initial Appr."),
        INITIAL_APP_REASON("Initial Appr. Rsn"),
        NEXT_APP_DT("Next Appr."),
        NEXT_APP_REASON("Next Appr. Rsn"),
        FIRST_TRIAL_DT("First Trial"),
        FIRST_TRIAL_REASON("First Trial Rsn"),
        ASSIGNED_CROWN_NAME("Assigned Crown Name"),
        PERSON_DATE_OF_BIRTH("Date Of Birth"),
        PERSON_GIVEN_NAME_2("Given Name 2"),
        PERSON_GIVEN_NAME_3("Given Name 3"),
        PERSON_FULL_NAME("Full Name"),
        ACCUSED_FULL_NAME("Accused Full Name"),
        LIMITATION_DATE("Limitation Date"),
        CROWN_OFFICE("Crown Office"),
        RCC_STATUS("RCC Status"),
        LAST_JUSTIN_UPDATE("Last JUSTIN Update"),
        LAST_API_RECORD_UPDATE("Last API Record Update");

        private String label;

        private FIELD_MAPPINGS(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private String name;
    private Object value;

    public DemsFieldData() {
    }

    public DemsFieldData(String name, Object value) {
        setName(name);
        setValue(value);
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
