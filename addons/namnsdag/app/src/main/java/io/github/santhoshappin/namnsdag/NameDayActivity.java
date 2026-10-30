package io.github.santhoshappin.namnsdag;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** A standalone module card: today's names, the coming week and a name search. It never writes the launcher's configuration. */
public final class NameDayActivity extends Activity {
    private static final int DAYS_AHEAD = 7;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            refresh();
            handler.postDelayed(this, 60_000L);
        }
    };
    private NameDays list;
    private TextView date, today, note, tomorrow;
    private LinearLayout week;
    private LocalDate shown;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                getResources().openRawResource(R.raw.namnsdagar), StandardCharsets.UTF_8))) {
            list = NameDays.parse(in);
        } catch (IOException e) {
            throw new IllegalStateException("res/raw/namnsdagar.txt does not parse", e);
        }
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_HORIZONTAL);
        panel.setPadding(dp(64), dp(40), dp(64), dp(32));
        panel.setBackgroundColor(Color.rgb(12, 30, 52));
        date = line(panel, 24, 0xfffecc02);
        today = line(panel, 52, Color.WHITE);
        note = line(panel, 22, 0xffc9d6e6);
        tomorrow = line(panel, 26, 0xffdde6f0);
        tomorrow.setPadding(0, dp(14), 0, dp(18));
        TextView heading = line(panel, 18, 0xff8fa6c0);
        heading.setText(R.string.coming_days);
        week = new LinearLayout(this);
        week.setOrientation(LinearLayout.VERTICAL);
        panel.addView(week, new LinearLayout.LayoutParams(dp(560), LinearLayout.LayoutParams.WRAP_CONTENT));
        Button find = new Button(this);
        find.setText(R.string.find_name);
        find.setOnClickListener(v -> search());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(18);
        panel.addView(find, params);
        setContentView(panel);
        find.requestFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        shown = null;
        handler.removeCallbacks(tick);
        tick.run();
    }

    @Override protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    /** Runs once a minute while visible; redraws only when the date has changed, so midnight turns the page. */
    private void refresh() {
        LocalDate now = LocalDate.now();
        if (now.equals(shown)) return;
        shown = now;
        Locale locale = Locale.getDefault();
        date.setText(capitalise(format(now, "EEEEdMMMM"), locale));
        NameDays.Day day = list.on(now);
        today.setText(day.names.isEmpty() ? getString(R.string.no_names_today)
                : getString(R.string.names_today, NameDays.join(day.names, getString(R.string.and))));
        note.setText(day.note);
        note.setVisibility(day.note.isEmpty() ? View.GONE : View.VISIBLE);
        tomorrow.setText(getString(R.string.tomorrow, names(list.on(now.plusDays(1)))));
        week.removeAllViews();
        for (int i = 2; i < 2 + DAYS_AHEAD; i++) {
            LocalDate next = now.plusDays(i);
            LinearLayout row = new LinearLayout(this);
            row.setPadding(0, dp(3), 0, dp(3));
            TextView when = text(20, 0xff8fa6c0);
            when.setText(format(next, "EEEdMMM"));
            row.addView(when, new LinearLayout.LayoutParams(dp(170), LinearLayout.LayoutParams.WRAP_CONTENT));
            TextView who = text(20, 0xffdde6f0);
            who.setText(names(list.on(next)));
            row.addView(who, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            week.addView(row);
        }
    }

    /** The names, or the feast day's name in their place. */
    private String names(NameDays.Day day) {
        return day.names.isEmpty() ? day.note : String.join(", ", day.names);
    }

    private void search() {
        EditText field = new EditText(this);
        field.setHint(R.string.find_hint);
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(24), dp(8), dp(24), 0);
        box.addView(field, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.find_name).setView(box)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.find, null).create();
        Runnable answer = () -> {
            String query = field.getText().toString().trim();
            if (query.isEmpty()) return;
            dialog.dismiss();
            new AlertDialog.Builder(this).setMessage(result(query)).setPositiveButton(android.R.string.ok, null).show();
        };
        field.setOnEditorActionListener((v, action, event) -> {
            answer.run();
            return true;
        });
        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> answer.run()));
        dialog.show();
    }

    private String result(String query) {
        LocalDate now = LocalDate.now();
        NameDays.Match match = list.find(query, now);
        if (match == null) return getString(R.string.not_found, query);
        long days = ChronoUnit.DAYS.between(now, match.date);
        if (days == 0) return getString(R.string.found_today, match.name);
        if (days == 1) return getString(R.string.found_tomorrow, match.name);
        return getString(R.string.found_on, match.name, format(match.date, "dMMMM"),
                getResources().getQuantityString(R.plurals.days_away, (int) days, (int) days));
    }

    private static String format(LocalDate day, String skeleton) {
        Locale locale = Locale.getDefault();
        return DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton), locale)
                .format(day);
    }

    private static String capitalise(String text, Locale locale) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(locale) + text.substring(1);
    }

    private TextView line(LinearLayout parent, int size, int color) {
        TextView view = text(size, color);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(4), 0, dp(4));
        parent.addView(view, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return view;
    }

    private TextView text(int size, int color) {
        TextView view = new TextView(this);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setMaxLines(2);
        return view;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
