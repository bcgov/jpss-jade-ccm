package ccm.models.system.dems;

import java.util.List;

import ccm.models.business.BusinessCourtAppearanceSummary;

public class DemsCourtCaseAppearanceSummaryData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsCourtCaseAppearanceData() {
    }

    public DemsCourtCaseAppearanceData(String key, String name, BusinessCourtAppearanceSummary bcas) {
        setName(name.substring(0, name.length() > 255 ? 254 : name.length()));
        setKey(key);


        DemsFieldData initialApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_DT.getId(), DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_DT.getLabel(), bcas.getInitial_appr_dt());
        DemsFieldData initialApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_REASON.getId(), DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_REASON.getLabel(), bcas.getInitial_appr_rsn_cd());
        DemsFieldData nextApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.NEXT_APP_DT.getId(), DemsFieldData.FIELD_MAPPINGS.NEXT_APP_DT.getLabel(), bcas.getNext_appr_dt());
        DemsFieldData nextApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.NEXT_APP_REASON.getId(), DemsFieldData.FIELD_MAPPINGS.NEXT_APP_REASON.getLabel(), bcas.getNext_appr_rsn_cd());
        DemsFieldData trialStartApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_REASON.getId(), DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_REASON.getLabel(), bcas.getTrial_start_appr_dt());
        DemsFieldData trialStartApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_REASON.getId(), DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_REASON.getLabel(), bcas.getTrial_start_appr_rsn_cd());

        List<DemsFieldData> fieldData = this.getFields();
        fieldData.add(initialApprDt);
        fieldData.add(initialApprRsnCd);
        fieldData.add(nextApprDt);
        fieldData.add(nextApprRsnCd);
        fieldData.add(trialStartApprDt);
        fieldData.add(trialStartApprRsnCd);
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
