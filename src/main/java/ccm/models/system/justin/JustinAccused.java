package ccm.models.system.justin;
public class JustinAccused {
    private String part_id;
    private String accused_name;
    private String proposed_process_type;
    private String proposed_appr_date;

    private String crown_decision;
    private String offence_date;

    private String indigenous_yn;
    private String birth_date;
    
    public String getPart_id() {
      return part_id;
    }
    public void setPart_id(String part_id) {
      this.part_id = part_id;
    }
    public String getAccused_name() {
      return accused_name;
    }
    public void setAccused_name(String accused_name) {
      this.accused_name = accused_name;
    }
    public String getProposed_process_type() {
      return proposed_process_type;
    }
    public void setProposed_process_type(String proposed_process_type) {
      this.proposed_process_type = proposed_process_type;
    }
    public String getProposed_appr_date() {
      return proposed_appr_date;
    }
    public void setProposed_appr_date(String proposed_appr_date) {
      this.proposed_appr_date = proposed_appr_date;
    }
    public String getCrown_decision() {
      return crown_decision;
    }
    public void setCrown_decision(String crown_decision) {
      this.crown_decision = crown_decision;
    }
    public String getOffence_date() {
      return offence_date;
    }
    public void setOffence_date(String offence_date) {
      this.offence_date = offence_date;
    }

    public String getIndigenous_yn() {
      return indigenous_yn;
    }
    public void setIndigenous_yn(String indigenous_yn) {
      this.indigenous_yn = indigenous_yn;
    }
    public String getBirth_date() {
      return birth_date;
    }
    public void setBirth_date(String birth_date) {
      this.birth_date = birth_date;
    }
    
  }
  