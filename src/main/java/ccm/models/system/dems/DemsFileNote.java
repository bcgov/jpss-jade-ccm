package ccm.models.system.dems;

import ccm.models.common.data.FileNote;
import ccm.models.common.event.FileNoteEvent;

public class DemsFileNote {

    private FileNoteEvent fileNoteEvent;

    public DemsFileNote() {

    }
    public DemsFileNote(FileNote fNote, FileNoteEvent fNoteEvent) {
        fileNote= fNote;
        fileNoteEvent = fNoteEvent;
    }
    public FileNoteEvent getFileNoteEvent() {
        return fileNoteEvent;
    }
    public void setFileNoteEvent(FileNoteEvent fileNoteEvent) {
        this.fileNoteEvent = fileNoteEvent;
    }
    private FileNote fileNote;
    public FileNote getFileNote() {
        return fileNote;
    }
    public void setFileNote(FileNote fileNote) {
        this.fileNote = fileNote;
    }
    
}
