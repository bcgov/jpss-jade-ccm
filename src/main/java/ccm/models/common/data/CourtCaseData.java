package ccm.models.common.data;

import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinAgencyFileRef;
import ccm.models.system.justin.JustinCourtFile;
import ccm.models.system.justin.JustinCourtFileRef;

import java.util.ArrayList;
import java.util.List;

public class CourtCaseData {
  public static final String DASH_STRING = "-";
  public static final String COLON_STRING = ":";
  public static final String YES_STRING = "Y";

  private String court_file_id;
  private String court_file_no;
  private String court_file_type_reference;
  private String anticipated_crown_election;
  private String court_file_seq_no;
  private String court_file_level;
  private String court_file_class;
  private String court_file_designation;
  private String court_file_sworn_date;
  private String offence_description_list;
  private String court_file_number_seq_type;
  private String court_home_registry;
  private String court_home_registry_name;
  private String court_home_registry_identifier;
  private String accused_names;
  //private String rms_processing_status;
  private List<String> case_flags;

  private List<CaseAccused> accused_persons;
  private List<ChargeAssessmentDataRef> related_agency_file;
  private ChargeAssessmentDataRef primary_agency_file;
  //added as part of jade-2483
  private List<CourtFileDataRef> related_court_file;

  private String approving_crown_agency_name;
  private String approving_crown_agency_ident;

  private List<CourtCaseData> related_court_cases;

  public CourtCaseData() {
  }

  public CourtCaseData(JustinCourtFile jcf) {
    related_court_cases = new ArrayList<CourtCaseData>();
    setCourt_file_id(jcf.getMdoc_justin_no());
    setCourt_file_no(jcf.getCourt_file_no());
    setCourt_file_type_reference(jcf.getType_reference());
    setAnticipated_crown_election(jcf.getCrown_election());
    setCourt_file_seq_no(jcf.getMdoc_seq_no());
    setCourt_file_level(jcf.getCourt_level_cd());
    setCourt_file_class(jcf.getCourt_class_cd());
    setCourt_file_designation(jcf.getFile_designation());
    setCourt_file_sworn_date(jcf.getSworn_date());
    setOffence_description_list(jcf.getApproved_charges());

    //"$MAPID30-$MAPID33-$MAPID31"
    StringBuilder fileSeqNoType = new StringBuilder();
    fileSeqNoType.append(jcf.getCourt_file_no());
    if(jcf.getMdoc_seq_no() != null) {
      fileSeqNoType.append(DASH_STRING);
      fileSeqNoType.append(jcf.getMdoc_seq_no());
    }
    if(jcf.getType_reference() != null) {
      fileSeqNoType.append(DASH_STRING);
      fileSeqNoType.append(jcf.getType_reference());
    }
    setCourt_file_number_seq_type(fileSeqNoType.toString());

    //"$MAPID62- $MAPID29"
    StringBuilder courtHomeReg = new StringBuilder();
    courtHomeReg.append(jcf.getHome_court_agency_identifier());
    courtHomeReg.append(COLON_STRING + " ");
    courtHomeReg.append(jcf.getHome_court_agency_name());
    setCourt_home_registry(courtHomeReg.toString());
    setCourt_home_registry_name(jcf.getHome_court_agency_name());
    setCourt_home_registry_identifier(jcf.getHome_court_agency_identifier());


    //[(If $MAPID81=Y then add "VUL1"), (If $MAPID80=Y then add "CHI1"), (If $MAPID82=Y then add "K", (If $MAPID79=Y then add "Indigenous"]
    case_flags = new ArrayList<String>();

    if (YES_STRING.equalsIgnoreCase(jcf.getVul1())) {
        case_flags.add("VUL1");
    }
    if (YES_STRING.equalsIgnoreCase(jcf.getChi1())) {
        case_flags.add("CHI1");
    }
    if(YES_STRING.equalsIgnoreCase(jcf.getKfile_yn())) {
      case_flags.add("K");
    }

    // Removed per BCPSDEMS-550 (and sub-task BCPSDEMS-552)
    // if(jcf.getMdocaccused() != null) {
    //   for (JustinAccused accused : jcf.getMdocaccused()) {
    //       if (YES_STRING.equalsIgnoreCase(accused.getIndigenous_yn())) {
    //           case_flags.add("Indigenous");
    //           break;
    //       }
    //   }
    // }

    List<CaseAccused> accusedList = new ArrayList<CaseAccused>();
    StringBuilder accused_names = new StringBuilder();

    if(jcf.getMdocaccused() != null) {
      for (JustinAccused ja : jcf.getMdocaccused()) {

        CaseAccused accused = new CaseAccused(ja);
        accusedList.add(accused);
        // Map 87
        if(accused_names.length() > 0) {
            accused_names.append(", ");
        }
        accused_names.append(ja.getAccused_given_1_nm());
        accused_names.append(" ");
        accused_names.append(ja.getAccused_surname_nm());
      }
    }
    setAccused_persons(accusedList);
    setAccused_names(accused_names.toString());

    List<ChargeAssessmentDataRef> agencyList = new ArrayList<ChargeAssessmentDataRef>();
    List<CourtFileDataRef> courtList = new ArrayList<CourtFileDataRef>();

    if(jcf.getRelated_rcc() != null) {
      for (JustinAgencyFileRef jafr : jcf.getRelated_rcc()) {
        ChargeAssessmentDataRef agencyRef = new ChargeAssessmentDataRef(jafr);
        agencyList.add(agencyRef);
        if(agencyRef.getPrimary_yn().equalsIgnoreCase("Y") || getPrimary_agency_file() == null) {
          setPrimary_agency_file(agencyRef);
        }
      }
    }
    setRelated_agency_file(agencyList);
    if(jcf.getRelated_court_file() != null){
      for(JustinCourtFileRef jcfr : jcf.getRelated_court_file()){
        CourtFileDataRef courtRef = new CourtFileDataRef(jcfr);
        courtList.add(courtRef);
      }
    }
    setRelated_court_file(courtList);

    if(jcf.getApproving_crown_agency_name() != null) {
      String approving_crown_name = jcf.getApproving_crown_agency_name();

      int index_crown_consel = approving_crown_name.indexOf("Crown Counsel");

      // MAP 69
      if (index_crown_consel >= 0) {
          // removing the suffix string
          approving_crown_name = approving_crown_name.substring(0, index_crown_consel).trim();
      }

      setApproving_crown_agency_name(approving_crown_name);
    }
    setApproving_crown_agency_ident(jcf.getApproving_crown_agency_ident());

  }

  public String getCourt_file_id() {
    return court_file_id;
  }

  public void setCourt_file_id(String court_file_id) {
    this.court_file_id = court_file_id;
  }

  public String getCourt_file_no() {
    return court_file_no;
  }

  public void setCourt_file_no(String court_file_no) {
    this.court_file_no = court_file_no;
  }

  public String getCourt_file_type_reference() {
    return court_file_type_reference;
  }

  public void setCourt_file_type_reference(String court_file_type_reference) {
    this.court_file_type_reference = court_file_type_reference;
  }

  public String getAnticipated_crown_election() {
    return anticipated_crown_election;
  }

  public void setAnticipated_crown_election(String anticipated_crown_election) {
    this.anticipated_crown_election = anticipated_crown_election;
  }

  public String getCourt_file_seq_no() {
    return court_file_seq_no;
  }

  public void setCourt_file_seq_no(String court_file_seq_no) {
    this.court_file_seq_no = court_file_seq_no;
  }

  public String getCourt_file_level() {
    return court_file_level;
  }

  public void setCourt_file_level(String court_file_level) {
    this.court_file_level = court_file_level;
  }

  public String getCourt_file_class() {
    return court_file_class;
  }

  public void setCourt_file_class(String court_file_class) {
    this.court_file_class = court_file_class;
  }

  public String getCourt_file_designation() {
    return court_file_designation;
  }

  public void setCourt_file_designation(String court_file_designation) {
    this.court_file_designation = court_file_designation;
  }

  public String getCourt_file_sworn_date() {
    return court_file_sworn_date;
  }

  public void setCourt_file_sworn_date(String court_file_sworn_date) {
    this.court_file_sworn_date = court_file_sworn_date;
  }

  public String getOffence_description_list() {
    return offence_description_list;
  }

  public void setOffence_description_list(String offence_description_list) {
    this.offence_description_list = offence_description_list;
  }

  public String getCourt_file_number_seq_type() {
    return court_file_number_seq_type;
  }

  public void setCourt_file_number_seq_type(String court_file_number_seq_type) {
    this.court_file_number_seq_type = court_file_number_seq_type;
  }

  public String getCourt_home_registry() {
    return court_home_registry;
  }

  public void setCourt_home_registry(String court_home_registry) {
    this.court_home_registry = court_home_registry;
  }

  public String getCourt_home_registry_name() {
    return court_home_registry_name;
  }

  public void setCourt_home_registry_name(String court_home_registry_name) {
    this.court_home_registry_name = court_home_registry_name;
  }

  public String getCourt_home_registry_identifier() {
    return court_home_registry_identifier;
  }

  public void setCourt_home_registry_identifier(String court_home_registry_identifier) {
    this.court_home_registry_identifier = court_home_registry_identifier;
  }

  public List<String> getCase_flags() {
    return case_flags;
  }

  public void setCase_flags(List<String> case_flags) {
    this.case_flags = case_flags;
  }

  public String getAccused_names() {
    return accused_names;
  }

  public void setAccused_names(String accused_names) {
    this.accused_names = accused_names;
  }

  public List<CaseAccused> getAccused_persons() {
    return accused_persons;
  }
  public void setAccused_persons(List<CaseAccused> accused_person) {
    this.accused_persons = accused_person;
  }
  public List<ChargeAssessmentDataRef> getRelated_agency_file() {
    return related_agency_file;
  }
  public void setRelated_agency_file(List<ChargeAssessmentDataRef> related_agency_file) {
    this.related_agency_file = related_agency_file;
  }

  public ChargeAssessmentDataRef getPrimary_agency_file() {
    return primary_agency_file;
  }

  public void setPrimary_agency_file(ChargeAssessmentDataRef primary_agency_file) {
    this.primary_agency_file = primary_agency_file;
  }

  public String getApproving_crown_agency_name() {
    return approving_crown_agency_name;
  }

  public void setApproving_crown_agency_name(String approving_crown_agency_name) {
    this.approving_crown_agency_name = approving_crown_agency_name;
  }

  public String getApproving_crown_agency_ident() {
    return approving_crown_agency_ident;
  }

  public void setApproving_crown_agency_ident(String approving_crown_agency_ident) {
    this.approving_crown_agency_ident = approving_crown_agency_ident;
  }

  public List<CourtFileDataRef> getRelated_court_file() {
    return related_court_file;
  }
  public void setRelated_court_file(List<CourtFileDataRef> related_court_file) {
    this.related_court_file = related_court_file;
  }

  public List<CourtCaseData> getRelated_court_cases() {
    return related_court_cases;
  }

  public void setRelated_court_cases(List<CourtCaseData> related_court_cases) {
    this.related_court_cases = related_court_cases;
  }

}
  