package ccm.models.system.dems;

public class DemsCaseGroupMembersSyncHelper {
  private Long caseGroupId;
  private DemsCaseGroupMembersSyncData syncData;

  public DemsCaseGroupMembersSyncHelper() {
  }

  public DemsCaseGroupMembersSyncHelper(Long caseGroupId, DemsCaseGroupMembersSyncData syncData) {
    this.caseGroupId = caseGroupId;
    this.syncData = syncData;
  }

  public Long getCaseGroupId() {
    return caseGroupId;
  }

  public void setCaseGroupId(Long caseGroupId) {
    this.caseGroupId = caseGroupId;
  }

  public DemsCaseGroupMembersSyncData getSyncData() {
    return syncData;
  }

  public void setSyncData(DemsCaseGroupMembersSyncData syncData) {
    this.syncData = syncData;
  }
}
  