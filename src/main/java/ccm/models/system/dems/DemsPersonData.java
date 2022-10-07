package ccm.models.system.dems;

import java.util.List;
import java.util.ArrayList;
import ccm.models.business.BusinessCourtCaseAccused;

public class DemsPersonData {

    private String id;
    private String key;
    private String name;
    private String firstName;
    private String lastName;
    private List<DemsFieldData> fields;
    private DemsAddressData address;
    private List<DemsOrganisationData> orgs;

    public DemsPersonData() {
    }

    public DemsPersonData(BusinessCourtCaseAccused ba) {
        setKey(ba.getIdentifier());
        setName(ba.getName());
        setLastName(ba.getSurname());
        setFirstName(ba.getGiven_1_name());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        if(ba.getBirth_date() != null && ba.getBirth_date().length() > 0) {
            DemsFieldData dob = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_DATE_OF_BIRTH.getId(), DemsFieldData.FIELD_MAPPINGS.PERSON_DATE_OF_BIRTH.getLabel(), ba.getBirth_date());
            fieldData.add(dob);
        }
        if(ba.getGiven_2_name() != null && ba.getGiven_2_name().length() > 0) {
            DemsFieldData given2 = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_GIVEN_NAME_2.getId(), DemsFieldData.FIELD_MAPPINGS.PERSON_GIVEN_NAME_2.getLabel(), ba.getGiven_2_name());
            fieldData.add(given2);
        }
        if(ba.getGiven_3_name() != null && ba.getGiven_3_name().length() > 0) {
            DemsFieldData given3 = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_GIVEN_NAME_3.getId(), DemsFieldData.FIELD_MAPPINGS.PERSON_GIVEN_NAME_3.getLabel(), ba.getGiven_3_name());
            fieldData.add(given3);
        }

        DemsFieldData fullName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_FULL_NAME.getId(), DemsFieldData.FIELD_MAPPINGS.PERSON_FULL_NAME.getLabel(), 
            ba.getGiven_1_name() + 
            (ba.getGiven_2_name() != null && ba.getGiven_2_name().length() > 0 ? " " + ba.getGiven_2_name() : "" ) +
            (ba.getGiven_3_name() != null && ba.getGiven_3_name().length() > 0 ? " " + ba.getGiven_3_name() : "" ) + 
            " " + ba.getSurname()
            );
        fieldData.add(fullName);

        setFields(fieldData);
        setAddress(new DemsAddressData(null));
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public List<DemsOrganisationData> getOrgs() {
        return orgs;
    }

    public void setOrgs(List<DemsOrganisationData> orgs) {
        this.orgs = orgs;
    }

}
