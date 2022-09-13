package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseData;
import ccm.models.business.BusinessCourtCaseMetadataData;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsCourtCaseAppearanceSummaryData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

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
        ASSIGNED_CROWN(17, "Assigned Crown");

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
    public DemsCourtCaseData() {
    }

    public DemsCourtCaseData(String key, String name, BusinessCourtCaseMetadataData bccm) {
        setName(accused_names.substring(0, accused_names.length() > 255 ? 254 : accused_names.length()));
        setKey(bcc.getRcc_id());

        DemsFieldData courtFileId = new DemsFieldData(FIELD_MAPPINGS.MDOC_JUSTIN_NO.getId(), FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), bccm.getCourt_file_id());
        DemsFieldData crownElection = new DemsFieldData(FIELD_MAPPINGS.CROWN_ELECTION.getId(), FIELD_MAPPINGS.CROWN_ELECTION.getLabel(), bccm.getAnticipated_crown_election());
        DemsFieldData courtFileLevel = new DemsFieldData(FIELD_MAPPINGS.COURT_FILE_LEVEL.getId(), FIELD_MAPPINGS.COURT_FILE_LEVEL.getLabel(), bccm.getCourt_file_level());
        DemsFieldData fileClass = new DemsFieldData(FIELD_MAPPINGS.CLASS.getId(), FIELD_MAPPINGS.CLASS.getLabel(), bccm.getCourt_file_class());
        DemsFieldData designation = new DemsFieldData(FIELD_MAPPINGS.DESIGNATION.getId(), FIELD_MAPPINGS.DESIGNATION.getLabel(), bccm.getCourt_file_designation());
        DemsFieldData swornDate = new DemsFieldData(FIELD_MAPPINGS.SWORN_DATE.getId(), FIELD_MAPPINGS.SWORN_DATE.getLabel(), bccm.getCourt_file_sworn_date());
        DemsFieldData approvedCharges = new DemsFieldData(FIELD_MAPPINGS.APPROVED_CHARGES.getId(), FIELD_MAPPINGS.APPROVED_CHARGES.getLabel(), bccm.getOffence_description_list());
        DemsFieldData courtFileNo = new DemsFieldData(FIELD_MAPPINGS.COURT_FILE_NO.getId(), FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), bccm.getCourt_file_number_seq_type());
        DemsFieldData courtHomeReg = new DemsFieldData(FIELD_MAPPINGS.COURT_HOME_REG.getId(), FIELD_MAPPINGS.COURT_HOME_REG.getLabel(), bccm.getCourt_home_registry());
        //DemsFieldData rmsProcStatus = new DemsFieldData(FIELD_MAPPINGS.RMS_PROC_STAT.getId(), FIELD_MAPPINGS.RMS_PROC_STAT.getLabel(), bccm.get());
        //DemsFieldData assignedLegalStaff = new DemsFieldData(FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getId(), FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), bccm.get());

        List<DemsFieldData> fieldData = this.getFields();
        fieldData.add(courtFileId);
        fieldData.add(crownElection);
        fieldData.add(courtFileLevel);
        fieldData.add(fileClass);
        fieldData.add(designation);
        fieldData.add(swornDate);
        fieldData.add(approvedCharges);
        fieldData.add(courtFileNo);
        fieldData.add(courtHomeReg);

    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

}
