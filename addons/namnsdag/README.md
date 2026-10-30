# Namnsdag

An optional, standalone module card for the Swedish name days (namnsdagar). The card opens a screen with
today's names ("Idag har Elsa och Isabella namnsdag"), tomorrow's, the seven days after that, and a
**Sök namn** button that tells you when a name is celebrated next ("Erik har namnsdag den 18 maj, om 200
dagar"). On a feast day with no names, such as Allhelgonadagen, the screen says so and names the day.
Installing it never changes the launcher's header, date line, settings or theme.

The screen is in Swedish when the TV or the app is set to Swedish, and in English otherwise. The names are
the Swedish ones either way.

## Setup

Build from this folder with JDK 17 and Android SDK 34:

```sh
./gradlew --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

Sideload the APK with the repository's normal ADB tools. On Home, approve **Namnsdag** when the launcher
asks about its card, or enable it later under Settings → Module cards. The card's caption is fixed
("Dagens namn"): spec 1 has no way to put today's names on the tile itself, so they are on the card's
screen.

The day changes at midnight by the TV's own clock and time zone, checked once a minute while the screen is
open. Nothing runs when it is closed.

## Data

`app/src/main/res/raw/namnsdagar.txt` holds the name-day list (namnlängden) that Swedish almanacs have
used since 2001, with the names added in 2011, 2015, 2018 and 2022: 366 days, one line each. The list is
kept by a committee of Svenska Akademien, Kungl. Vetenskapsakademien, Kungl. Vitterhetsakademien and
Institutet för språk och folkminnen (Isof). The file was taken from Swedish Wikipedia's [date-ordered
list](https://sv.wikipedia.org/wiki/Lista_%C3%B6ver_namnsdagar_i_Sverige_i_datumordning), and every
name Isof lists as [added since 2001](https://www.isof.se/lar-dig-mer/kunskapsbanker/lar-dig-mer-om-personnamn-i-sverige/namnsdagar-i-almanackan)
was checked against it.

Isof has announced six more names from 2027 (Cassandra, Eira, Flora, Mio, Vilgot and Zackarias). Their
dates go into the file when the 2027 almanacs publish them.

## Permissions

None requested. No Internet, location, overlays, accessibility, boot receiver, alarms, background or
foreground service, analytics or ads. The list is built into the app and read from its resources.

## Validation

Unit tests (`NameDaysTest`) read the shipped file and check that every day of a leap year has names or a
feast-day note, that the 2022 additions are on their dates, that feast days and Julafton carry their notes,
that the search ignores case and extra spaces and finds the next date (rolling into next year, and to
the next leap year for 29 February), and that names are joined with "och".

![Namnsdag on a TV](screenshot.jpg)
