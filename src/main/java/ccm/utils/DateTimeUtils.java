package ccm.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeUtils {

    // in bcDateTimeString format: "yyyy-MM-dd" or "yyyy-MM-dd HH:mm:ss"
    // out format: "yyyy-MM-dd HH:mm:ss"
    public static String convertToUtcFromBCDateTimeString(String bcDateTimeString) throws DateTimeParseException {
        // Java 8 - Convert Date Time From One Timezone To Another
        // https://www.javaprogramto.com/2020/12/java-convert-date-between-timezones.html

        String utcDateTimeString = null;

        // preprocess input date/time string.
        if (bcDateTimeString != null) {
            String bcDateTimeWithTimeZoneString = null;
            if (bcDateTimeString.length() > 10) {
                // this is a date+time.  Add BC timezone to string.
                bcDateTimeWithTimeZoneString = bcDateTimeString + " PT";
            } else {
                // this is a date.  add default time and BC timezone to string.
                bcDateTimeWithTimeZoneString = bcDateTimeString + " 00:00:00 PT";
            }

            ZonedDateTime zonedBCDateTime = ZonedDateTime.parse(bcDateTimeWithTimeZoneString, 
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

            utcDateTimeString = zonedBCDateTime.withZoneSameInstant(ZoneId.of("UTC")).format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        return utcDateTimeString;
    }

    public static void main(String[] args) {
        String bcDateTimeString1 = "2000-10-01 10:01:02";
        String utcDateTimeString1 = DateTimeUtils.convertToUtcFromBCDateTimeString(bcDateTimeString1);
        System.out.println("");
        System.out.println("BC date/time PDT : " + bcDateTimeString1);
        System.out.println("UTC date/time : " + utcDateTimeString1);

        String bcDateTimeString2 = "2000-12-02 10:01:02";
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
