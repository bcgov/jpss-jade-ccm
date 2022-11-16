package ccm.utils;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtils {

    // in bcDateTimeString format: "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss.SSS"
    // out format: "yyyy-MM-dd HH:mm:ss.SSS"
    public static String convertToUtcFromBCDateTimeString(String bcDateTimeString) throws DateTimeParseException {
        // Java 8 - Convert Date Time From One Timezone To Another
        // https://www.javaprogramto.com/2020/12/java-convert-date-between-timezones.html

        String utcDateTimeString = null;
        ZonedDateTime zonedDateTime = null;

        zonedDateTime = convertToZonedDateTimeFromBCDateTimeString(bcDateTimeString);

        if (zonedDateTime != null) {
            utcDateTimeString = zonedDateTime.withZoneSameInstant(ZoneId.of("UTC")).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
        }

        return utcDateTimeString;
    }

    public static ZonedDateTime convertToZonedDateTimeFromBCDateTimeString(String bcDateTimeString) throws DateTimeParseException {
        ZonedDateTime zonedDateTime = null;

        // preprocess input date/time string.
        if (bcDateTimeString != null) {
            String bcDateTimeWithTimeZoneString = null;
            if (bcDateTimeString.length() > 10) {
                // this is a date+time.  Add BC timezone to string.
                if (bcDateTimeString.contains(".")) {
                    bcDateTimeWithTimeZoneString = bcDateTimeString + " PT";
                } else {
                    bcDateTimeWithTimeZoneString = bcDateTimeString + ".000 PT";
                }
            } else {
                // this is a date.  add default time and BC timezone to string.
                bcDateTimeWithTimeZoneString = bcDateTimeString + " 00:00:00.000 PT";
            }

            zonedDateTime = ZonedDateTime.parse(bcDateTimeWithTimeZoneString, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS z"));
        }

        return zonedDateTime;
    }

    public static String generateCurrentDtm() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date date = new Date();
        
        return formatter.format(date);
    }

    public static Long convertToEpochMilliFromBCDateTimeString(String bcDateTimeString) throws DateTimeParseException {
        Long epochMilli = null;
        ZonedDateTime zonedDateTime = null;

        zonedDateTime = convertToZonedDateTimeFromBCDateTimeString(bcDateTimeString);

        if (zonedDateTime != null) {
            epochMilli = zonedDateTime.toInstant().toEpochMilli();
        }

        return epochMilli;
    }

    public static void main(String[] args) {
        String bcDateTimeString1 = "2000-10-01 10:01:02.003";
        String utcDateTimeString1 = DateTimeUtils.convertToUtcFromBCDateTimeString(bcDateTimeString1);
        System.out.println("");
        System.out.println("BC date/time PDT : " + bcDateTimeString1);
        System.out.println("UTC date/time : " + utcDateTimeString1);

        String bcDateTimeString2 = "2000-12-02 10:01:02.003";
        String utcDateTimeString2 = DateTimeUtils.convertToUtcFromBCDateTimeString(bcDateTimeString2);
        System.out.println("");
        System.out.println("BC date/time PST : " + bcDateTimeString2);
        System.out.println("UTC date/time : " + utcDateTimeString2);

        String bcDateTimeString3 = "2000-12-03";
        String utcDateTimeString3 = DateTimeUtils.convertToUtcFromBCDateTimeString(bcDateTimeString3);
        System.out.println("");
        System.out.println("BC date PST : " + bcDateTimeString3);
        System.out.println("UTC date/time : " + utcDateTimeString3);

        String bcDateTimeString4 = null;
        String utcDateTimeString4 = DateTimeUtils.convertToUtcFromBCDateTimeString(bcDateTimeString4);
        System.out.println("");
        System.out.println("BC date PST : " + bcDateTimeString4);
        System.out.println("UTC date/time : " + utcDateTimeString4);
	}
}
