package ccm.models.system.justin;

import java.util.List;

public class JustinCrownAssignmentList {
  private String mdoc_justin_no;

  private List<JustinCrownAssignmentData> crown_assignment;

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }
  public List<JustinCrownAssignmentData> getCrown_assignment() {
    return crown_assignment;
  }
  public void setCrown_assignment(List<JustinCrownAssignmentData> crown_assignment) {
    this.crown_assignment = crown_assignment;
  }

}