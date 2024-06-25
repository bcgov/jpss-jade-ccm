package ccm.models.common.data;

public class FileDisposition {
    
    private String mdoc_justin_no;
    private String disposition_date;
    
   
    public FileDisposition() {
    }

    public FileDisposition(String mdoc_justin_no, String disposition_date) {
        this.mdoc_justin_no = mdoc_justin_no;
        this.disposition_date = disposition_date;
    }
    
    public String getDisposition_date() {
        return disposition_date;
    }
    public void setDisposition_date(String disposition_date) {
        this.disposition_date = disposition_date;
    }

    public String getMdoc_justin_no() {
        return mdoc_justin_no;
    }
    public void setMdoc_justin_no(String mdoc_justin_no) {
        this.mdoc_justin_no = mdoc_justin_no;
    }
    
}
