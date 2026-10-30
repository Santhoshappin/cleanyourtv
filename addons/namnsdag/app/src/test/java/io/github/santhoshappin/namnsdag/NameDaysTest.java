package io.github.santhoshappin.namnsdag;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class NameDaysTest {
    private static NameDays shipped() throws IOException {
        try (BufferedReader in = Files.newBufferedReader(Paths.get("src/main/res/raw/namnsdagar.txt"), StandardCharsets.UTF_8)) {
            return NameDays.parse(in);
        }
    }

    private static NameDays of(String text) throws IOException {
        return NameDays.parse(new BufferedReader(new StringReader(text)));
    }

    @Test public void theListHasEveryDayOfALeapYear() throws IOException {
        NameDays list = shipped();
        assertEquals(366, list.size());
        for (LocalDate day = LocalDate.of(2028, 1, 1); day.getYear() == 2028; day = day.plusDays(1)) {
            NameDays.Day entry = list.on(day);
            assertTrue(day + " has neither names nor a note", !entry.names.isEmpty() || !entry.note.isEmpty());
        }
    }

    @Test public void theNamesAddedIn2022AreThere() throws IOException {
        NameDays list = shipped();
        assertEquals(Arrays.asList("Henrik", "Henry"), list.on(LocalDate.of(2026, 1, 19)).names);
        assertEquals(Arrays.asList("Lydia", "Cornelia"), list.on(LocalDate.of(2026, 12, 3)).names);
        assertEquals(Arrays.asList("Tobias", "Tim"), list.on(LocalDate.of(2026, 11, 2)).names);
    }

    @Test public void aFeastDayHasNoNamesAndANote() throws IOException {
        NameDays list = shipped();
        NameDays.Day day = list.on(LocalDate.of(2026, 11, 1));
        assertTrue(day.names.isEmpty());
        assertEquals("Allhelgonadagen", day.note);
        NameDays.Day eve = list.on(LocalDate.of(2026, 12, 24));
        assertEquals(Collections.singletonList("Eva"), eve.names);
        assertEquals("Julafton", eve.note);
    }

    @Test public void findIgnoresCaseAndSpacesAndKeepsTheListsSpelling() throws IOException {
        NameDays list = shipped();
        NameDays.Match match = list.find("  gustav   adolf ", LocalDate.of(2026, 10, 30));
        assertEquals("Gustav Adolf", match.name);
        assertEquals(LocalDate.of(2026, 11, 6), match.date);
        assertEquals("Åsa", list.find("åsa", LocalDate.of(2026, 1, 1)).name);
    }

    @Test public void findLooksAheadToNextYearAndCountsToday() throws IOException {
        NameDays list = shipped();
        assertEquals(LocalDate.of(2027, 1, 2), list.find("Svea", LocalDate.of(2026, 10, 30)).date);
        assertEquals(LocalDate.of(2026, 10, 30), list.find("Elsa", LocalDate.of(2026, 10, 30)).date);
        assertNull(list.find("Zebulon", LocalDate.of(2026, 10, 30)));
    }

    @Test public void aNameOnTheLeapDayWaitsForTheNextLeapYear() throws IOException {
        NameDays list = of("02-28|Maria\n02-29|Skotta|Skottdagen\n");
        assertEquals(LocalDate.of(2028, 2, 29), list.find("Skotta", LocalDate.of(2026, 3, 1)).date);
        assertTrue(list.on(LocalDate.of(2027, 3, 1)).names.isEmpty());
    }

    @Test public void joinUsesTheWordForAnd() {
        assertEquals("", NameDays.join(Collections.emptyList(), "och"));
        assertEquals("Elsa", NameDays.join(Collections.singletonList("Elsa"), "och"));
        assertEquals("Elsa och Isabella", NameDays.join(Arrays.asList("Elsa", "Isabella"), "och"));
        assertEquals("Kasper, Melker och Baltsar", NameDays.join(Arrays.asList("Kasper", "Melker", "Baltsar"), "och"));
    }

    @Test(expected = IOException.class) public void aDateTwiceIsRefused() throws IOException {
        of("01-02|Svea\n01-02|Alfred\n");
    }

    @Test(expected = IOException.class) public void aLineWithNothingIsRefused() throws IOException {
        of("01-02||\n");
    }
}
