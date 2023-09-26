package ccm.models.common.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class CaseHyperlinkDataList {
    private List<CaseHyperlinkData> case_hyperlinks ;
    private List<CaseHyperlinkData> case_hyperlinks_test = new ArrayList<CaseHyperlinkData>();
    public CaseHyperlinkDataList() {
        case_hyperlinks = new ArrayList<CaseHyperlinkData>();
    }

    public CaseHyperlinkDataList(CaseHyperlinkData body) {
       case_hyperlinks.add(body);    
    }
       
    public List<CaseHyperlinkData> getcase_hyperlinks() {
        return case_hyperlinks;
    }

    public void setcase_hyperlinks(List<CaseHyperlinkData> caseHyperlink) {
        this.case_hyperlinks = caseHyperlink;
    }

    public void addCaseHyperlinkData(List<CaseHyperlinkData> test) {

        Iterator<CaseHyperlinkData> i = test.iterator();
        while (i != null && i.hasNext()) {
            case_hyperlinks_test.add(i.next());
          }
        setcase_hyperlinks(case_hyperlinks_test);
    }
}