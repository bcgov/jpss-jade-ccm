package ccm.models.common.data;

public class FileNote {
    private String file_note_id;
    private String user_name;
    private String entry_date;
    private String note_text;
    private String rcc_id;
    private String mdoc_justin_no;

    public String getFile_note_id() {
        return file_note_id;
    }
    public void setFile_note_id(String file_note_id) {
        this.file_note_id = file_note_id;
    }
    public String getUser_name() {
        return user_name;
    }
    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }
    public String getEntry_date() {
        return entry_date;
    }
    public void setEntry_date(String entry_date) {
        this.entry_date = entry_date;
    }
    public String getNote_text() {
        return note_text;
    }
    public void setNote_text(String note_text) {
        this.note_text = note_text;
    }
    public String getRcc_id() {
        return rcc_id;
    }
    public void setRcc_id(String rcc_id) {
        this.rcc_id = rcc_id;
    }
    public String getMdoc_justin_no() {
        return mdoc_justin_no;
    }
    public void setMdoc_justin_no(String mdoc_justin_no) {
        this.mdoc_justin_no = mdoc_justin_no;
    }

}
