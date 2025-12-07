package com.example.ridermesh.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;

/**
 * Foreground Service responsible for capturing audio from the microphone,
 * encoding it (Opus), and transmitting it via UDP.
 * Also handles receiving UDP packets, decoding, and playing via AudioTrack.
 */
public class AudioStreamService extends Service {

    private boolean isStreaming = false;
    // Placeholder for Opus encoder/decoder wrapper
    // private final OpusCodec opusCodec = new OpusCodec();
    // Placeholder for UDP Socket
    // private final UdpSocket udpSocket = new UdpSocket();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start Foreground Notification here (Required for Android 8+)
        // In a real app, create a notification channel and a notification.
        // startForeground(1, new NotificationCompat.Builder(this, "CHANNEL_ID").build());

        startAudioLoop();
        return START_STICKY;
    }

    private void startAudioLoop() {
        if (isStreaming) return;
        isStreaming = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                // 1. Setup AudioRecord
                // 2. Setup AudioTrack
                // 3. Loop:
                //    - Read PCM from Mic
                //    - Encode (Opus)
                //    - Send UDP
                //    - Receive UDP
                //    - Decode (Opus)
                //    - Write to AudioTrack
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        isStreaming = false;
        // Close sockets and release audio resources
        super.onDestroy();
    }
}
