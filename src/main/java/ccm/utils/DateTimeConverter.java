package ccm.utils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class DateTimeConverter {

    // in bcDateTimeString format: "yyyy-MM-dd HH:mm:ss"
    // out format: "yyyy-MM-dd HH:mm:ss"
    public static String convertToUtcFromBCDateTimeString(String bcDateTimeString) throws DateTimeParseException {
        // Java 8 - Convert Date Time From One Timezone To Another
        // https://www.javaprogramto.com/2020/12/java-convert-date-between-timezones.html

        String utcDateTimeString = null;

        ZonedDateTime zonedBCDateTime = ZonedDateTime.parse(bcDateTimeString + " PT", 
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z"));

        utcDateTimeString = zonedBCDateTime.withZoneSameInstant(ZoneId.of("UTC")).format(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        return utcDateTimeString;
    }    
    
    // in bcDateString format: "yyyy-MM-dd"
    // out format: "yyyy-MM-dd HH:mm:ss"
    public static String convertToUtcFromBCDateString(String bcDateString) throws DateTimeParseException {
        return convertToUtcFromBCDateTimeString(bcDateString + " 00:00:00");
    }

    public static void main(String[] args) {
        String bcDateTimeString1 = "2000-10-01 10:01:02";
        String utcDateTimeString1 = DateTimeConverter.convertToUtcFromBCDateTimeString(bcDateTimeString1);
        System.out.println("");
        System.out.println("BC date/time PDT : " + bcDateTimeString1);
        System.out.println("UTC date/time : " + utcDateTimeString1);

        String bcDateTimeString2 = "2000-12-02 10:01:02";
        String utcDateTimeString2 = DateTimeConverter.convertToUtcFromBCDateTimeString(bcDateTimeString2);
        System.out.println("");
        System.out.println("BC date/time PST : " + bcDateTimeString2);
        System.out.println("UTC date/time : " + utcDateTimeString2);

        String bcDateString3 = "2000-12-03";
        String utcDateString3 = DateTimeConverter.convertToUtcFromBCDateString(bcDateString3);
        System.out.println("");
        System.out.println("BC date PST : " + bcDateString3);
        System.out.println("UTC date/time : " + utcDateString3);
	}
}
