package com.nemesis.moonstudio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

public final class AudioBandCapture {
    public interface Listener { void onBands(float bass, float mid, float treble); }

    private static final int SAMPLE_RATE = 22050;
    private static final int FFT_SIZE = 2048;
    private final Listener listener;
    private volatile boolean running;
    private AudioRecord record;
    private Thread worker;

    public AudioBandCapture(Listener listener) { this.listener = listener; }

    public synchronized void start() {
        if (running) return;
        int min = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (min <= 0) return;
        AudioRecord candidate = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                Math.max(min, FFT_SIZE * 4));
        if (candidate.getState() != AudioRecord.STATE_INITIALIZED) {
            candidate.release();
            return;
        }
        record = candidate;
        running = true;
        try {
            candidate.startRecording();
        } catch (Exception e) {
            running = false;
            candidate.release();
            record = null;
            return;
        }
        worker = new Thread(new Runnable() {
            @Override public void run() { captureLoop(candidate); }
        }, "NemesisAudioBands");
        worker.start();
    }

    private void captureLoop(AudioRecord source) {
        short[] pcm = new short[FFT_SIZE];
        double[] real = new double[FFT_SIZE];
        double[] imaginary = new double[FFT_SIZE];
        while (running) {
            int n = source.read(pcm, 0, pcm.length);
            if (!running) break;
            if (n < FFT_SIZE) continue;

            // Hann-windowed FFT separates the three frequency ranges instead of
            // measuring the same overall microphone level three times.
            for (int i = 0; i < FFT_SIZE; i++) {
                double window = 0.5 - 0.5 * Math.cos((2.0 * Math.PI * i) / (FFT_SIZE - 1));
                real[i] = (pcm[i] / 32768.0) * window;
                imaginary[i] = 0.0;
            }
            fft(real, imaginary);

            float bass = bandLevel(real, imaginary, 40, 250);
            float mid = bandLevel(real, imaginary, 250, 2000);
            float treble = bandLevel(real, imaginary, 2000, 8000);
            if (listener != null && running) listener.onBands(bass, mid, treble);
        }
    }

    private float bandLevel(double[] real, double[] imaginary, int lowHz, int highHz) {
        int first = Math.max(1, (int) Math.ceil((double) lowHz * FFT_SIZE / SAMPLE_RATE));
        int last = Math.min(FFT_SIZE / 2, (int) Math.floor((double) highHz * FFT_SIZE / SAMPLE_RATE));
        double power = 0.0;
        for (int bin = first; bin <= last; bin++) {
            power += real[bin] * real[bin] + imaginary[bin] * imaginary[bin];
        }
        // Convert one-sided FFT power to an approximate band RMS, then scale
        // for a useful 0..1 visual response. This is an effect meter, not SPL.
        double rms = Math.sqrt(2.0 * power) / FFT_SIZE;
        return (float) Math.max(0.0, Math.min(1.0, rms * 3.5));
    }

    private void fft(double[] real, double[] imaginary) {
        int n = real.length;
        int j = 0;
        for (int i = 1; i < n; i++) {
            int bit = n >> 1;
            while ((j & bit) != 0) {
                j ^= bit;
                bit >>= 1;
            }
            j ^= bit;
            if (i < j) {
                double temp = real[i]; real[i] = real[j]; real[j] = temp;
                temp = imaginary[i]; imaginary[i] = imaginary[j]; imaginary[j] = temp;
            }
        }
        for (int size = 2; size <= n; size <<= 1) {
            int half = size >> 1;
            double angle = -2.0 * Math.PI / size;
            double stepReal = Math.cos(angle);
            double stepImaginary = Math.sin(angle);
            for (int start = 0; start < n; start += size) {
                double wr = 1.0;
                double wi = 0.0;
                for (int k = 0; k < half; k++) {
                    int even = start + k;
                    int odd = even + half;
                    double tr = wr * real[odd] - wi * imaginary[odd];
                    double ti = wr * imaginary[odd] + wi * real[odd];
                    real[odd] = real[even] - tr;
                    imaginary[odd] = imaginary[even] - ti;
                    real[even] += tr;
                    imaginary[even] += ti;
                    double nextWr = wr * stepReal - wi * stepImaginary;
                    wi = wr * stepImaginary + wi * stepReal;
                    wr = nextWr;
                }
            }
        }
    }

    public synchronized void stop() {
        running = false;
        AudioRecord oldRecord = record;
        Thread oldWorker = worker;
        record = null;
        worker = null;
        if (oldRecord != null) {
            try { oldRecord.stop(); } catch (Exception ignored) { }
        }
        if (oldWorker != null && oldWorker != Thread.currentThread()) {
            try { oldWorker.join(500); } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (oldRecord != null) {
            try { oldRecord.release(); } catch (Exception ignored) { }
        }
    }
}
