package ccm.models.system.dems;


public class DemsCaseParticipantData {

    private String personIdOrKey;
    private String participantType;

    public DemsCaseParticipantData() {
    }

    public DemsCaseParticipantData(String personIdOrKey, String participantType) {

        setPersonIdOrKey(personIdOrKey);
        setParticipantType(participantType);
    }

    public String getPersonIdOrKey() {
        return personIdOrKey;
    }

    public void setPersonIdOrKey(String personIdOrKey) {
        this.personIdOrKey = personIdOrKey;
    }

    public String getParticipantType() {
        return participantType;
    }

    public void setParticipantType(String participantType) {
        this.participantType = participantType;
    }

    

}
