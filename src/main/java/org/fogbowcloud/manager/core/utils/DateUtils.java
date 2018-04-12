package org.fogbowcloud.manager.core.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

	public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

	public long currentTimeMillis() {
		return System.currentTimeMillis();
	}

	public static Date getDateFromISO8601Format(String expirationDateStr) {
		SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.ROOT);
		dateFormatISO8601.setTimeZone(TimeZone.getTimeZone("GMT"));
		try {
			return dateFormatISO8601.parse(expirationDateStr);
		} catch (Exception e) {
			return null;
		}
	}

	public static String getDateISO8601Format(long dateMili) {
		SimpleDateFormat dateFormatISO8601 = new SimpleDateFormat(ISO_8601_DATE_FORMAT, Locale.ROOT);
		dateFormatISO8601.setTimeZone(TimeZone.getTimeZone("GMT"));
		String expirationDate = dateFormatISO8601.format(new Date(dateMili));
		return expirationDate;
	}
}
