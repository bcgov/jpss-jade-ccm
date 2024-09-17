package ccm.models.common.data;

import ccm.models.system.justin.JustinFileNote;

public class FileNote {
    private String file_note_id;
    private String user_name;
    private String entry_date;
    private String note_txt;
    private String rcc_id;
    private String mdoc_justin_no;
    private String original_file_number;

    public FileNote() {
    }

    public FileNote(JustinFileNote justinNote) {
        this.mdoc_justin_no = justinNote.getMdoc_justin_no();
        this.file_note_id = justinNote.getFile_note_id();
        this.user_name = justinNote.getUser_name();
        this.entry_date = justinNote.getEntry_date();
        this.note_txt = justinNote.getNote_txt();
        this.rcc_id = justinNote.getRcc_id();
    }

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
    public String getNote_txt() {
        return note_txt;
    }
    public void setNote_txt(String note_txt) {
        this.note_txt = note_txt;
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
    public String getOriginal_file_number(){
        return original_file_number;
    }
    public void setOriginal_file_number(String original_file_number){
        this.original_file_number = original_file_number;
    }
}
