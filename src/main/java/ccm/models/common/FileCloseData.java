package ccm.models.common;

public class FileCloseData {
    
    private String mdoc_justin_no;
    private String rms_event_type;
    private String rms_event_date;
    
   
    public FileCloseData(String mdoc_justin_no, String rmsEventType, String rmsEventDate) {
        this.mdoc_justin_no = mdoc_justin_no;
        this.rms_event_date = rmsEventDate;
        this.rms_event_type = rmsEventType;
    }
    
    public String getRms_event_date() {
        return rms_event_date;
    }
    public void setRms_event_date(String rms_event_date) {
        this.rms_event_date = rms_event_date;
    }

    public String getMdoc_justin_no() {
        return mdoc_justin_no;
    }
    public void setMdoc_justin_no(String mdoc_justin_no) {
        this.mdoc_justin_no = mdoc_justin_no;
    }

    public String getRms_event_type() {
        return rms_event_type;
    }
    public void setRms_event_type(String rms_event_type) {
        this.rms_event_type = rms_event_type;
    }
    
}
