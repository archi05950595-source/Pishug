package com.pishug.cat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(250, 233, 205));
        getWindow().setNavigationBarColor(Color.rgb(255, 247, 234));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_HORIZONTAL);
        page.setPadding(dp(22), dp(32), dp(22), dp(28));
        page.setBackgroundColor(Color.rgb(255, 247, 234));

        TextView title = new TextView(this);
        title.setText("ПИШУГ");
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextSize(35);
        title.setTextColor(Color.rgb(124, 60, 25));
        title.setGravity(Gravity.CENTER);
        page.addView(title, new LinearLayout.LayoutParams(-1, dp(56)));

        TextView subtitle = new TextView(this);
        subtitle.setText("Твой маленький рыжий друг 🐾");
        subtitle.setTextSize(17);
        subtitle.setTextColor(Color.rgb(121, 83, 58));
        subtitle.setGravity(Gravity.CENTER);
        page.addView(subtitle, new LinearLayout.LayoutParams(-1, dp(36)));

        CatView preview = new CatView(this);
        LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(dp(235), dp(265));
        catParams.topMargin = dp(12);
        catParams.bottomMargin = dp(12);
        page.addView(preview, catParams);

        status = new TextView(this);
        status.setTextSize(15);
        status.setTextColor(Color.rgb(110, 76, 52));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, dp(52));
        statusParams.bottomMargin = dp(9);
        page.addView(status, statusParams);

        Button permissions = makeButton("1. Разрешить поверх окон", Color.rgb(225, 151, 76));
        permissions.setOnClickListener(v -> openOverlaySettings());
        page.addView(permissions, buttonParams());

        Button start = makeButton("2. Пустить Пишуга гулять", Color.rgb(193, 104, 43));
        start.setOnClickListener(v -> startPet());
        page.addView(start, buttonParams());

        Button stop = makeButton("Убрать Пишуга с экрана", Color.rgb(119, 97, 81));
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, PetService.class));
            status.setText("Пишуг отдыхает. Запусти его снова!");
        });
        page.addView(stop, buttonParams());

        TextView tip = new TextView(this);
        tip.setText("Нажми на котика — он мяукнет!\nПеретаскивай его пальцем по экрану.");
        tip.setTextSize(14);
        tip.setTextColor(Color.rgb(128, 102, 80));
        tip.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tipParams = new LinearLayout.LayoutParams(-1, dp(62));
        tipParams.topMargin = dp(8);
        page.addView(tip, tipParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        setContentView(scroll);
        updateStatus();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (status != null) updateStatus();
    }

    private Button makeButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(16);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(16));
        button.setBackground(background);
        button.setElevation(dp(3));
        return button;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(57));
        params.bottomMargin = dp(11);
        return params;
    }

    private void updateStatus() {
        if (Settings.canDrawOverlays(this)) {
            status.setText("✅ Разрешение получено. Можно играть!");
        } else {
            status.setText("Сначала разреши показывать Пишуга поверх других приложений.");
        }
    }

    private void openOverlaySettings() {
        Intent settings = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(settings);
    }

    private void startPet() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            status.setText("Включи разрешение и вернись в приложение.");
            return;
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }
        try {
            startForegroundService(new Intent(this, PetService.class));
            status.setText("🐈 Пишуг гуляет! Открой другой экран.");
        } catch (Exception e) {
            status.setText("Не удалось запустить: " + e.getMessage());
        }
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
