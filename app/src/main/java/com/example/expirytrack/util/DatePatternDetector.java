package com.example.expirytrack.util;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to detect date patterns from OCR text.
 *
 * Supported formats (in priority order):
 *   Keyword + DD/MM/YYYY|YY      e.g. EXP 15/12/26, BEST BEFORE 20/03/2015
 *   Keyword + DD-MM-YYYY|YY      e.g. EXP 15-12-2027
 *   Keyword + COMPACT-6          e.g. EXP:040917, BBF:060826, EXP 200625
 *                                  → tries DDMMYY, YYMMDD, MMDDYY in order
 *   DD/MM/YYYY|YY                (no keyword)
 *   DD-MM-YYYY|YY                (no keyword)
 *   YYYY-MM-DD  ISO              e.g. 2027-03-15
 *   DD MM YYYY  space-separated  e.g. 15 03 2027
 *   MM/YYYY                      e.g. 03/2027
 */
public class DatePatternDetector {

    // ── Separator-based patterns ──────────────────────────────────────────
    private static final String PATTERN_SLASH      = "(\\d{1,2})/(\\d{1,2})/(\\d{2,4})";
    private static final String PATTERN_DASH       = "(\\d{1,2})-(\\d{1,2})-(\\d{2,4})";
    private static final String PATTERN_MONTH_YEAR = "(\\d{1,2})/(\\d{4})";
    private static final String PATTERN_ISO        = "(\\d{4})-(\\d{1,2})-(\\d{1,2})";
    private static final String PATTERN_SPACE      = "(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{4})";

    // ── Compact 6-digit pattern (must follow a keyword to reduce false positives) ──
    // Captures exactly 6 consecutive digits, NOT followed by more digits
    private static final String PATTERN_COMPACT_6  = "(\\d{6})(?!\\d)";

    // ── Keywords ──────────────────────────────────────────────────────────
    // FIX (Ex2): "BEST BEFORE" has a space — use \\s* between words so it
    // matches "BEST BEFORE", "BESTBEFORE", "Best Before", etc.
    // Separator after keyword: optional space/colon/dot/slash
    private static final String DATE_KEYWORDS =
            "(EXP|BBF|BEST\\s*BEFORE|BEST|USE\\s*BY|หมดอายุ|วันหมดอายุ)" +
                    "\\s*[:/.]?\\s*";

    private static final int MIN_YEAR = 2015;   // widened slightly for older products
    private static final int MAX_YEAR = 2050;

    // ── Public result type ────────────────────────────────────────────────

    public static class DateResult {
        public boolean found;
        public long    timestamp;   // milliseconds
        public String  displayText;
        public int     startIndex;
        public int     endIndex;

        public DateResult(boolean found, long timestamp, String displayText,
                          int startIndex, int endIndex) {
            this.found       = found;
            this.timestamp   = timestamp;
            this.displayText = displayText;
            this.startIndex  = startIndex;
            this.endIndex    = endIndex;
        }
    }

    // ── Entry point ───────────────────────────────────────────────────────

    public static DateResult detectDate(String text) {
        if (text == null || text.isEmpty()) {
            return new DateResult(false, 0, "", -1, -1);
        }
        String clean = text.replaceAll("\\s+", " ").trim();

        DateResult r;

        // 1. Keyword + slash  (e.g. BEST BEFORE 20/03/2015, EXP 15/12/26)
        r = findKeyword(clean, PATTERN_SLASH, false);
        if (r.found) return r;

        // 2. Keyword + dash
        r = findKeyword(clean, PATTERN_DASH, false);
        if (r.found) return r;

        // 3. Keyword + compact-6  (e.g. EXP:040917, EXP 200625, BBF:060826)
        r = findKeywordCompact6(clean);
        if (r.found) return r;

        // 4. DD/MM/YYYY without keyword
        r = findPatternSlash(clean);
        if (r.found) return r;

        // 5. DD-MM-YYYY without keyword
        r = findPatternDash(clean);
        if (r.found) return r;

        // 6. YYYY-MM-DD ISO
        r = findPatternISO(clean);
        if (r.found) return r;

        // 7. DD MM YYYY space-separated
        r = findPatternSpace(clean);
        if (r.found) return r;

        // 8. MM/YYYY (highest false-positive risk — always last)
        r = findPatternMonthYear(clean);
        if (r.found) return r;

        return new DateResult(false, 0, "", -1, -1);
    }

    // ── Keyword + pattern helpers ─────────────────────────────────────────

    /**
     * Generic: keyword followed by a separator-based pattern.
     * Group numbering: group(1)=keyword, group(2..4)=date parts.
     * @param isoOrder if true, groups are YYYY/MM/DD; otherwise DD/MM/YYYY
     */
    private static DateResult findKeyword(String text, String datePat, boolean isoOrder) {
        Pattern p = Pattern.compile(DATE_KEYWORDS + datePat, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                int a = Integer.parseInt(m.group(2));
                int b = Integer.parseInt(m.group(3));
                int c = Integer.parseInt(m.group(4));

                int day, month, year;
                if (isoOrder) { year = a; month = b; day = c; }
                else          { day = a; month = b; year = c; }

                if (year < 100) year += (year < 30) ? 2000 : 1900;
                if (year < MIN_YEAR || year > MAX_YEAR) continue;
                if (!isValidDate(day, month, year)) continue;

                return make(true, day, month, year, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return notFound();
    }

    /**
     * Keyword followed by exactly 6 digits (no separator).
     * Tries three interpretations in order:
     *   1. DDMMYY  (most common on Thai packaging)
     *   2. YYMMDD  (used by some Thai dairies)
     *   3. MMDDYY  (some imported products)
     */
    private static DateResult findKeywordCompact6(String text) {
        Pattern p = Pattern.compile(DATE_KEYWORDS + PATTERN_COMPACT_6,
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        while (m.find()) {
            String digits = m.group(2); // the 6-digit string
            DateResult r = tryCompact6(digits, m.start(), m.end());
            if (r.found) return r;
        }
        return notFound();
    }

    /**
     * Attempts all three compact-6 interpretations for a digit string.
     * Returns the first that yields a valid, plausible date.
     */
    private static DateResult tryCompact6(String d, int start, int end) {
        int p1 = Integer.parseInt(d.substring(0, 2));
        int p2 = Integer.parseInt(d.substring(2, 4));
        int p3 = Integer.parseInt(d.substring(4, 6));

        // 1. DDMMYY
        {
            int day = p1, month = p2, year = p3;
            year += (year < 30) ? 2000 : 1900;
            if (year >= MIN_YEAR && year <= MAX_YEAR && isValidDate(day, month, year))
                return make(true, day, month, year, start, end);
        }
        // 2. YYMMDD
        {
            int year = p1, month = p2, day = p3;
            year += (year < 30) ? 2000 : 1900;
            if (year >= MIN_YEAR && year <= MAX_YEAR && isValidDate(day, month, year))
                return make(true, day, month, year, start, end);
        }
        // 3. MMDDYY
        {
            int month = p1, day = p2, year = p3;
            year += (year < 30) ? 2000 : 1900;
            if (year >= MIN_YEAR && year <= MAX_YEAR && isValidDate(day, month, year))
                return make(true, day, month, year, start, end);
        }
        return notFound();
    }

    // ── Separator-based (no keyword) ──────────────────────────────────────

    private static DateResult findPatternSlash(String text) {
        Pattern p = Pattern.compile(PATTERN_SLASH);
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                int day   = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year  = Integer.parseInt(m.group(3));
                if (year < 100) year += (year < 30) ? 2000 : 1900;
                if (year < MIN_YEAR || year > MAX_YEAR) continue;
                if (!isValidDate(day, month, year)) continue;
                return make(true, day, month, year, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return notFound();
    }

    private static DateResult findPatternDash(String text) {
        Pattern p = Pattern.compile(PATTERN_DASH);
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                int day   = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int year  = Integer.parseInt(m.group(3));
                if (year < 100) year += (year < 30) ? 2000 : 1900;
                if (year < MIN_YEAR || year > MAX_YEAR) continue;
                if (!isValidDate(day, month, year)) continue;
                return make(true, day, month, year, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return notFound();
    }

    private static DateResult findPatternISO(String text) {
        Pattern p = Pattern.compile(PATTERN_ISO);
        Matcher m = p.matcher(text);
        while (m.find()) {
            try {
                int year  = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int day   = Integer.parseInt(m.group(3));
                if (!isValidDate(day, month, year)) continue;
                return make(true, day, month, year, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return notFound();
    }

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
                return make(true, day, month, year, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return notFound();
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
                int day = getLastDayOfMonth(month, year);
                return make(true, day, month, year, m.start(), m.end());
            } catch (Exception ignored) {}
        }
        return notFound();
    }

    // ── Utility ───────────────────────────────────────────────────────────

    private static DateResult make(boolean found, int day, int month, int year,
                                   int start, int end) {
        long ts = dateToTimestamp(day, month, year);
        String display = String.format("%02d/%02d/%04d", day, month, year);
        return new DateResult(found, ts, display, start, end);
    }

    private static DateResult notFound() {
        return new DateResult(false, 0, "", -1, -1);
    }

    private static long dateToTimestamp(int day, int month, int year) {
        Calendar c = Calendar.getInstance();
        c.set(year, month - 1, day, 0, 0, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static boolean isValidDate(int day, int month, int year) {
        if (month < 1 || month > 12) return false;
        if (day < 1) return false;
        return day <= getLastDayOfMonth(month, year);
    }

    private static int getLastDayOfMonth(int month, int year) {
        int[] days = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
        if (month == 2 && isLeapYear(year)) return 29;
        return days[month - 1];
    }

    private static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
}