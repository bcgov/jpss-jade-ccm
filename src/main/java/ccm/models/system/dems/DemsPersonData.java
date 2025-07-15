package ccm.models.system.dems;

import java.util.List;
import java.util.Random;

import java.util.ArrayList;

import ccm.models.common.data.CaseAccused;

public class DemsPersonData {

    private String id;
    private String key;
    //private String name;
    private String firstName;
    private String lastName;
    private String dob;
    private List<DemsFieldData> fields;
    private DemsAddressData address;

    public DemsPersonData() {
    }

    public DemsPersonData(CaseAccused ca) {
        setKey(ca.getIdentifier());
        setLastName(ca.getSurname());
        setFirstName(ca.getGiven_1_name());
        setDob(ca.getBirth_date());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();

        DemsFieldData given2 = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_GIVEN_NAME_2.getLabel(), ca.getGiven_2_name());
        fieldData.add(given2);

        DemsFieldData given3 = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_GIVEN_NAME_3.getLabel(), ca.getGiven_3_name());
        fieldData.add(given3);

        String fullGivenNamesAndLastNameString = generateFullGivenNamesAndLastNameFromAccused(ca);

        // check birth date info
        if (ca.getBirth_date() != null && !ca.getBirth_date().isEmpty()) {
            // append birth date to full name string
            fullGivenNamesAndLastNameString = fullGivenNamesAndLastNameString + "  (" + ca.getBirth_date() + ")";
        }

        DemsFieldData fullName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_FULL_NAME.getLabel(),
            fullGivenNamesAndLastNameString);

        fieldData.add(fullName);

        //setName(fullGivenNamesAndLastNameString);
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

    /*public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }*/

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

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
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

    public static String generateFullGivenNamesAndLastNameFromAccused(CaseAccused accused) {
        String concatenated_name_string = accused.getGiven_1_name() + 
            (accused.getGiven_2_name() != null && accused.getGiven_2_name().length() > 0 ? " " + accused.getGiven_2_name() : "" ) +
            (accused.getGiven_3_name() != null && accused.getGiven_3_name().length() > 0 ? " " + accused.getGiven_3_name() : "" ) + 
            " " + accused.getSurname();

        return concatenated_name_string;
    }

    public void setotcpin(String value, DemsPersonData d ) {
        List<DemsFieldData> fieldData = d.getFields();
        if(!value.isEmpty()){
            System.out.println("OTC :" + value);
            DemsFieldData otc = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OTC_PIN.getLabel(),value);
            fieldData.add(otc);
        }
    }

    public void generateOTC() {
        Random r = new Random();
        int low = 0000;
        int high = 999999;
        int random = r.nextInt(high-low) + low;
        String formatted = String.format("%06d", random);
        //System.out.println("Random Pin number generation: " + formatted);

        List<DemsFieldData> fieldData = getFields();
        DemsFieldData otc = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OTC_PIN.getLabel(),formatted);
        fieldData.add(otc);
    }

    public void generateMergedParticipantKeys(String participantKeys) {
        List<DemsFieldData> fieldData = getFields();
        DemsFieldData otc = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.MERGED_PARTICIPANT_KEYS.getLabel(), participantKeys);
        fieldData.add(otc);
        
    }

}
