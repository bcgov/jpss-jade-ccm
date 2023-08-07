package ccm.models.common.data;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinCrownAssignmentList;
import ccm.models.system.justin.JustinCrownAssignmentData;

public class CaseCrownAssignmentList {
  public static final String DASH_STRING = " - ";
  public static final String SEMICOLON_STRING = "; ";
  public static final String COMMA_STRING = ", ";
  public static final String ASSIGNMENT_TYPE_LST = "LST";

  private String mdoc_justin_no;
  private List<String> legalStaffAssignmentList;
  private List<String> crownAssignmentList;
  private List<String> crownAssignmentName;

  private List<CaseCrownAssignmentData> crown_assignment;

  public CaseCrownAssignmentList() {
  }

  public CaseCrownAssignmentList(JustinCrownAssignmentList jasl) {
    setMdoc_justin_no(jasl.getMdoc_justin_no());


    List<CaseCrownAssignmentData> appearanceList = new ArrayList<CaseCrownAssignmentData>();
    List<String> legalStaffAssignments = new ArrayList<String>();
    List<String> crownAssignments = new ArrayList<String>();
    List<String> crownAssignmentName = new ArrayList<String>();

    if(jasl.getCrown_assignment() != null) {
      for (JustinCrownAssignmentData jas : jasl.getCrown_assignment()) {
        CaseCrownAssignmentData bcas = new CaseCrownAssignmentData(jas);
        appearanceList.add(bcas);

        if(jas.getAssign_type_code() != null) {

        }

        /*
        Legal Staff Name and Assignment List: For every crown assignment If($MAPID45="LST" then "$MAPID44 - $MAPID45")	Jones, Keith - LST; Smith, John - LST
        Crown Name and Assignment List: For every crown assignment If($MAPID45!="LST" then "$MAPID44 - $MAPID45")	Rhodes, Matt - AHR; Brown, James - B
        */
        if(ASSIGNMENT_TYPE_LST.equals(jas.getAssign_type_code())) {
          StringBuilder legalStaffAssignment = new StringBuilder(jas.getCrown_staff_name());

          legalStaffAssignment.append(DASH_STRING);
          legalStaffAssignment.append(jas.getAssign_type_code());
          legalStaffAssignments.add(legalStaffAssignment.toString());
        }
        else {
          StringBuilder crownAssignment = new StringBuilder();
          StringBuilder crownAssignmentNm = new StringBuilder();

          crownAssignment.append(jas.getCrown_staff_name());
          crownAssignment.append(DASH_STRING);
          crownAssignment.append(jas.getAssign_type_code());
          crownAssignments.add(crownAssignment.toString());
          // need to split name by comma and put first name, then surname
          if(jas.getCrown_staff_name() != null) {
            String[] name = jas.getCrown_staff_name().split(COMMA_STRING);
            if(name.length>1) {
              crownAssignmentNm.append(name[1]);
              crownAssignmentNm.append(" ");
            }
            crownAssignmentNm.append(name[0]);
          }
          crownAssignmentName.add(crownAssignmentNm.toString());
        }

      }
    }

    setCrown_assignment(appearanceList);
    setCrownAssignmentList(crownAssignments);
    setCrownAssignmentName(crownAssignmentName);
    setLegalStaffAssignmentList(legalStaffAssignments);
  }



  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }
  public List<String> getLegalStaffAssignmentList() {
    return legalStaffAssignmentList;
  }

  public void setLegalStaffAssignmentList(List<String> legalStaffAssignmentList) {
    this.legalStaffAssignmentList = legalStaffAssignmentList;
  }

  public List<String> getCrownAssignmentList() {
    return crownAssignmentList;
  }

  public void setCrownAssignmentList(List<String> crownAssignmentList) {
    this.crownAssignmentList = crownAssignmentList;
  }

  public List<String> getCrownAssignmentName() {
    return crownAssignmentName;
  }

  public void setCrownAssignmentName(List<String> crownAssignmentName) {
    this.crownAssignmentName = crownAssignmentName;
  }

  public List<CaseCrownAssignmentData> getCrown_assignment() {
    return crown_assignment;
  }
  public void setCrown_assignment(List<CaseCrownAssignmentData> crown_assignment) {
    this.crown_assignment = crown_assignment;
  }

}