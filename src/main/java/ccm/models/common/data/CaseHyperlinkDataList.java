package ccm.models.common.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import ccm.models.system.dems.DemsLookupSearchData;


public class CaseHyperlinkDataList {
    private List<CaseHyperlinkData> case_hyperlinks ;
    private List<CaseHyperlinkData> case_hyperlinks_test = new ArrayList<CaseHyperlinkData>();
    public CaseHyperlinkDataList() {
        case_hyperlinks = new ArrayList<CaseHyperlinkData>();
    }

    public CaseHyperlinkDataList(CommonCaseList rccList) {
        case_hyperlinks = new ArrayList<CaseHyperlinkData>();
        for(String rcc : rccList.getKeys()) {
            CaseHyperlinkData hyperlinkData = new CaseHyperlinkData();
            hyperlinkData.setRcc_id(rcc);
            case_hyperlinks.add(hyperlinkData);
        }
    }

    public CaseHyperlinkDataList(CaseHyperlinkData body) {
       case_hyperlinks.add(body);    
    }

    public void processHyperlinks(String prefix, String suffix, List<Map<String, Object>> rccList) {
        for(CaseHyperlinkData data : case_hyperlinks) {
            String key = data.getRcc_id();
            for (Map<String, Object> item : rccList) {
                String itemKey = (String) item.get("key");
                if (key.equals(itemKey)) {
                    Integer id = (Integer) item.get("id");

                    data.setHyperlink(prefix + id + suffix);
                    data.setRcc_id(key.toString());
                    data.setMessage("Case found.");
                    break;
                }
            }
            if(data.getMessage() == null) {
                data.setMessage("Case not found.");
            }
        }
    }

    public void processCaseHyperlinks(String prefix, String suffix, List<DemsLookupSearchData> rccList) {
        for(CaseHyperlinkData data : case_hyperlinks) {
            String key = data.getRcc_id();
            for (DemsLookupSearchData item : rccList) {
                String itemKey = item.getKey();
                if (key.equals(itemKey)) {
                    Integer id = (Integer) item.getId();

                    data.setHyperlink(prefix + id + suffix);
                    data.setRcc_id(key.toString());
                    data.setMessage("Case found.");
                    break;
                }
            }
            if(data.getMessage() == null) {
                data.setMessage("Case not found.");
            }
        }
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