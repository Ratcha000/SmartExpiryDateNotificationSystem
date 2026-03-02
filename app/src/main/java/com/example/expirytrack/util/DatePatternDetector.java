package com.example.expirytrack.util;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to detect date patterns from OCR text
 */
public class DatePatternDetector {

    // Pattern: DD/MM/YYYY or DD/MM/YY
    private static final String PATTERN_SLASH = "(\\d{1,2})/(\\d{1,2})/(\\d{2,4})";

    // Pattern: DD-MM-YYYY
    private static final String PATTERN_DASH = "(\\d{1,2})-(\\d{1,2})-(\\d{2,4})";

    // Pattern: MM/YYYY (e.g., 03/2025)
    private static final String PATTERN_MONTH_YEAR = "(\\d{1,2})/(\\d{4})";

    // Pattern: YYYY-MM-DD
    private static final String PATTERN_ISO = "(\\d{4})-(\\d{1,2})-(\\d{1,2})";

    // Keywords before date
    private static final String DATE_KEYWORDS = "(EXP|BBF|หมดอายุ|Exp|Bbf|exp|bbf)\\s*:?\\s*";

    public static class DateResult {
        public boolean found;
        public long timestamp; // in milliseconds
        public String displayText;
        public int startIndex;
        public int endIndex;

        public DateResult(boolean found, long timestamp, String displayText, int startIndex, int endIndex) {
            this.found = found;
            this.timestamp = timestamp;
            this.displayText = displayText;
            this.startIndex = startIndex;
            this.endIndex = endIndex;
        }
    }

    public static DateResult detectDate(String text) {
        if (text == null || text.isEmpty()) {
            return new DateResult(false, 0, "", -1, -1);
        }

        // Remove extra spaces and clean text
        String cleanText = text.replaceAll("\\s+", " ").trim();

        // Try to find dates with keywords
        DateResult result = findDateWithKeyword(cleanText);
        if (result.found) {
            return result;
        }

        // Try pattern: DD/MM/YYYY or DD/MM/YY
        result = findPatternSlash(cleanText);
        if (result.found) {
            return result;
        }

        // Try pattern: DD-MM-YYYY
        result = findPatternDash(cleanText);
        if (result.found) {
            return result;
        }

        // Try pattern: YYYY-MM-DD
        result = findPatternISO(cleanText);
        if (result.found) {
            return result;
        }

        // Try pattern: MM/YYYY
        result = findPatternMonthYear(cleanText);
        if (result.found) {
            return result;
        }

        return new DateResult(false, 0, "", -1, -1);
    }

    private static DateResult findDateWithKeyword(String text) {
        String pattern = DATE_KEYWORDS + PATTERN_SLASH;
        Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);

        while (m.find()) {
            try {
                int day = Integer.parseInt(m.group(2));
                int month = Integer.parseInt(m.group(3));
                int year = Integer.parseInt(m.group(4));

                long timestamp = dateToTimestamp(day, month, year);
                String displayText = String.format("%02d/%02d/%04d", day, month, year);

                return new DateResult(true, timestamp, displayText, m.start(), m.end());
            } catch (Exception e) {
                // Continue searching
            }
        }

        return new DateResult(false, 0, "", -1, -1);
    }

    private static DateResult findPatternSlash(String text) {
        Pattern p = Pattern.compile(PATTERN_SLASH);
        Matcher m = p.matcher(text);

        while (m.find()) {
            try {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year = Integer.parseInt(m.group(3));

                // Handle 2-digit year
                if (year < 100) {
                    year += (year < 30) ? 2000 : 2000;
                }

                // Validate date
                if (!isValidDate(day, month, year)) {
                    continue;
                }

                long timestamp = dateToTimestamp(day, month, year);
                String displayText = String.format("%02d/%02d/%04d", day, month, year);

                return new DateResult(true, timestamp, displayText, m.start(), m.end());
            } catch (Exception e) {
                // Continue searching
            }
        }

        return new DateResult(false, 0, "", -1, -1);
    }

    private static DateResult findPatternDash(String text) {
        Pattern p = Pattern.compile(PATTERN_DASH);
        Matcher m = p.matcher(text);

        while (m.find()) {
            try {
                int day = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year = Integer.parseInt(m.group(3));

                // Handle 2-digit year
                if (year < 100) {
                    year += (year < 30) ? 2000 : 2000;
                }

                if (!isValidDate(day, month, year)) {
                    continue;
                }

                long timestamp = dateToTimestamp(day, month, year);
                String displayText = String.format("%02d/%02d/%04d", day, month, year);

                return new DateResult(true, timestamp, displayText, m.start(), m.end());
            } catch (Exception e) {
                // Continue searching
            }
        }

        return new DateResult(false, 0, "", -1, -1);
    }

    private static DateResult findPatternISO(String text) {
        Pattern p = Pattern.compile(PATTERN_ISO);
        Matcher m = p.matcher(text);

        while (m.find()) {
            try {
                int year = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int day = Integer.parseInt(m.group(3));

                if (!isValidDate(day, month, year)) {
                    continue;
                }

                long timestamp = dateToTimestamp(day, month, year);
                String displayText = String.format("%02d/%02d/%04d", day, month, year);

                return new DateResult(true, timestamp, displayText, m.start(), m.end());
            } catch (Exception e) {
                // Continue searching
            }
        }

        return new DateResult(false, 0, "", -1, -1);
    }

    private static DateResult findPatternMonthYear(String text) {
        Pattern p = Pattern.compile(PATTERN_MONTH_YEAR);
        Matcher m = p.matcher(text);

        while (m.find()) {
            try {
                int month = Integer.parseInt(m.group(1));
                int year = Integer.parseInt(m.group(2));

                if (month < 1 || month > 12) {
                    continue;
                }

                // Set day to last day of month
                int day = getLastDayOfMonth(month, year);

                long timestamp = dateToTimestamp(day, month, year);
                String displayText = String.format("%02d/%02d/%04d", day, month, year);

                return new DateResult(true, timestamp, displayText, m.start(), m.end());
            } catch (Exception e) {
                // Continue searching
            }
        }

        return new DateResult(false, 0, "", -1, -1);
    }

    private static long dateToTimestamp(int day, int month, int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private static boolean isValidDate(int day, int month, int year) {
        if (month < 1 || month > 12) {
            return false;
        }
        if (day < 1) {
            return false;
        }

        int maxDay = getLastDayOfMonth(month, year);
        return day <= maxDay;
    }

    private static int getLastDayOfMonth(int month, int year) {
        int[] daysInMonth = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        // Check for leap year
        if (month == 2 && isLeapYear(year)) {
            return 29;
        }

        return daysInMonth[month - 1];
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}
