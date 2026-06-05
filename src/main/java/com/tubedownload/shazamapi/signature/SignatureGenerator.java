package com.tubedownload.shazamapi.signature;



import java.util.*;

public class SignatureGenerator implements Iterator<DecodedMessage>, Iterable<DecodedMessage> {
    private static final int SAMPLE_RATE_HZ = 16000;
    private static final int FFT_WINDOW_SIZE = 2048;
    private static final int FFT_BINS = 1025;
    private static final int BATCH_SIZE = 128;
    private static final double[] HANNING_MATRIX = createHanningMatrix();

    private final List<Short> inputPendingProcessing = new ArrayList<>();
    private int samplesProcessed;
    private RingBuffer<Short> ringBufferOfSamples = new RingBuffer<>(FFT_WINDOW_SIZE, () -> (short) 0);
    private RingBuffer<double[]> spreadFftsOutput = new RingBuffer<>(256, () -> new double[FFT_BINS]);
    private RingBuffer<double[]> fftOutputs = new RingBuffer<>(256, () -> new double[FFT_BINS]);
    private double maxTimeSeconds = 3.1;
    private int maxPeaks = 255;
    private DecodedMessage nextSignature = new DecodedMessage();

    public void feedInput(short[] samples) {
        for (short sample : samples) {
            inputPendingProcessing.add(sample);
        }
    }

    public int samplesProcessed() {
        return samplesProcessed;
    }

    public void addSamplesProcessed(int samples) {
        samplesProcessed += samples;
    }

    public void maxTimeSeconds(double maxTimeSeconds) {
        this.maxTimeSeconds = maxTimeSeconds;
    }

    @Override
    public Iterator<DecodedMessage> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        return inputPendingProcessing.size() - samplesProcessed >= BATCH_SIZE;
    }

    @Override
    public DecodedMessage next() {
        DecodedMessage signature = getNextSignature();
        if (signature == null) {
            throw new NoSuchElementException();
        }
        return signature;
    }

    public DecodedMessage getNextSignature() {
        if (!hasNext()) {
            return null;
        }
        while (inputPendingProcessing.size() - samplesProcessed >= BATCH_SIZE
                && (nextSignature.numberSamples() / (double) nextSignature.sampleRateHz() < maxTimeSeconds
                || nextSignature.peakCount() < maxPeaks)) {
            short[] batch = new short[BATCH_SIZE];
            for (int i = 0; i < BATCH_SIZE; i++) {
                batch[i] = inputPendingProcessing.get(samplesProcessed + i);
            }
            processInput(batch);
            samplesProcessed += BATCH_SIZE;
        }
        DecodedMessage returnedSignature = nextSignature;
        nextSignature = new DecodedMessage();
        ringBufferOfSamples = new RingBuffer<>(FFT_WINDOW_SIZE, () -> (short) 0);
        fftOutputs = new RingBuffer<>(256, () -> new double[FFT_BINS]);
        spreadFftsOutput = new RingBuffer<>(256, () -> new double[FFT_BINS]);
        return returnedSignature;
    }

    private void processInput(short[] samples) {
        nextSignature.addSamples(samples.length);
        for (int offset = 0; offset < samples.length; offset += BATCH_SIZE) {
            doFft(Arrays.copyOfRange(samples, offset, Math.min(offset + BATCH_SIZE, samples.length)));
            doPeakSpreadingAndRecognition();
        }
    }

    private void doFft(short[] batch) {
        ringBufferOfSamples.writeSequential(batch);
        double[] real = new double[FFT_WINDOW_SIZE];
        double[] imaginary = new double[FFT_WINDOW_SIZE];
        for (int i = 0; i < FFT_WINDOW_SIZE; i++) {
            real[i] = HANNING_MATRIX[i] * ringBufferOfSamples.get(ringBufferOfSamples.position() + i);
        }
        fft(real, imaginary);
        double[] bins = new double[FFT_BINS];
        for (int bin = 0; bin < FFT_BINS; bin++) {
            bins[bin] = magnitude(real[bin], imaginary[bin]);
        }
        fftOutputs.append(bins);
    }


    private void fft(double[] real, double[] imaginary) {
        int n = real.length;
        for (int j = 1, i = 0; j < n; j++) {
            int bit = n >>> 1;
            for (; (i & bit) != 0; bit >>>= 1) {
                i ^= bit;
            }
            i ^= bit;
            if (j < i) {
                swap(real, i, j);
                swap(imaginary, i, j);
            }
        }
        for (int length = 2; length <= n; length <<= 1) {
            double angle = -2 * Math.PI / length;
            double wLengthReal = Math.cos(angle);
            double wLengthImaginary = Math.sin(angle);
            for (int i = 0; i < n; i += length) {
                double wReal = 1;
                double wImaginary = 0;
                for (int j = 0; j < length / 2; j++) {
                    int even = i + j;
                    int odd = even + length / 2;
                    double oddReal = real[odd] * wReal - imaginary[odd] * wImaginary;
                    double oddImaginary = real[odd] * wImaginary + imaginary[odd] * wReal;
                    real[odd] = real[even] - oddReal;
                    imaginary[odd] = imaginary[even] - oddImaginary;
                    real[even] += oddReal;
                    imaginary[even] += oddImaginary;
                    double nextWReal = wReal * wLengthReal - wImaginary * wLengthImaginary;
                    wImaginary = wReal * wLengthImaginary + wImaginary * wLengthReal;
                    wReal = nextWReal;
                }
            }
        }
    }

    private void swap(double[] values, int left, int right) {
        double tmp = values[left];
        values[left] = values[right];
        values[right] = tmp;
    }

    private double magnitude(double real, double imaginary) {
        return Math.max((real * real + imaginary * imaginary) / (1 << 17), 1e-10);
    }

    private void doPeakSpreadingAndRecognition() {
        doPeakSpreading();
        if (spreadFftsOutput.numWritten() >= 46) {
            doPeakRecognition();
        }
    }

    private void doPeakSpreading() {
        double[] originLastFft = fftOutputs.get(fftOutputs.position() - 1);
        double[] spreadLastFft = Arrays.copyOf(originLastFft, originLastFft.length);
        for (int position = 0; position < FFT_BINS; position++) {
            if (position < 1023) {
                spreadLastFft[position] = Math.max(spreadLastFft[position], Math.max(spreadLastFft[position + 1], spreadLastFft[position + 2]));
            }
            double maxValue = spreadLastFft[position];
            for (int formerFftNum : new int[]{-1, -3, -6}) {
                double[] formerFftOutput = spreadFftsOutput.get(spreadFftsOutput.position() + formerFftNum);
                maxValue = Math.max(formerFftOutput[position], maxValue);
                formerFftOutput[position] = maxValue;
            }
        }
        spreadFftsOutput.append(spreadLastFft);
    }

    private void doPeakRecognition() {
        double[] fftMinus46 = fftOutputs.get(fftOutputs.position() - 46);
        double[] fftMinus49 = spreadFftsOutput.get(spreadFftsOutput.position() - 49);
        for (int binPosition = 10; binPosition < 1015; binPosition++) {
            if (fftMinus46[binPosition] < 1 / 64.0 || fftMinus46[binPosition] < fftMinus49[binPosition - 1]) {
                continue;
            }
            double maxNeighborInFftMinus49 = 0;
            int[] neighborOffsets = {-10, -7, -4, -3, 1, 2, 5, 8};
            for (int neighborOffset : neighborOffsets) {
                maxNeighborInFftMinus49 = Math.max(fftMinus49[binPosition + neighborOffset], maxNeighborInFftMinus49);
            }
            if (fftMinus46[binPosition] <= maxNeighborInFftMinus49) {
                continue;
            }
            double maxNeighborInOtherAdjacentFfts = maxNeighborInFftMinus49;
            for (int otherOffset : otherOffsets()) {
                maxNeighborInOtherAdjacentFfts = Math.max(
                        spreadFftsOutput.get(spreadFftsOutput.position() + otherOffset)[binPosition - 1],
                        maxNeighborInOtherAdjacentFfts
                );
            }
            if (fftMinus46[binPosition] <= maxNeighborInOtherAdjacentFfts) {
                continue;
            }
            storePeak(fftMinus46, binPosition);
        }
    }

    private void storePeak(double[] fftMinus46, int binPosition) {
        int fftNumber = spreadFftsOutput.numWritten() - 46;
        double peakMagnitude = Math.log(Math.max(1 / 64.0, fftMinus46[binPosition])) * 1477.3 + 6144;
        double peakMagnitudeBefore = Math.log(Math.max(1 / 64.0, fftMinus46[binPosition - 1])) * 1477.3 + 6144;
        double peakMagnitudeAfter = Math.log(Math.max(1 / 64.0, fftMinus46[binPosition + 1])) * 1477.3 + 6144;
        double peakVariation1 = peakMagnitude * 2 - peakMagnitudeBefore - peakMagnitudeAfter;
        if (peakVariation1 <= 0) {
            throw new IllegalStateException("peakVariation1 is not positive");
        }
        double peakVariation2 = (peakMagnitudeAfter - peakMagnitudeBefore) * 32 / peakVariation1;
        double correctedPeakFrequencyBin = binPosition * 64 + peakVariation2;
        double frequencyHz = correctedPeakFrequencyBin * (SAMPLE_RATE_HZ / 2.0 / 1024.0 / 64.0);
        FrequencyBand band = bandForFrequency(frequencyHz);
        if (band == null) {
            return;
        }
        nextSignature.addPeak(band, new FrequencyPeak(fftNumber, (int) peakMagnitude, (int) correctedPeakFrequencyBin, SAMPLE_RATE_HZ));
    }

    private FrequencyBand bandForFrequency(double frequencyHz) {
        if (frequencyHz < 250) {
            return null;
        } else if (frequencyHz < 520) {
            return FrequencyBand.BAND_250_520;
        } else if (frequencyHz < 1450) {
            return FrequencyBand.BAND_520_1450;
        } else if (frequencyHz < 3500) {
            return FrequencyBand.BAND_1450_3500;
        } else if (frequencyHz <= 5500) {
            return FrequencyBand.BAND_3500_5500;
        }
        return null;
    }

    private int[] otherOffsets() {
        int[] offsets = new int[2 + 6 + 6];
        offsets[0] = -53;
        offsets[1] = -45;
        int index = 2;
        for (int value = 165; value < 201; value += 7) {
            offsets[index++] = value;
        }
        for (int value = 214; value < 250; value += 7) {
            offsets[index++] = value;
        }
        return offsets;
    }

    private static double[] createHanningMatrix() {
        double[] matrix = new double[FFT_WINDOW_SIZE];
        for (int i = 0; i < FFT_WINDOW_SIZE; i++) {
            matrix[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * (i + 1) / (FFT_WINDOW_SIZE + 1));
        }
        return matrix;
    }
}
