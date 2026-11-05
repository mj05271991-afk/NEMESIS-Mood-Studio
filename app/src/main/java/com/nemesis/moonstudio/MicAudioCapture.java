package com.nemesis.moonstudio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

/** Default-device microphone spectrum input. Captures mono PCM and reports normalized band levels. */
final class MicAudioCapture {
    interface Listener {
        void onStarted();
        void onBands(float bass, float mids, float treble);
        void onError(String message);
    }

    private static final int RATE = 16000;
    private static final int WINDOW = 1024;
    private volatile boolean running;
    private AudioRecord recorder;
    private Thread worker;
    private final Listener listener;

    MicAudioCapture(Listener listener) { this.listener = listener; }

    synchronized void start() {
        if (running) return;
        if (worker != null && worker.isAlive()) {
            listener.onError("Microphone is still shutting down. Please try again in a moment.");
            return;
        }

        int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) {
            listener.onError("This device does not support microphone capture.");
            return;
        }

        final AudioRecord source;
        try {
            source = new AudioRecord(MediaRecorder.AudioSource.MIC, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                    Math.max(min, WINDOW * 4));
            if (source.getState() != AudioRecord.STATE_INITIALIZED) {
                source.release();
                listener.onError("Microphone could not be initialized.");
                return;
            }
            source.startRecording();
            if (source.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                source.release();
                listener.onError("Microphone did not enter the recording state.");
                return;
            }
            recorder = source;
            running = true;
            worker = new Thread(new Runnable() {
                public void run() { captureLoop(source); }
            }, "NemesisMicAudio");
            worker.start();
            listener.onStarted();
        } catch (SecurityException e) {
            releaseRecorder();
            listener.onError("Microphone permission was not granted.");
        } catch (Exception e) {
            releaseRecorder();
            listener.onError("Microphone start failed: " + e.getMessage());
        }
    }

    private void captureLoop(AudioRecord source) {
        short[] samples = new short[WINDOW];
        try {
            while (running && recorder == source) {
                int count = source.read(samples, 0, samples.length);
                if (count <= 0) {
                    if (running) listener.onError("Microphone read failed (" + count + ").");
                    break;
                }
                float bass = band(samples, count, 55, 250);
                float mids = band(samples, count, 250, 2200);
                float treble = band(samples, count, 2200, 7000);
                if (running && recorder == source) listener.onBands(bass, mids, treble);
            }
        } catch (Exception e) {
            if (running) listener.onError("Microphone capture stopped: " + e.getMessage());
        } finally {
            synchronized (this) {
                if (recorder == source) {
                    running = false;
                    recorder = null;
                    release(source);
                }
                if (worker == Thread.currentThread()) worker = null;
            }
        }
    }

    private float band(short[] data, int n, int low, int high) {
        double energy = 0;
        int bins = 0;
        for (int f = low; f <= high; f += Math.max(1, (high - low) / 8)) {
            double omega = 2.0 * Math.PI * f / RATE;
            double coeff = 2.0 * Math.cos(omega), q0, q1 = 0, q2 = 0;
            for (int i = 0; i < n; i++) {
                q0 = coeff * q1 - q2 + data[i];
                q2 = q1;
                q1 = q0;
            }
            double power = q1 * q1 + q2 * q2 - coeff * q1 * q2;
            energy += Math.sqrt(Math.max(0, power)) / n;
            bins++;
        }
        double level = bins == 0 ? 0 : (energy / bins) / 8500.0;
        return (float)Math.max(0, Math.min(1, level));
    }

    private void releaseRecorder() {
        AudioRecord current = recorder;
        recorder = null;
        if (current != null) release(current);
    }

    private void release(AudioRecord current) {
        try { current.stop(); } catch (Exception ignored) { }
        try { current.release(); } catch (Exception ignored) { }
    }

    synchronized void stop() {
        running = false;
        AudioRecord current = recorder;
        recorder = null;
        if (current != null) release(current);
    }
}
