package com.pishug.cat;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import java.util.ArrayList;
import java.util.List;

/** Local, procedurally synthesized cat-like meow. No network or third-party audio. */
final class MeowPlayer {
    private final Handler handler;
    private final List<AudioTrack> active = new ArrayList<>();
    private static final int RATE = 22050;
    private static final float DURATION_SECONDS = 0.72f;
    private int count = 0;

    MeowPlayer(Handler handler) { this.handler = handler; }

    void play() {
        int samples = (int) (DURATION_SECONDS * RATE);
        short[] sound = new short[samples];
        double phase = 0;
        float variation = (count++ % 3) * 34;
        for (int i = 0; i < samples; i++) {
            double p = i / (double) samples;
            double pitch = 480 + variation + 370 * Math.sin(Math.PI * p) - 70 * p
                    + 13 * Math.sin(p * 45);
            phase += 2.0 * Math.PI * pitch / RATE;
            // Harmonics and two syllabic envelope humps mimic a small meow.
            double vowel = 0.66 * Math.sin(phase)
                    + 0.26 * Math.sin(2 * phase + 0.1)
                    + 0.10 * Math.sin(3 * phase - 0.1);
            double envelope = Math.pow(Math.max(0, Math.sin(Math.PI * p)), 0.7)
                    * (0.8 + 0.2 * Math.sin(Math.PI * 2 * p));
            double pcm = vowel * envelope * 0.43;
            sound[i] = (short) Math.round(Math.max(-1, Math.min(1, pcm)) * 32767);
        }
        AudioTrack track = null;
        try {
            track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(samples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build();
            int written = track.write(sound, 0, samples, AudioTrack.WRITE_BLOCKING);
            if (written < samples) {
                track.release();
                return;
            }
            track.play();
            final AudioTrack playing = track;
            active.add(playing);
            handler.postDelayed(() -> {
                active.remove(playing);
                try { playing.stop(); } catch (IllegalStateException ignored) { }
                playing.release();
            }, 1100);
        } catch (Exception ex) {
            if (track != null) {
                try { track.release(); } catch (Exception ignored) { }
            }
        }
    }

    void release() {
        for (AudioTrack track : new ArrayList<>(active)) {
            try { track.stop(); } catch (IllegalStateException ignored) { }
            try { track.release(); } catch (Exception ignored) { }
        }
        active.clear();
    }
}
