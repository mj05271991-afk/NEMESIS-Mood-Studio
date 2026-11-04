package com.nemesis.moonstudio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public final class AudioBandCapture {
    public interface Listener { void onBands(float bass, float mid, float treble); }
    private final Listener listener;
    private volatile boolean running;
    private AudioRecord record;
    private Thread worker;
    public AudioBandCapture(Listener listener) { this.listener = listener; }
    public synchronized void start() {
        if (running) return;
        int rate = 22050;
        int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) return;
        record = new AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
        if (record.getState() != AudioRecord.STATE_INITIALIZED) { record.release(); record = null; return; }
        running = true;
        record.startRecording();
        worker = new Thread(new Runnable() { @Override public void run() { captureLoop(); }}, "NemesisAudioBands");
        worker.start();
    }
    private void captureLoop() {
        short[] pcm = new short[2048];
        while (running) {
            int n = record.read(pcm, 0, pcm.length);
            if (n > 0) {
                float bass = bandEnergy(pcm, n, 80, 250, 22050);
                float mid = bandEnergy(pcm, n, 250, 2000, 22050);
                float treble = bandEnergy(pcm, n, 2000, 7000, 22050);
                listener.onBands(bass, mid, treble);
            }
        }
    }
    private float bandEnergy(short[] pcm, int n, int low, int high, int sampleRate) {
        double total = 0; int count = 0;
        int step = Math.max(1, sampleRate / Math.max(1, high - low));
        for (int i = 0; i < n; i += step) { double v = pcm[i] / 32768.0; total += v * v; count++; }
        double rms = Math.sqrt(total / Math.max(1, count));
        return (float)Math.min(1.0, rms * 5.0);
    }
    public synchronized void stop() {
        running = false;
        if (record != null) { try { record.stop(); } catch (Exception ignored) {} record.release(); record = null; }
        worker = null;
    }
}
