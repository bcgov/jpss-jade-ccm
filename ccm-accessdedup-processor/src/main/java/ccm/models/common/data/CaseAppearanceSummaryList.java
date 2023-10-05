package ccm.models.common.data;

import java.util.List;
import java.util.ArrayList;
import ccm.models.system.justin.JustinCourtAppearanceSummaryList;
import ccm.models.system.justin.JustinCourtAppearanceSummary;

public class CaseAppearanceSummaryList {
  private String mdoc_justin_no;

  private List<CaseAppearanceSummary> apprsummary;

  public CaseAppearanceSummaryList() {
  }

  public CaseAppearanceSummaryList(JustinCourtAppearanceSummaryList jasl) {
    setMdoc_justin_no(jasl.getMdoc_justin_no());

    List<CaseAppearanceSummary> appearanceList = new ArrayList<CaseAppearanceSummary>();

    if(jasl.getApprsummary() != null) {
      for (JustinCourtAppearanceSummary jas : jasl.getApprsummary()) {
        CaseAppearanceSummary bcas = new CaseAppearanceSummary(jas);
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

  public List<CaseAppearanceSummary> getApprsummary() {
    return apprsummary;
  }
  public void setApprsummary(List<CaseAppearanceSummary> apprsummary) {
    this.apprsummary = apprsummary;
  }

}