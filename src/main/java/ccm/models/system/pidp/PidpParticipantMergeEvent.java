package ccm.models.system.pidp;

import ccm.models.common.event.ParticipantMergeEvent;
import ccm.utils.DateTimeUtils;

public class PidpParticipantMergeEvent {
  private String mergeEventTime;
  private String sourceParticipantId; // To be inactivated
  private String targetParticipantId; // Primary

  public PidpParticipantMergeEvent(ParticipantMergeEvent pme) {
    setMergeEventTime(DateTimeUtils.generateCurrentDtm());
    setSourceParticipantId(pme.getJustin_from_part_id());
    setTargetParticipantId(pme.getJustin_to_part_id());
  }

  public String getMergeEventTime() {
    return mergeEventTime;
  }
  public void setMergeEventTime(String mergeEventTime) {
    this.mergeEventTime = mergeEventTime;
  }
  public String getSourceParticipantId() {
    return sourceParticipantId;
  }
  public void setSourceParticipantId(String sourceParticipantId) {
    this.sourceParticipantId = sourceParticipantId;
  }
  public String getTargetParticipantId() {
    return targetParticipantId;
  }
  public void setTargetParticipantId(String targetParticipantId) {
    this.targetParticipantId = targetParticipantId;
  }

}
  