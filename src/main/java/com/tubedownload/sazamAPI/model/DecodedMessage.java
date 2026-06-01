package com.tubedownload.sazamAPI.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DecodedMessage {

    private int sampleRateHz;

    private int numberSamples;

    private Map<FrequencyBand, List<FrequencyPeak>>
            frequencyBandToSoundPeaks;

    public DecodedMessage() {

        this.frequencyBandToSoundPeaks =
                new EnumMap<>(FrequencyBand.class);
    }

    public static DecodedMessage empty() {

        DecodedMessage message =
                new DecodedMessage();

        message.setSampleRateHz(16000);
        message.setNumberSamples(0);

        return message;
    }

    public int getTotalPeaks() {

        return frequencyBandToSoundPeaks
                .values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }

    public void addPeak(
            FrequencyBand band,
            FrequencyPeak peak) {

        frequencyBandToSoundPeaks
                .computeIfAbsent(
                        band,
                        key -> new ArrayList<>())
                .add(peak);
    }

    public List<FrequencyPeak> getPeaks(
            FrequencyBand band) {

        return frequencyBandToSoundPeaks
                .getOrDefault(
                        band,
                        List.of());
    }

    public boolean hasPeaks() {

        return !frequencyBandToSoundPeaks.isEmpty();
    }

    public void clearPeaks() {

        frequencyBandToSoundPeaks.clear();
    }

    public double getDurationSeconds() {

        if (sampleRateHz == 0) {
            return 0.0;
        }

        return (double) numberSamples
                / sampleRateHz;
    }

    public int getSampleRateHz() {
        return sampleRateHz;
    }

    public void setSampleRateHz(int sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public int getNumberSamples() {
        return numberSamples;
    }

    public void setNumberSamples(int numberSamples) {
        this.numberSamples = numberSamples;
    }

    public Map<FrequencyBand, List<FrequencyPeak>>
    getFrequencyBandToSoundPeaks() {

        return frequencyBandToSoundPeaks;
    }

    public void setFrequencyBandToSoundPeaks(
            Map<FrequencyBand, List<FrequencyPeak>>
                    frequencyBandToSoundPeaks) {

        this.frequencyBandToSoundPeaks =
                frequencyBandToSoundPeaks;
    }

    @Override
    public String toString() {

        return "DecodedMessage{" +
                "sampleRateHz=" + sampleRateHz +
                ", numberSamples=" + numberSamples +
                ", durationSeconds=" + getDurationSeconds() +
                ", totalPeaks=" + getTotalPeaks() +
                '}';
    }
}
