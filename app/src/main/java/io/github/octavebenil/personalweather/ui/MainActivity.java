package io.github.octavebenil.personalweather.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;

import io.github.octavebenil.personalweather.R;
import io.github.octavebenil.personalweather.data.SavedLocation;
import io.github.octavebenil.personalweather.data.SettingsStore;
import io.github.octavebenil.personalweather.data.WeatherRepository;
import io.github.octavebenil.personalweather.domain.CommuteAdvice;
import io.github.octavebenil.personalweather.domain.CommuteAdvisor;
import io.github.octavebenil.personalweather.domain.Forecast;
import io.github.octavebenil.personalweather.notification.Notifications;
import io.github.octavebenil.personalweather.scheduling.AlarmScheduler;
import io.github.octavebenil.personalweather.scheduling.DayRefreshScheduler;
import io.github.octavebenil.personalweather.scheduling.MorningAlarmService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Main screen: current weather, commute advice and the hour-by-hour forecast. */
public final class MainActivity extends AppCompatActivity {

    /** Set by the ringing notification: opening the app stops the alarm sound. */
    public static final String EXTRA_STOP_ALARM = "stop_alarm";

    /** While the screen is visible, the forecast is refreshed this often. */
    private static final long AUTO_REFRESH_MS = 15 * 60 * 1000L;
    private static final int HOURS_SHOWN = 24;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoRefresh = new Runnable() {
        @Override
        public void run() {
            refresh(true);
            handler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    private SettingsStore settings;
    private WeatherRepository repository;
    private HourlyAdapter adapter;

    private SwipeRefreshLayout swipe;
    private TextView place;
    private TextView currentIcon;
    private TextView currentTemp;
    private TextView currentDesc;
    private TextView updated;
    private TextView status;
    private MaterialCardView adviceCard;
    private TextView adviceTitle;
    private TextView adviceDetail;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(findViewById(R.id.toolbar));

        settings = new SettingsStore(this);
        repository = new WeatherRepository(this);
        Notifications.createChannels(this);

        swipe = findViewById(R.id.swipe);
        place = findViewById(R.id.place);
        currentIcon = findViewById(R.id.current_icon);
        currentTemp = findViewById(R.id.current_temp);
        currentDesc = findViewById(R.id.current_desc);
        updated = findViewById(R.id.updated);
        status = findViewById(R.id.status);
        adviceCard = findViewById(R.id.advice_card);
        adviceTitle = findViewById(R.id.advice_title);
        adviceDetail = findViewById(R.id.advice_detail);

        RecyclerView list = findViewById(R.id.hourly_list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HourlyAdapter();
        list.setAdapter(adapter);

        swipe.setOnRefreshListener(() -> refresh(true));

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), result -> refresh(true));

        AlarmScheduler.apply(this);
        DayRefreshScheduler.apply(this);

        Forecast cached = repository.cached();
        if (cached != null) {
            render(cached, settings.getLocation(), false, null);
        }
        requestPermissionsIfNeeded();
        stopAlarmIfRequested(getIntent());
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        stopAlarmIfRequested(intent);
    }

    private void stopAlarmIfRequested(Intent intent) {
        if (intent != null && intent.getBooleanExtra(EXTRA_STOP_ALARM, false)) {
            intent.removeExtra(EXTRA_STOP_ALARM);
            startService(new Intent(this, MorningAlarmService.class)
                    .setAction(MorningAlarmService.ACTION_STOP));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        handler.post(autoRefresh);
    }

    @Override
    protected void onStop() {
        handler.removeCallbacks(autoRefresh);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (item.getItemId() == R.id.action_refresh) {
            refresh(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void requestPermissionsIfNeeded() {
        List<String> missing = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (!missing.isEmpty()) {
            permissionLauncher.launch(missing.toArray(new String[0]));
        }
    }

    private void refresh(boolean updateLocation) {
        if (executor.isShutdown()) {
            return;
        }
        swipe.setRefreshing(true);
        executor.execute(() -> {
            WeatherRepository.Result result = repository.refresh(updateLocation);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                swipe.setRefreshing(false);
                render(result.forecast, result.location, result.fresh, result.error);
            });
        });
    }

    private void render(Forecast forecast, SavedLocation location, boolean fresh, String error) {
        place.setText(location != null && location.getPlaceName() != null
                ? location.getPlaceName()
                : getString(R.string.my_location));

        if ("no_location".equals(error)) {
            showStatus(getString(R.string.error_no_location));
        } else if ("network".equals(error)) {
            showStatus(getString(R.string.error_network));
        } else {
            status.setVisibility(View.GONE);
        }

        if (forecast == null) {
            return;
        }
        currentIcon.setText(WeatherText.emoji(forecast.getCurrentCondition()));
        currentTemp.setText(WeatherText.temperature(forecast.getCurrentTemperatureC()));
        currentDesc.setText(getString(R.string.current_detail,
                WeatherText.describe(this, forecast.getCurrentCondition()),
                WeatherText.temperature(forecast.getApparentTemperatureC()),
                forecast.getCurrentWindSpeedKmh()));
        updated.setText(getString(R.string.updated_at,
                forecast.getFetchedAt().format(DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()))));

        renderAdvice(forecast);
        adapter.submit(forecast.upcomingHours(LocalDateTime.now(), HOURS_SHOWN), settings.getThreshold());
    }

    private void renderAdvice(Forecast forecast) {
        LocalDate day = LocalDate.now();
        // After the return commute, show tomorrow's advice instead.
        if (LocalDateTime.now().toLocalTime().isAfter(settings.getReturnWindow().getEnd())) {
            day = day.plusDays(1);
        }
        CommuteAdvice advice = new CommuteAdvisor(settings.getThreshold())
                .advise(forecast, day, settings.getCommuteWindows());
        String prefix = day.equals(LocalDate.now()) ? getString(R.string.today) : getString(R.string.tomorrow);
        adviceTitle.setText(getString(R.string.advice_title_format, prefix, WeatherText.adviceTitle(this, advice)));
        adviceDetail.setText(WeatherText.adviceDetail(this, advice));
        int color;
        switch (advice.getRecommendation()) {
            case CAR:
                color = R.color.advice_car;
                break;
            case BUS:
                color = R.color.advice_bus;
                break;
            default:
                color = R.color.advice_unknown;
                break;
        }
        adviceCard.setCardBackgroundColor(ContextCompat.getColor(this, color));
    }

    private void showStatus(String message) {
        status.setText(message);
        status.setVisibility(View.VISIBLE);
    }
}
