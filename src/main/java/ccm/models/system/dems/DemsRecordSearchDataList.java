package ccm.models.system.dems;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


@JsonIgnoreProperties(ignoreUnknown = true)
public class DemsRecordSearchDataList {

    private String totalRows;
    private String totalHits;
    private String searchHistoryId;
    private List<DemsRecordSearchData> items;

    public String getTotalRows() {
        return totalRows;
    }
    public void setTotalRows(String totalRows) {
        this.totalRows = totalRows;
    }
    public String getTotalHits() {
        return totalHits;
    }
    public void setTotalHits(String totalHits) {
        this.totalHits = totalHits;
    }
    public String getSearchHistoryId() {
        return searchHistoryId;
    }
    public void setSearchHistoryId(String searchHistoryId) {
        this.searchHistoryId = searchHistoryId;
    }
    public List<DemsRecordSearchData> getItems() {
        return items;
    }
    public void setItems(List<DemsRecordSearchData> items) {
        this.items = items;
    }

}
