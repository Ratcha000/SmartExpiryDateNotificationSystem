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

    // Pattern: DD-MM-YYYY or DD-MM-YY
    private static final String PATTERN_DASH = "(\\d{1,2})-(\\d{1,2})-(\\d{2,4})";

    // Pattern: MM/YYYY only (4-digit year, 2-digit month) — checked AFTER 3-part slash
    private static final String PATTERN_MONTH_YEAR = "(\\d{1,2})/(\\d{4})";

    // Pattern: YYYY-MM-DD (ISO)
    private static final String PATTERN_ISO = "(\\d{4})-(\\d{1,2})-(\\d{1,2})";

    // Pattern: DD MM YYYY (space-separated, common on Thai packaging)
    private static final String PATTERN_SPACE = "(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{4})";

    // Keywords before date
    private static final String DATE_KEYWORDS =
            "(EXP|BBF|BEST|USE BY|หมดอายุ|วันหมดอายุ|Exp|Bbf|Best|exp|bbf|best)\\s*[:/.]?\\s*";

    // Minimum plausible expiry year
    private static final int MIN_YEAR = 2020;
    private static final int MAX_YEAR = 2050;

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

        // 1. Try DD/MM/YYYY (3-part slash — most common on Thai packaging)
        result = findPatternSlash(cleanText);
        if (result.found) return result;

        // 2. Try DD-MM-YYYY (dash separator)
        result = findPatternDash(cleanText);
        if (result.found) return result;

        // 3. Try YYYY-MM-DD (ISO)
        result = findPatternISO(cleanText);
        if (result.found) return result;

        // 4. Try DD MM YYYY (space-separated)
        result = findPatternSpace(cleanText);
        if (result.found) return result;

        // 5. Try MM/YYYY (last — fewest digits, highest false-positive risk)
        result = findPatternMonthYear(cleanText);
        if (result.found) return result;

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

                // Handle 2-digit year: 00-29 → 2000-2029, 30-99 → 1930-1999
                if (year < 100) {
                    year += (year < 30) ? 2000 : 1900;
                }

                // Reject implausible years for expiry dates
                if (year < MIN_YEAR || year > MAX_YEAR) continue;

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

                // Handle 2-digit year: 00-29 → 2000-2029, 30-99 → 1930-1999
                if (year < 100) {
                    year += (year < 30) ? 2000 : 1900;
                }

                // Reject implausible years
                if (year < MIN_YEAR || year > MAX_YEAR) continue;

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

    /** Space-separated: DD MM YYYY (e.g. "15 03 2025") */
    private static DateResult findPatternSpace(String text) {
        Pattern p = Pattern.compile(PATTERN_SPACE);
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                int day   = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year  = Integer.parseInt(m.group(3));
                if (year < MIN_YEAR || year > MAX_YEAR) continue;
                if (!isValidDate(day, month, year)) continue;
                long timestamp = dateToTimestamp(day, month, year);
                String display = String.format("%02d/%02d/%04d", day, month, year);
                return new DateResult(true, timestamp, display, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return new DateResult(false, 0, "", -1, -1);
    }

    private static DateResult findPatternMonthYear(String text) {
        Pattern p = Pattern.compile(PATTERN_MONTH_YEAR);
        Matcher m = p.matcher(text);

        while (m.find()) {
            try {
                int month = Integer.parseInt(m.group(1));
                int year  = Integer.parseInt(m.group(2));

                if (month < 1 || month > 12) continue;
                if (year < MIN_YEAR || year > MAX_YEAR) continue;

                // Set day to last day of month (expiry = end of that month)
                int day = getLastDayOfMonth(month, year);
                long timestamp = dateToTimestamp(day, month, year);
                String displayText = String.format("%02d/%02d/%04d", day, month, year);
                return new DateResult(true, timestamp, displayText, m.start(), m.end());
            } catch (Exception ignored) {}
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
