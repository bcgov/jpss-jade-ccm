package ccm.models.system.dems;

public class DemsCaseGroupMembersSyncHelper {
  private Long caseGroupId;
  private String caseGroupName;
  private DemsCaseGroupMembersSyncData syncData;

  public DemsCaseGroupMembersSyncHelper() {
  }

  public DemsCaseGroupMembersSyncHelper(Long caseGroupId, String caseGroupName, DemsCaseGroupMembersSyncData syncData) {
    this.caseGroupId = caseGroupId;
    this.caseGroupName = caseGroupName;
    this.syncData = syncData;
  }

  public Long getCaseGroupId() {
    return caseGroupId;
  }

  public void setCaseGroupId(Long caseGroupId) {
    this.caseGroupId = caseGroupId;
  }

  public String getCaseGroupName() {
    return caseGroupName;
  }

  public void setCaseGroupName(String caseGroupName) {
    this.caseGroupName = caseGroupName;
  }

  public DemsCaseGroupMembersSyncData getSyncData() {
    return syncData;
  }

  public void setSyncData(DemsCaseGroupMembersSyncData syncData) {
    this.syncData = syncData;
  }
}
  