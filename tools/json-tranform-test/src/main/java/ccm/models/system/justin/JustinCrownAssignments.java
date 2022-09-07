package ccm.models.system.justin;

import java.util.List;

public class JustinCrownAssignments {
  private String mdoc_justin_no;

  private List<JustinCrownAssignment> crown_assignment;

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }
  public List<JustinCrownAssignment> getCrown_assignment() {
    return crown_assignment;
  }
  public void setCrown_assignment(List<JustinCrownAssignment> crown_assignment) {
    this.crown_assignment = crown_assignment;
  }

}