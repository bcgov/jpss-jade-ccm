package ccm.models.business;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinCourtAppearanceSummaryList;
import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class BusinessCourtCaseAppearanceSummaryList {
  private String mdoc_justin_no;

  private List<BusinessCourtCaseAppearanceSummary> apprsummary;

  public BusinessCourtCaseAppearanceSummaryList() {
  }

  public BusinessCourtCaseAppearanceSummaryList(JustinCourtAppearanceSummaryList jasl) {
    setMdoc_justin_no(jasl.getMdoc_justin_no());

    List<BusinessCourtCaseAppearanceSummary> appearanceList = new ArrayList<BusinessCourtCaseAppearanceSummary>();

    if(jasl.getApprsummary() != null) {
      for (JustinCourtAppearanceSummary jas : jasl.getApprsummary()) {
        BusinessCourtCaseAppearanceSummary bcas = new BusinessCourtCaseAppearanceSummary(jas);
        appearanceList.add(bcas);
      }
    }

    setApprsummary(appearanceList);
  }

  public String getMdoc_justin_no() {
    return mdoc_justin_no;
  }
  public void setMdoc_justin_no(String mdoc_justin_no) {
    this.mdoc_justin_no = mdoc_justin_no;
  }

  public List<BusinessCourtCaseAppearanceSummary> getApprsummary() {
    return apprsummary;
  }
  public void setApprsummary(List<BusinessCourtCaseAppearanceSummary> apprsummary) {
    this.apprsummary = apprsummary;
  }

}