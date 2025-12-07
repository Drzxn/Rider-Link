package com.example.ridermesh.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Foreground Service responsible for capturing audio from the microphone,
 * encoding it (Opus), and transmitting it via UDP.
 * Also handles receiving UDP packets, decoding, and playing via AudioTrack.
 */
class AudioStreamService : Service() {

    private var isStreaming = false
    // Placeholder for Opus encoder/decoder wrapper
    // private val opusCodec = OpusCodec()
    // Placeholder for UDP Socket
    // private val udpSocket = UdpSocket()

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start Foreground Notification here
        startAudioLoop()
        return START_STICKY
    }

    private fun startAudioLoop() {
        if (isStreaming) return
        isStreaming = true

        Thread {
            // 1. Setup AudioRecord
            // 2. Setup AudioTrack
            // 3. Loop:
            //    - Read PCM from Mic
            //    - Encode (Opus)
            //    - Send UDP
            //    - Receive UDP
            //    - Decode (Opus)
            //    - Write to AudioTrack
        }.start()
    }

    override fun onDestroy() {
        isStreaming = false
        // Close sockets and release audio resources
        super.onDestroy()
    }
}
