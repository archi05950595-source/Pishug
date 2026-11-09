package com.pishug.cat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

/** Foreground service that keeps one small interactive cat overlay on screen. */
public class PetService extends Service {
    private static final String CHANNEL_ID = "pishug_pet";
    private static final String ACTION_STOP = "com.pishug.cat.STOP";
    private static final int NOTIFICATION_ID = 301;

    private WindowManager windowManager;
    private WindowManager.LayoutParams params;
    private CatView cat;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private MeowPlayer meow;
    private long lastTick;
    private long lastRandomMeow;
    private long startedAt;
    private long pauseWalkingUntil;
    private int direction = 1;
    private int screenWidth;
    private int screenHeight;
    private boolean dragging;

    private final Runnable walker = new Runnable() {
        @Override public void run() {
            if (cat == null || params == null) return;
            long now = SystemClock.uptimeMillis();
            long delta = Math.min(150, now - lastTick);
            lastTick = now;
            boolean idle = (now - startedAt) % 14000L > 10500L;
            boolean moving = !dragging && !idle && now > pauseWalkingUntil;
            cat.setWalking(moving);
            if (moving) {
                params.x += Math.round(direction * delta * 0.050f);
                if (params.x > screenWidth - dp(167)) {
                    params.x = Math.max(0, screenWidth - dp(167));
                    direction = -1;
                }
                if (params.x < 0) {
                    params.x = 0;
                    direction = 1;
                }
                cat.setFacing(direction);
                try { windowManager.updateViewLayout(cat, params); }
                catch (IllegalArgumentException ignored) { return; }
            }
            // Occasionally meow when awake, without filling the screen with buttons.
            if (!dragging && now - lastRandomMeow > 26000 && now > pauseWalkingUntil) {
                lastRandomMeow = now;
                if (meow != null) meow.play();
            }
            handler.postDelayed(this, 45);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotification();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (cat == null) createOverlay();
        return START_STICKY;
    }

    private void createNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                CHANNEL_ID, "Пишуг на экране", NotificationManager.IMPORTANCE_LOW));

        Intent stopIntent = new Intent(this, PetService.class).setAction(ACTION_STOP);
        PendingIntent stop = PendingIntent.getService(this, 1, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent activityIntent = new Intent(this, MainActivity.class);
        PendingIntent content = PendingIntent.getActivity(this, 2, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.pishug_icon)
                .setContentTitle("Пишуг гуляет 🐈")
                .setContentText("Нажми на кота, чтобы он мяукнул")
                .setContentIntent(content)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel, "Убрать кота", stop).build())
                .build();
        startForeground(NOTIFICATION_ID, notification);
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        meow = new MeowPlayer(handler);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        params = new WindowManager.LayoutParams(
                dp(172), dp(190),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = Math.max(0, screenWidth / 2 - dp(86));
        params.y = Math.max(dp(70), screenHeight - dp(380));
        params.setTitle("Пишуг — рыжий кот");

        cat = new CatView(this);
        cat.setOnTouchListener(new View.OnTouchListener() {
            private float touchX, touchY;
            private int startX, startY;

            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        startX = params.x;
                        startY = params.y;
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float distanceX = event.getRawX() - touchX;
                        float distanceY = event.getRawY() - touchY;
                        if (Math.abs(distanceX) > dp(9) || Math.abs(distanceY) > dp(9)) {
                            dragging = true;
                        }
                        if (dragging) {
                            params.x = Math.max(0, Math.min(screenWidth - dp(172),
                                    startX + Math.round(distanceX)));
                            params.y = Math.max(0, Math.min(screenHeight - dp(190),
                                    startY + Math.round(distanceY)));
                            try { windowManager.updateViewLayout(cat, params); }
                            catch (IllegalArgumentException ignored) { }
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            cat.celebrate();
                            meow.play();
                            lastRandomMeow = SystemClock.uptimeMillis();
                            pauseWalkingUntil = SystemClock.uptimeMillis() + 1400;
                        } else {
                            pauseWalkingUntil = SystemClock.uptimeMillis() + 900;
                        }
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        dragging = false;
                        return true;
                    default:
                        return true;
                }
            }
        });
        try {
            windowManager.addView(cat, params);
        } catch (Exception e) {
            cat = null;
            stopSelf();
            return;
        }
        lastTick = SystemClock.uptimeMillis();
        startedAt = lastTick;
        lastRandomMeow = lastTick;
        handler.post(walker);
    }

    private int dp(float number) {
        return (int) (number * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override public void onDestroy() {
        handler.removeCallbacks(walker);
        if (cat != null && windowManager != null) {
            try { windowManager.removeView(cat); }
            catch (IllegalArgumentException ignored) { }
            cat = null;
        }
        if (meow != null) meow.release();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
