package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.List;

import ccm.models.business.BusinessCourtCaseAppearanceSummaryList;
import ccm.utils.DateTimeConverter;
import ccm.models.business.BusinessCourtCaseAppearanceSummary;

public class DemsCourtCaseAppearanceSummaryData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsCourtCaseAppearanceSummaryData() {
    }

    public DemsCourtCaseAppearanceSummaryData(String key, String name, BusinessCourtCaseAppearanceSummaryList b) {
        setKey(key);
        setName(name);

        if (b != null && b.getApprsummary() != null && b.getApprsummary().size() > 0) {
            BusinessCourtCaseAppearanceSummary bcas = b.getApprsummary().get(0);

            DemsFieldData initialApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_DT.getLabel(), DateTimeConverter.convertToUtcFromBCDateTimeString(bcas.getInitial_appr_dtm()));
            DemsFieldData initialApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_REASON.getLabel(), bcas.getInitial_appr_rsn_cd());
            DemsFieldData nextApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.NEXT_APP_DT.getLabel(), DateTimeConverter.convertToUtcFromBCDateTimeString(bcas.getNext_appr_dtm()));
            DemsFieldData nextApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.NEXT_APP_REASON.getLabel(), bcas.getNext_appr_rsn_cd());
            DemsFieldData trialStartApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_DT.getLabel(), DateTimeConverter.convertToUtcFromBCDateTimeString(bcas.getTrial_start_appr_dtm()));
            DemsFieldData trialStartApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_REASON.getLabel(), bcas.getTrial_start_appr_rsn_cd());

            List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
            fieldData.add(initialApprDt);
            fieldData.add(initialApprRsnCd);
            fieldData.add(nextApprDt);
            fieldData.add(nextApprRsnCd);
            fieldData.add(trialStartApprDt);
            fieldData.add(trialStartApprRsnCd);
            setFields(fieldData);
        }
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<DemsFieldData> getFields() {
        return fields;
    }

    public void setFields(List<DemsFieldData> fields) {
        this.fields = fields;
    }

}
