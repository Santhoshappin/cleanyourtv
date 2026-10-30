package io.github.santhoshappin.namnsdag;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** The Swedish name-day list, read from res/raw/namnsdagar.txt. Plain Java, so the unit tests run on the host. */
final class NameDays {
    static final Locale SWEDISH = new Locale("sv", "SE");

    static final class Day {
        /** In almanac order; empty on a feast day. */
        final List<String> names;
        /** "Julafton", "Allhelgonadagen"; empty when the day has none. */
        final String note;

        Day(List<String> names, String note) {
            this.names = Collections.unmodifiableList(names);
            this.note = note;
        }
    }

    /** A name and the next date it is celebrated on. */
    static final class Match {
        final String name;
        final LocalDate date;

        Match(String name, LocalDate date) {
            this.name = name;
            this.date = date;
        }
    }

    private static final Day NONE = new Day(Collections.emptyList(), "");

    private final Map<MonthDay, Day> days;
    /** Folded name to the date it belongs to, and the spelling the list uses. */
    private final Map<String, MonthDay> dates = new HashMap<>();
    private final Map<String, String> spellings = new HashMap<>();

    private NameDays(Map<MonthDay, Day> days) {
        this.days = days;
        for (Map.Entry<MonthDay, Day> entry : days.entrySet()) {
            for (String name : entry.getValue().names) {
                dates.put(fold(name), entry.getKey());
                spellings.put(fold(name), name);
            }
        }
    }

    /** Lines of "MM-DD|names|note"; blank lines and lines starting with # are skipped. */
    static NameDays parse(BufferedReader in) throws IOException {
        Map<MonthDay, Day> days = new HashMap<>();
        for (String line; (line = in.readLine()) != null; ) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] fields = line.split("\\|", -1);
            if (fields.length < 2 || fields.length > 3) throw new IOException("bad line: " + line);
            MonthDay date = MonthDay.parse("--" + fields[0]);
            List<String> names = new ArrayList<>();
            for (String name : fields[1].split(",")) if (!name.trim().isEmpty()) names.add(name.trim());
            String note = fields.length == 3 ? fields[2].trim() : "";
            if (names.isEmpty() && note.isEmpty()) throw new IOException("no names and no note: " + line);
            if (days.put(date, new Day(names, note)) != null) throw new IOException("date twice: " + fields[0]);
        }
        return new NameDays(days);
    }

    int size() { return days.size(); }

    Day on(LocalDate date) {
        Day day = days.get(MonthDay.from(date));
        return day == null ? NONE : day;
    }

    /** The next time the name is celebrated, today included, or null when the list does not have it. */
    Match find(String query, LocalDate from) {
        String key = fold(query);
        MonthDay date = dates.get(key);
        if (date == null) return null;
        LocalDate next = date.atYear(from.getYear());
        if (next.isBefore(from)) next = date.atYear(from.getYear() + 1);
        // 29 February: atYear moves it to the 28th in other years, so look ahead to the next leap year.
        while (!MonthDay.from(next).equals(date)) next = date.atYear(next.getYear() + 1);
        return new Match(spellings.get(key), next);
    }

    /** "Elsa", "Elsa och Isabella", "Kasper, Melker och Baltsar", with the word for "and" given. */
    static String join(List<String> names, String and) {
        if (names.size() <= 1) return names.isEmpty() ? "" : names.get(0);
        return String.join(", ", names.subList(0, names.size() - 1)) + " " + and + " " + names.get(names.size() - 1);
    }

    private static String fold(String name) {
        return name.trim().replaceAll("\\s+", " ").toLowerCase(SWEDISH);
    }
}
