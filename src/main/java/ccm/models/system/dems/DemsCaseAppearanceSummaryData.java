package ccm.models.system.dems;

import java.util.ArrayList;
import java.util.List;

import ccm.models.common.data.CaseAppearanceSummary;
import ccm.models.common.data.CaseAppearanceSummaryList;
import ccm.utils.DateTimeUtils;
import java.time.ZonedDateTime;

public class DemsCaseAppearanceSummaryData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsCaseAppearanceSummaryData() {
    }

    public DemsCaseAppearanceSummaryData(String key, String name, CaseAppearanceSummaryList commonList) {
        setKey(key);
        setName(name);

        if (commonList != null && commonList.getApprsummary() != null && commonList.getApprsummary().size() > 0) {

            ZonedDateTime earliestInitialApprDt = null;
            String earliestInitialApprRsn = null;
            ZonedDateTime earliestNextApprDt = null;
            String earliestNextApprRsn = null;
            ZonedDateTime earliestTrialStartApprDt = null;
            String earliestTrialStartApprRsn = null;

            for(CaseAppearanceSummary bcas : commonList.getApprsummary()) {

                if(bcas.getInitial_appr_dtm() != null && !bcas.getInitial_appr_dtm().isEmpty()) {
                    ZonedDateTime currentDtmObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(bcas.getInitial_appr_dtm());
                    if (currentDtmObj != null) {
                        if (earliestInitialApprDt == null || currentDtmObj.isBefore(earliestInitialApprDt)) {
                            earliestInitialApprDt = currentDtmObj;
                            earliestInitialApprRsn = bcas.getInitial_appr_rsn_cd();
                        }
                    }

                }

                if(bcas.getNext_appr_dtm() != null && !bcas.getNext_appr_dtm().isEmpty()) {
                    ZonedDateTime currentDtmObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(bcas.getNext_appr_dtm());
                    if (currentDtmObj != null) {
                        if (earliestNextApprDt == null || currentDtmObj.isBefore(earliestNextApprDt)) {
                            earliestNextApprDt = currentDtmObj;
                            earliestNextApprRsn = bcas.getNext_appr_rsn_cd();
                        }
                    }

                }

                if(bcas.getTrial_start_appr_dtm() != null && !bcas.getTrial_start_appr_dtm().isEmpty()) {
                    ZonedDateTime currentDtmObj = DateTimeUtils.convertToZonedDateTimeFromBCDateTimeString(bcas.getTrial_start_appr_dtm());
                    if (currentDtmObj != null) {
                        if (earliestTrialStartApprDt == null || currentDtmObj.isBefore(earliestTrialStartApprDt)) {
                            earliestTrialStartApprDt = currentDtmObj;
                            earliestTrialStartApprRsn = bcas.getTrial_start_appr_rsn_cd();
                        }
                    }

                }

            }

            DemsFieldData initialApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_DT.getLabel(), DateTimeUtils.convertToUtcFromZonedDateTime(earliestInitialApprDt));
            DemsFieldData  initialApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.INITIAL_APP_REASON.getLabel(), earliestInitialApprRsn);
            DemsFieldData  nextApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.NEXT_APP_DT.getLabel(), DateTimeUtils.convertToUtcFromZonedDateTime(earliestNextApprDt));
            DemsFieldData  nextApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.NEXT_APP_REASON.getLabel(), earliestNextApprRsn);
            DemsFieldData  trialStartApprDt = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_DT.getLabel(), DateTimeUtils.convertToUtcFromZonedDateTime(earliestTrialStartApprDt));
            DemsFieldData  trialStartApprRsnCd = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.FIRST_TRIAL_REASON.getLabel(), earliestTrialStartApprRsn);


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
