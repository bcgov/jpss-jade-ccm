package ccm.models.system.dems;

import java.util.List;
import java.util.Set;
import java.util.Date;
import java.util.HashSet;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import ccm.models.common.data.ChargeAssessmentDataRef;
import ccm.models.common.data.CourtCaseData;
import ccm.utils.DateTimeUtils;

public class DemsApprovedCourtCaseData {
    public static final String COMMA_STRING = ",";
    public static final String SEMICOLON_SPACE_STRING = "; ";

    private String name;
    private String key;
    private List<DemsFieldData> fields;

    public DemsApprovedCourtCaseData() {
    }

    public DemsApprovedCourtCaseData(String key, String name, CourtCaseData primaryCourtCaseData, List<String> existingCaseFlags, List<CourtCaseData> courtCaseDataList) {
        setKey(key);
        setName(name);

        // Map any case flags that exist
        List<String> caseFlagList = new ArrayList<String>();
        for (String caseFlag : primaryCourtCaseData.getCase_flags()) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.name().equals(caseFlag)) {
                caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.K.getLabel());
            }
        }
        // Note: intentionally left-out the K-file flag check in following code, as the metadata overrides that value.
        for(String caseFlag : existingCaseFlags) {
            if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel())) {
                  caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel());
                }
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel());
                }//fix for JADE-2559
            } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel().equals(caseFlag)) {
                if(!caseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel())) {
                    caseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel());
                }
            }
        }
        
        //DemsFieldData rmsProcStatus = new FIELD_MAPPINGS.RMS_PROC_STAT.getLabel(), bccm.get());
        //DemsFieldData assignedLegalStaff = new FIELD_MAPPINGS.ASSIGNED_LEGAL_STAFF.getLabel(), bccm.get());
        DemsFieldData accusedName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.ACCUSED_FULL_NAME.getLabel(), primaryCourtCaseData.getAccused_names());
        DemsFieldData lastJustinUpdate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.LAST_JUSTIN_UPDATE.getLabel(), DateTimeUtils.convertToUtcFromBCDateTimeString(DateTimeUtils.generateCurrentDtm()));
        //added as part of jade-2621
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date earliestDate = null;
        try{
            if(primaryCourtCaseData.getCourt_file_sworn_date() != null) {
                earliestDate = dateFormat.parse(primaryCourtCaseData.getCourt_file_sworn_date());
            }
            for (CourtCaseData d : courtCaseDataList){
                if(d.getCourt_file_sworn_date() != null) {
                    Date dd = dateFormat.parse(d.getCourt_file_sworn_date());
                    if(earliestDate == null || dd.before(earliestDate)){
                        earliestDate = dd;
                    }
                }
            }
        }catch(ParseException e){
            e.printStackTrace();
        }
        String earliestSwornDate = null;
        if(earliestDate != null) {
            earliestSwornDate = DateTimeUtils.convertToUtcFromBCDateTimeString(dateFormat.format(earliestDate));
        }
        DemsFieldData swornDate = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.SWORN_DATE.getLabel(), earliestSwornDate);

        Set<String> ctHomeRegName = new HashSet<>();
        Set<String> ctHomeReg = new HashSet<>();
        Set<String> ctFileLevel = new HashSet<>();
        Set<String> ctFileClass = new HashSet<>();
        Set<String> ctFileDesig = new HashSet<>();
        Set<String> antCrownElect = new HashSet<>();
        Set<String> apprCrownAgenName = new HashSet<>();
        Set<String> cFlagList = new HashSet<>();
        ctHomeRegName.add(primaryCourtCaseData.getCourt_home_registry_name());
        ctHomeReg.add(primaryCourtCaseData.getCourt_home_registry());
        ctFileLevel.add(primaryCourtCaseData.getCourt_file_level());
        ctFileClass.add(primaryCourtCaseData.getCourt_file_class());
        ctFileDesig.add(primaryCourtCaseData.getCourt_file_designation());
        antCrownElect.add(primaryCourtCaseData.getAnticipated_crown_election());
        apprCrownAgenName.add(primaryCourtCaseData.getApproving_crown_agency_name());
        cFlagList.addAll(caseFlagList);

        List<String> courtCaseDataCaseFlagList = new ArrayList<String>();
        if(courtCaseDataList != null) {
            for (CourtCaseData courtcase : courtCaseDataList) {
                //ccdList.add(courtcase);
                ctHomeRegName.add(courtcase.getCourt_home_registry_name());
                ctHomeReg.add(courtcase.getCourt_home_registry());
                ctFileLevel.add(courtcase.getCourt_file_level());
                ctFileClass.add(courtcase.getCourt_file_class());
                ctFileDesig.add(courtcase.getCourt_file_designation());
                antCrownElect.add(courtcase.getAnticipated_crown_election());
                apprCrownAgenName.add(courtcase.getApproving_crown_agency_name());
                for (String caseFlag : courtcase.getCase_flags()) {
                    //dataList.add(caseFlag);
                    if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel().equals(caseFlag)) {
                        if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel())) {
                            courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.VUL1.getLabel());
                        }
                    } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel().equals(caseFlag)) {
                        if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel())) {
                            courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.CHI1.getLabel());
                        }
                    } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel().equals(caseFlag)) {
                        if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel())) {
                            courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.Indigenous.getLabel());
                        }
                    } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel().equals(caseFlag)) {
                        if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel())) {
                            courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.HROIP.getLabel());
                        }
                    } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel().equals(caseFlag)) {
                        if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel())) {
                            courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.DO_LTO.getLabel());
                        }
                    } else if(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel().equals(caseFlag)) {
                        if(!courtCaseDataCaseFlagList.contains(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel())) {
                            courtCaseDataCaseFlagList.add(DemsListItemFieldData.CASE_FLAG_FIELD_MAPPINGS.RVO.getLabel());
                        }
                    }
                }
                cFlagList.addAll(courtCaseDataCaseFlagList);
            }
        }
        //System.out.println("ctHomeRegName:"+ ctHomeRegName);
        //System.out.println("ctHomeReg:"+ ctHomeReg);
        //System.out.println("ctFileLevel:"+ ctFileLevel);
        //System.out.println("ctFileClass:"+ ctFileClass);
        //System.out.println("ctFileDesig:"+ ctFileDesig);
        //System.out.println("antCrownElect:"+ antCrownElect);
        //System.out.println("apprCrownAgenName:"+ apprCrownAgenName);
        //System.out.println("cFlagList:"+ cFlagList);

        DemsFieldData courtHomeRegName = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG_NAME.getLabel(), new ArrayList<String>(ctHomeRegName));
        DemsFieldData courtHomeReg = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_HOME_REG.getLabel(), new ArrayList<String>(ctHomeReg));
        DemsFieldData courtFileLevel = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_LEVEL.getLabel(), new ArrayList<String>(ctFileLevel));
        DemsFieldData fileClass = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CLASS.getLabel(), new ArrayList<String>(ctFileClass));
        DemsFieldData designation = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.DESIGNATION.getLabel(), new ArrayList<String>(ctFileDesig));
        DemsFieldData crownElection = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_ELECTION.getLabel(), new ArrayList<String>(antCrownElect));
        DemsFieldData crownOffice = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CROWN_OFFICE.getLabel(), new ArrayList<String>(apprCrownAgenName));
        DemsFieldData caseFlags = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_FLAGS.getLabel(), new ArrayList<String>(cFlagList));

        StringBuilder courtFileIDbuilder = new StringBuilder();
        if(primaryCourtCaseData.getCourt_file_id() != null) {
            courtFileIDbuilder.append(primaryCourtCaseData.getCourt_file_id());
        }
        for (CourtCaseData courtcase : courtCaseDataList) {
            if(courtcase.getCourt_file_id() != null) {
                if(courtFileIDbuilder.length() > 0) {
                    courtFileIDbuilder.append("; ");
                }
                courtFileIDbuilder.append(courtcase.getCourt_file_id());
            }
        }
        DemsFieldData courtFileId = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.MDOC_JUSTIN_NO.getLabel(), courtFileIDbuilder.toString());


        StringBuilder courtFileNobuilder = new StringBuilder();
        if(primaryCourtCaseData.getCourt_file_number_seq_type()!=null) {
            courtFileNobuilder.append(primaryCourtCaseData.getCourt_file_number_seq_type());
        }
        for (CourtCaseData courtcase : courtCaseDataList) {
            if(courtcase.getCourt_file_number_seq_type()!=null) {
                if(courtFileNobuilder.length() > 0) {
                    courtFileNobuilder.append("; ");
                }
                courtFileNobuilder.append(courtcase.getCourt_file_number_seq_type());
            }
        }
        DemsFieldData courtFileNo = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_NO.getLabel(), courtFileNobuilder.toString());


        StringBuilder courtFileDetailbuilder = new StringBuilder();//primaryCourtCaseData.getCourt_home_registry_identifier() + ": " + primaryCourtCaseData.getCourt_file_number_seq_type()
        if(primaryCourtCaseData.getCourt_file_number_seq_type() != null && primaryCourtCaseData.getCourt_home_registry_identifier()!=null) {
            courtFileDetailbuilder.append(primaryCourtCaseData.getCourt_home_registry_identifier()).append(":").append(primaryCourtCaseData.getCourt_file_number_seq_type());
        }
        for (CourtCaseData courtcase : courtCaseDataList) {
            if(courtcase.getCourt_file_number_seq_type() != null && courtcase.getCourt_home_registry_identifier()!=null) {
                if(courtFileDetailbuilder.length() > 0) {
                    courtFileDetailbuilder.append("; ");
                }
                courtFileDetailbuilder.append(courtcase.getCourt_home_registry_identifier()).append(":").append(courtcase.getCourt_file_number_seq_type());
            }
        }
        DemsFieldData courtFileDetails = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.COURT_FILE_DETAILS.getLabel(), courtFileDetailbuilder.toString());


        StringBuilder chargebuilder = new StringBuilder();
        if(primaryCourtCaseData.getOffence_description_list() != null) {
            if(chargebuilder.length() > 0) {
                chargebuilder.append("; ");
            }
            chargebuilder.append(primaryCourtCaseData.getOffence_description_list());
            for (CourtCaseData courtcase : courtCaseDataList) {
                if(chargebuilder.length() > 0) {
                    chargebuilder.append("; ");
                }
                chargebuilder.append(courtcase.getOffence_description_list());
            }
        }
        DemsFieldData approvedCharges = new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CHARGES.getLabel(), chargebuilder.toString());


        List<DemsFieldData> fieldData = new ArrayList<DemsFieldData>();
        fieldData.add(courtFileId);
        fieldData.add(crownElection);
        fieldData.add(courtFileLevel);
        fieldData.add(fileClass);
        fieldData.add(designation);
        fieldData.add(swornDate);
        fieldData.add(approvedCharges);
        fieldData.add(courtFileNo);
        fieldData.add(courtFileDetails);
        fieldData.add(courtHomeReg);
        fieldData.add(courtHomeRegName);
        fieldData.add(caseFlags);
        fieldData.add(accusedName);
        fieldData.add(crownOffice);
        fieldData.add(lastJustinUpdate);
        //added as part of jade-2483
        if(primaryCourtCaseData.getRelated_agency_file().size() == 1 && courtCaseDataList.size() == 0){
            fieldData.add(new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_STATE.getLabel(), "Approved Court Case"));
        }else if(primaryCourtCaseData.getRelated_agency_file().size() >= 2 && courtCaseDataList.size() == 0 ){
            fieldData.add(new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_STATE.getLabel(), "Merged Court Case"));
        }else if(primaryCourtCaseData.getRelated_agency_file().size() == 1 && courtCaseDataList.size() >= 1){
            fieldData.add(new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_STATE.getLabel(), "Updated Court Files"));
        }else if(primaryCourtCaseData.getRelated_agency_file().size() >= 2 && courtCaseDataList.size() >= 1){
            fieldData.add(new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.CASE_STATE.getLabel(), "Merged Court Cases"));
        }else{

        }
        for(ChargeAssessmentDataRef raf : primaryCourtCaseData.getRelated_agency_file()){
            if(raf.getPrimary_yn().equals("Y")){
                fieldData.add(new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_ID.getLabel(), raf.getRcc_id()));
                fieldData.add(new DemsFieldData(DemsFieldData.FIELD_MAPPINGS.PRIMARY_AGENCY_FILE_NO.getLabel(), raf.getAgency_file_no()));
            }
        }
        setFields(fieldData);
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
