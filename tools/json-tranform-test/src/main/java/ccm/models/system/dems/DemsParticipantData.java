package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsParticipantData {

    private String key;
    private String name;
    private String firstName;
    private String lastName;
    private List<DemsFieldData> fields;
    private DemsAddressData address;

    public DemsParticipantData() {
    }

    public DemsParticipantData(BusinessCourtCaseAccused ba) {
        setKey(ba.getIdentifier());
        setName(ba.getFull_name());
        setLastName(ba.getSurname());
        setFirstName(ba.getFirst_name());

        DemsFieldData partId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PART_ID.getId(), DemsFieldData.FIELD_MAPPINGS.PART_ID.getLabel(), ba.getIdentifier());
        DemsFieldData dob = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.DATE_OF_BIRTH.getId(), DemsFieldData.FIELD_MAPPINGS.DATE_OF_BIRTH.getLabel(), ba.getBirth_date());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        fieldData.add(partId);
        fieldData.add(dob);

        setFields(fieldData);
        setAddress(new DemsAddressData(null));
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

    public DemsAddressData getAddress() {
        return address;
    }

    public void setAddress(DemsAddressData address) {
        this.address = address;
    }

}
