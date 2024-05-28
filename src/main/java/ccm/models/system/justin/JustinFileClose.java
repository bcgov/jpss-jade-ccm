package ccm.models.system.justin;

public class JustinFileClose {

    public static final  String  SEMA = "SEMA";  // Semi-Active
    public static final  String  PEND = "PEND";  // Pending
    public static final  String  NPRQ = "NPRQ";  // No Process Required
    public static final  String  DEST = "DEST";  // Destroyed
    public static final  String  RETN = "RETN";  // Returned
    public static final  String  ACTIVE = "ACTIVE";

    
    
    private String mdoc_justin_no;
    private String rms_event_type;
   
    private String rms_event_date;

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
