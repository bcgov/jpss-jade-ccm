package ccm.models.business;

import ccm.models.system.justin.JustinAccused;
import ccm.models.system.justin.JustinAgencyFile;
import ccm.models.system.justin.JustinCourtFile;

import java.util.ArrayList;
import java.util.List;

public class BusinessCourtCaseMetadataData {
  public static final String DASH_STRING = "-";
  public static final String COLON_STRING = "-";
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
  //private String rms_processing_status;
  private List<String> case_flags;

  private List<BusinessCourtCaseAccused> accused_person;
  private List<BusinessCourtCaseData> related_agency_file;
  private List<BusinessCourtCaseMetadataData> related_court_file;

  public BusinessCourtCaseMetadataData() {
  }

  public BusinessCourtCaseMetadataData(JustinCourtFile jcf) {
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
      courtHomeReg.append(DASH_STRING);
      courtHomeReg.append(jcf.getHome_court_agency_name());
      setCourt_home_registry(courtHomeReg.toString());
      setCourt_home_registry_name(jcf.getHome_court_agency_name());


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
      if(jcf.getMdocaccused() != null) {
        for (JustinAccused accused : jcf.getMdocaccused()) {
            if (YES_STRING.equalsIgnoreCase(accused.getIndigenous_yn())) {
                case_flags.add("Indigenous");
                break;
            }
        }
      }

      List<BusinessCourtCaseAccused> accusedList = new ArrayList<BusinessCourtCaseAccused>();

      if(jcf.getMdocaccused() != null) {
        for (JustinAccused ja : jcf.getMdocaccused()) {

          BusinessCourtCaseAccused accused = new BusinessCourtCaseAccused(ja);
          accusedList.add(accused);
        }
      }
      setAccused_person(accusedList);


      List<BusinessCourtCaseData> agencyList = new ArrayList<BusinessCourtCaseData>();

      if(jcf.getRelated_rcc() != null) {
        for (JustinAgencyFile jaf : jcf.getRelated_rcc()) {

          BusinessCourtCaseData agency = new BusinessCourtCaseData(jaf);
          agencyList.add(agency);
        }
      }
      setRelated_agency_file(agencyList);


      List<BusinessCourtCaseMetadataData> relatedList = new ArrayList<BusinessCourtCaseMetadataData>();

      if(jcf.getRelated_court_file() != null) {
        for (JustinCourtFile rjcf : jcf.getRelated_court_file()) {

          BusinessCourtCaseMetadataData related = new BusinessCourtCaseMetadataData(rjcf);
          relatedList.add(related);
        }
      }
      setRelated_court_file(relatedList);

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

  public List<String> getCase_flags() {
    return case_flags;
  }

  public void setCase_flags(List<String> case_flags) {
    this.case_flags = case_flags;
  }

  public List<BusinessCourtCaseAccused> getAccused_person() {
    return accused_person;
  }
  public void setAccused_person(List<BusinessCourtCaseAccused> accused_person) {
    this.accused_person = accused_person;
  }
  public List<BusinessCourtCaseData> getRelated_agency_file() {
    return related_agency_file;
  }
  public void setRelated_agency_file(List<BusinessCourtCaseData> related_agency_file) {
    this.related_agency_file = related_agency_file;
  }
  public List<BusinessCourtCaseMetadataData> getRelated_court_file() {
    return related_court_file;
  }
  public void setRelated_court_file(List<BusinessCourtCaseMetadataData> related_court_file) {
    this.related_court_file = related_court_file;
  }

}
  