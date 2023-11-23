package ccm.models.common.data;

import ccm.models.system.justin.JustinCourtFileRef;

public class CourtFileDataRef {
    private String mdoc_justin_no;
    private String mdoc_relation_type_cd;

    public CourtFileDataRef() {
    }

    public CourtFileDataRef(JustinCourtFileRef jafr) {
        setMdoc_justin_no(jafr.getMdoc_justin_no());
        setMdoc_relation_type_cd(jafr.getMdoc_relation_type_cd());
    }

    public String getMdoc_justin_no() {
        return mdoc_justin_no;
    }
    public void setMdoc_justin_no(String mdoc_justin_no) {
        this.mdoc_justin_no = mdoc_justin_no;
    }
    public String getMdoc_relation_type_cd() {
        return mdoc_relation_type_cd;
    }
    public void setMdoc_relation_type_cd(String mdoc_relation_type_cd) {
     this.mdoc_relation_type_cd = mdoc_relation_type_cd;
    }
}
