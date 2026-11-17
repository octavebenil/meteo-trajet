package io.github.octavebenil.personalweather.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import io.github.octavebenil.personalweather.R;
import io.github.octavebenil.personalweather.domain.HourlyForecast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Hour-by-hour list. Rainy hours are highlighted. */
final class HourlyAdapter extends RecyclerView.Adapter<HourlyAdapter.Holder> {

    private final List<HourlyForecast> items = new ArrayList<>();
    private int threshold;

    void submit(List<HourlyForecast> hours, int threshold) {
        items.clear();
        items.addAll(hours);
        this.threshold = threshold;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hour, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        HourlyForecast hour = items.get(position);
        holder.time.setText(WeatherText.hour(hour));
        holder.icon.setText(WeatherText.emoji(hour.getCondition()));
        holder.icon.setContentDescription(WeatherText.describe(holder.itemView.getContext(), hour.getCondition()));
        holder.temp.setText(WeatherText.temperature(hour.getTemperatureC()));
        holder.rain.setText(String.format(Locale.getDefault(), "💧 %d %%", hour.getPrecipitationProbability()));
        holder.mm.setText(String.format(Locale.getDefault(), "%.1f mm", hour.getPrecipitationMm()));
        boolean rainy = hour.isRainy(threshold);
        holder.itemView.setBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(),
                rainy ? R.color.rain_highlight : android.R.color.transparent));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView time;
        final TextView icon;
        final TextView temp;
        final TextView rain;
        final TextView mm;

        Holder(View view) {
            super(view);
            time = view.findViewById(R.id.hour_time);
            icon = view.findViewById(R.id.hour_icon);
            temp = view.findViewById(R.id.hour_temp);
            rain = view.findViewById(R.id.hour_rain);
            mm = view.findViewById(R.id.hour_mm);
        }
    }
}
