package ccm.models.system.dems;

import java.util.List;
import java.util.Random;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import ccm.models.common.data.CaseAccused;
@JsonIgnoreProperties(ignoreUnknown = true)
public class DemsPersonData {

    private String id;
    private String key;
    private String name;
    private String firstName;
    private String lastName;
    private List<DemsFieldData> fields;
    private DemsAddressData address;
    private List<DemsOrganisationData> orgs;
    private String title;

    public DemsPersonData() {
    }

    public DemsPersonData(CaseAccused ca) {
        setKey(ca.getIdentifier());
        setLastName(ca.getSurname());
        setFirstName(ca.getGiven_1_name());

        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        
        // BCPSDEMS-602 - workaround to not provide the field data if date is null
        if (ca.getBirth_date() != null) {
            DemsFieldData dob = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PERSON_DATE_OF_BIRTH.getLabel(), ca.getBirth_date());
            fieldData.add(dob);
        }

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

        setName(fullGivenNamesAndLastNameString);
        setFields(fieldData);
        setAddress(new DemsAddressData(null));
    }

    public DemsPersonData(String json) throws JsonMappingException, JsonProcessingException {
        String prefix = "";String suffix = "";
                          ObjectMapper mapper = new ObjectMapper();
                          JsonNode rootNode = mapper.readTree(json);
                  
                          // Navigate to the desired JSON path
                          JsonNode node = rootNode.at("");
                          System.out.println("node :"+node);
                          // Convert the node to a string
                          String value = node.toString();
                          System.out.println("value :"+value);
                          
                          // Add prefix and suffix if provided
                          if (!prefix.isEmpty()) {
                              value = prefix + value;
                          }
                          if (!suffix.isEmpty()) {
                              value = value + suffix;
                          }
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
    public void generateOTC(DemsPersonData d ) {
        Random r = new Random();
        int low = 0000;
        int high = 999999;
        int random = r.nextInt(high-low) + low;
        System.out.println("Random Pin number generation" + random);

        List<DemsFieldData> fieldData = d.getFields();
        DemsFieldData otc = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.OTC_PIN.getLabel(),random);
        fieldData.add(otc);
        
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
