package io.github.octavebenil.personalweather.ui;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;

import io.github.octavebenil.personalweather.R;
import io.github.octavebenil.personalweather.data.SettingsStore;
import io.github.octavebenil.personalweather.domain.TimeWindow;
import io.github.octavebenil.personalweather.scheduling.AlarmScheduler;
import io.github.octavebenil.personalweather.scheduling.DayRefreshScheduler;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/** Settings: morning alarm time, ringtone, rain threshold, commute windows. */
public final class SettingsActivity extends AppCompatActivity {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final int MIN_THRESHOLD = 10;

    private SettingsStore settings;

    private MaterialSwitch alarmSwitch;
    private TextView alarmTime;
    private MaterialSwitch soundSwitch;
    private MaterialSwitch soundOnlyRainSwitch;
    private TextView ringtoneValue;
    private View ringtoneRow;
    private TextView thresholdValue;
    private TextView outboundValue;
    private TextView returnValue;
    private MaterialSwitch dayAlertsSwitch;
    private TextView exactAlarmWarning;

    private ActivityResultLauncher<Intent> ringtonePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        settings = new SettingsStore(this);

        alarmSwitch = findViewById(R.id.alarm_switch);
        alarmTime = findViewById(R.id.alarm_time_value);
        soundSwitch = findViewById(R.id.sound_switch);
        soundOnlyRainSwitch = findViewById(R.id.sound_only_rain_switch);
        ringtoneValue = findViewById(R.id.ringtone_value);
        ringtoneRow = findViewById(R.id.ringtone_row);
        thresholdValue = findViewById(R.id.threshold_value);
        outboundValue = findViewById(R.id.outbound_value);
        returnValue = findViewById(R.id.return_value);
        dayAlertsSwitch = findViewById(R.id.day_alerts_switch);
        exactAlarmWarning = findViewById(R.id.exact_alarm_warning);
        SeekBar thresholdBar = findViewById(R.id.threshold_bar);

        ringtonePicker = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getData() == null) {
                        return;
                    }
                    Uri uri = pickedUri(result.getData());
                    settings.setRingtoneUri(uri == null ? null : uri.toString());
                    bind();
                });

        alarmSwitch.setChecked(settings.isAlarmEnabled());
        alarmSwitch.setOnCheckedChangeListener((button, checked) -> {
            settings.setAlarmEnabled(checked);
            AlarmScheduler.apply(this);
            bind();
        });
        findViewById(R.id.alarm_time_row).setOnClickListener(v ->
                pickTime(settings.getAlarmTime(), time -> {
                    settings.setAlarmTime(time);
                    AlarmScheduler.apply(this);
                    bind();
                }));

        soundSwitch.setChecked(settings.isSoundEnabled());
        soundSwitch.setOnCheckedChangeListener((button, checked) -> {
            settings.setSoundEnabled(checked);
            bind();
        });
        soundOnlyRainSwitch.setChecked(settings.isSoundOnlyWhenRain());
        soundOnlyRainSwitch.setOnCheckedChangeListener((button, checked) -> settings.setSoundOnlyWhenRain(checked));
        ringtoneRow.setOnClickListener(v -> openRingtonePicker());

        thresholdBar.setMax(90 - MIN_THRESHOLD);
        thresholdBar.setProgress(settings.getThreshold() - MIN_THRESHOLD);
        thresholdBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    settings.setThreshold(progress + MIN_THRESHOLD);
                    bind();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        findViewById(R.id.outbound_row).setOnClickListener(v -> pickWindow(settings.getOutboundWindow(),
                (start, end) -> settings.setOutboundWindow(start, end)));
        findViewById(R.id.return_row).setOnClickListener(v -> pickWindow(settings.getReturnWindow(),
                (start, end) -> settings.setReturnWindow(start, end)));

        dayAlertsSwitch.setChecked(settings.isDayAlertsEnabled());
        dayAlertsSwitch.setOnCheckedChangeListener((button, checked) -> {
            settings.setDayAlertsEnabled(checked);
            DayRefreshScheduler.apply(this);
        });

        exactAlarmWarning.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        AlarmScheduler.apply(this);
        bind();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void bind() {
        alarmTime.setText(settings.getAlarmTime().format(HH_MM));
        boolean soundOn = settings.isSoundEnabled();
        soundOnlyRainSwitch.setEnabled(soundOn);
        ringtoneRow.setEnabled(soundOn);
        ringtoneRow.setAlpha(soundOn ? 1f : 0.5f);
        ringtoneValue.setText(ringtoneTitle());
        thresholdValue.setText(getString(R.string.threshold_value, settings.getThreshold()));
        outboundValue.setText(format(settings.getOutboundWindow()));
        returnValue.setText(format(settings.getReturnWindow()));
        boolean warn = settings.isAlarmEnabled() && !AlarmScheduler.canScheduleExact(this);
        exactAlarmWarning.setVisibility(warn ? View.VISIBLE : View.GONE);
    }

    private String ringtoneTitle() {
        String stored = settings.getRingtoneUri();
        Uri uri = stored != null ? Uri.parse(stored) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (uri == null) {
            return getString(R.string.ringtone_default);
        }
        try {
            Ringtone ringtone = RingtoneManager.getRingtone(this, uri);
            return ringtone != null ? ringtone.getTitle(this) : getString(R.string.ringtone_default);
        } catch (RuntimeException e) {
            return getString(R.string.ringtone_default);
        }
    }

    private void openRingtonePicker() {
        String stored = settings.getRingtoneUri();
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, getString(R.string.ringtone))
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                        stored != null ? Uri.parse(stored) : RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
        ringtonePicker.launch(intent);
    }

    @SuppressWarnings("deprecation")
    private static Uri pickedUri(Intent data) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri.class);
        }
        return data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
    }

    private void pickTime(LocalTime initial, Consumer<LocalTime> onPicked) {
        new TimePickerDialog(this, (view, hour, minute) -> onPicked.accept(LocalTime.of(hour, minute)),
                initial.getHour(), initial.getMinute(), DateFormat.is24HourFormat(this)).show();
    }

    private void pickWindow(TimeWindow current, WindowSetter setter) {
        pickTime(current.getStart(), start ->
                pickTime(current.getEnd().isBefore(start) ? start.plusHours(1) : current.getEnd(), end -> {
                    if (end.isBefore(start)) {
                        setter.set(end, start);
                    } else {
                        setter.set(start, end);
                    }
                    bind();
                }));
    }

    private static String format(TimeWindow window) {
        return window.getStart().format(HH_MM) + " – " + window.getEnd().format(HH_MM);
    }

    private interface WindowSetter {
        void set(LocalTime start, LocalTime end);
    }
}
