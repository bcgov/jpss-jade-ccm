package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;

import ccm.models.business.BusinessCourtCaseData;
import ccm.models.business.BusinessCourtCaseMetadataData;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsCourtCaseData {
    public static final String PACIFIC_TIMEZONE = "Pacific Standard Time";
    public static final String TEMPLATE_CASE = "28";
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private String description;
    private String timeZoneId;
    private String templateCase;
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

    public DemsCourtCaseData(BusinessCourtCaseData bcc) {

        StringBuilder accused_names = new StringBuilder();
        for (BusinessCourtCaseAccused ba : bcc.getAccused_person()) {
            // Map 87
            if(accused_names.length() > 0) {
                accused_names.append(SEMICOLON_SPACE_STRING);
            }
            if(ba.getFull_name() != null && !ba.getFull_name().isEmpty()) {
                // JADE-1470 surnames should be in all uppercase.
                String[] names = ba.getFull_name().split(COMMA_STRING, 2);
                if(names.length > 1) {
                    accused_names.append(names[0].toUpperCase());
                    accused_names.append(COMMA_STRING);
                    accused_names.append(names[1]);
                } else {
                    accused_names.append(ba.getFull_name());
                }
            }
        }
        setName(accused_names.substring(0, accused_names.length() > 255 ? 254 : accused_names.length()));
        setTimeZoneId(PACIFIC_TIMEZONE);
        setKey(bcc.getRcc_id());
        setDescription("");
        setTemplateCase(TEMPLATE_CASE);


        // Map any case flags that exist
        List<DemsListItemFieldData> caseFlagList = new ArrayList<DemsListItemFieldData>();
        for (String caseFlag : bcc.getCase_flags()) {
            if(CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(CASE_FLAG_FIELD_MAPPINGS.VUL1.getId()));
            } else if(CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(CASE_FLAG_FIELD_MAPPINGS.CHI1.getId()));
            } else if(CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(CASE_FLAG_FIELD_MAPPINGS.Indigenous.getId()));
            } else if(CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(new DemsListItemFieldData(CASE_FLAG_FIELD_MAPPINGS.K.getId()));
            }
        }
        DemsListItemFieldData caseDecisionValue = null;
        if(CASE_DECISION_FIELD_MAPPINGS.ADV.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(CASE_DECISION_FIELD_MAPPINGS.ADV.getId());
        } else if(CASE_DECISION_FIELD_MAPPINGS.ACT.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(CASE_DECISION_FIELD_MAPPINGS.ACT.getId());
        } else if(CASE_DECISION_FIELD_MAPPINGS.RET.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(CASE_DECISION_FIELD_MAPPINGS.RET.getId());
        } else if(CASE_DECISION_FIELD_MAPPINGS.ACL.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(CASE_DECISION_FIELD_MAPPINGS.ACL.getId());
        } else if(CASE_DECISION_FIELD_MAPPINGS.NAC.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(CASE_DECISION_FIELD_MAPPINGS.NAC.getId());
        } else if(CASE_DECISION_FIELD_MAPPINGS.REF.name().equals(bcc.getCase_decision_cd())) {
            caseDecisionValue = new DemsListItemFieldData(CASE_DECISION_FIELD_MAPPINGS.REF.getId());
        }


        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData agencyFileId = new DemsFieldData(FIELD_MAPPINGS.AGENCY_FILE_ID.getId(), FIELD_MAPPINGS.AGENCY_FILE_ID.getLabel(), bcc.getRcc_id());
        DemsFieldData agencyFileNo = new DemsFieldData(FIELD_MAPPINGS.AGENCY_FILE_NO.getId(), FIELD_MAPPINGS.AGENCY_FILE_NO.getLabel(), bcc.getAgency_file_no());
        DemsFieldData submitDate = new DemsFieldData(FIELD_MAPPINGS.SUBMIT_DATE.getId(), FIELD_MAPPINGS.SUBMIT_DATE.getLabel(), bcc.getRcc_submit_date());
        DemsFieldData assessmentCrown = new DemsFieldData(FIELD_MAPPINGS.ASSESSMENT_CROWN.getId(), FIELD_MAPPINGS.ASSESSMENT_CROWN.getLabel(), bcc.getAssessment_crown_name());
        DemsFieldData caseDecision = new DemsFieldData(FIELD_MAPPINGS.CASE_DECISION.getId(), FIELD_MAPPINGS.CASE_DECISION.getLabel(), caseDecisionValue);
        DemsFieldData proposedCharges = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_CHARGES.getId(), FIELD_MAPPINGS.PROPOSED_CHARGES.getLabel(), bcc.getCharge());
        DemsFieldData initiatingAgency = new DemsFieldData(FIELD_MAPPINGS.INITIATING_AGENCY.getId(), FIELD_MAPPINGS.INITIATING_AGENCY.getLabel(), bcc.getInitiating_agency());
        DemsFieldData investigatingOfficer = new DemsFieldData(FIELD_MAPPINGS.INVESTIGATING_OFFICER.getId(), FIELD_MAPPINGS.INVESTIGATING_OFFICER.getLabel(), bcc.getInvestigating_officer());
        DemsFieldData proposedCrownOffice = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getId(), FIELD_MAPPINGS.PROPOSED_CROWN_OFFICE.getLabel(), bcc.getProposed_crown_office());
        DemsFieldData caseFlags = new DemsFieldData(FIELD_MAPPINGS.CASE_FLAGS.getId(), FIELD_MAPPINGS.CASE_FLAGS.getLabel(), caseFlagList);
        DemsFieldData offenceDate = new DemsFieldData(FIELD_MAPPINGS.OFFENCE_DATE.getId(), FIELD_MAPPINGS.OFFENCE_DATE.getLabel(), bcc.getEarliest_offence_date());
        DemsFieldData proposedAppDate = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_APP_DATE.getId(), FIELD_MAPPINGS.PROPOSED_APP_DATE.getLabel(), bcc.getEarliest_proposed_appearance_date());
        DemsFieldData proposedProcessType = new DemsFieldData(FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getId(), FIELD_MAPPINGS.PROPOSED_PROCESS_TYPE.getLabel(), bcc.getProposed_process_type_list());

        fieldData.add(agencyFileId);
        fieldData.add(agencyFileNo);
        fieldData.add(submitDate);
        fieldData.add(assessmentCrown);
        fieldData.add(caseDecision);
        fieldData.add(proposedCharges);
        fieldData.add(initiatingAgency);
        fieldData.add(investigatingOfficer);
        fieldData.add(proposedCrownOffice);
        fieldData.add(caseFlags);
        fieldData.add(offenceDate);
        fieldData.add(proposedAppDate);
        fieldData.add(proposedProcessType);
        setFields(fieldData);

    }

    public DemsCourtCaseData(BusinessCourtCaseData bcc, BusinessCourtCaseMetadataData bccm) {
        this(bcc);
        /*MDOC_JUSTIN_NO(15, "Court File Unique ID"),
        CROWN_ELECTION(16, "Crown Election"),
        COURT_FILE_LEVEL(17, "Court File Level"),
        CLASS(19, "Class"),
        DESIGNATION(20, "Designation"),
        SWORN_DATE(21, "Sworn Date"),
        APPROVED_CHARGES(22, "Approved Charges"),
        COURT_HOME_REG(27, "Court Home Registry"),
        RMS_PROC_STAT(32, "RMS Processing Status"),
        ASSIGNED_LEGAL_STAFF(33, "Assigned Legal Staff"),
        ASSIGNED_CROWN(17, "Assigned Crown"); */
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTimeZoneId() {
        return timeZoneId;
    }

    public void setTimeZoneId(String timeZoneId) {
        this.timeZoneId = timeZoneId;
    }

    public String getTemplateCase() {
        return templateCase;
    }

    public void setTemplateCase(String templateCase) {
        this.templateCase = templateCase;
    }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

}
