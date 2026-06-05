package com.tubedownload.shazamapi.signature;





import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.zip.CRC32;

public class DecodedMessage {
    public static final String DATA_URI_PREFIX = "data:audio/vnd.shazam.sig;base64,";
    private static final int HEADER_SIZE = 48;
    private static final int HEADER_MAGIC_1 = 0xCAFE2580;
    private static final int HEADER_MAGIC_2 = 0x94119C00;
    private static final int HEADER_MAGIC_3 = (15 << 19) + 0x40000;
    private static final int BAND_TLV_BASE = 0x60030040;
    private static final Map<Integer, Integer> SAMPLE_RATE_FROM_SHIFTED_ID = Map.of(
            1 << 27, 8000,
            2 << 27, 11025,
            3 << 27, 16000,
            4 << 27, 32000,
            5 << 27, 44100,
            6 << 27, 48000
    );
    private static final Map<Integer, Integer> SHIFTED_ID_FROM_SAMPLE_RATE = Map.of(
            8000, 1 << 27,
            11025, 2 << 27,
            16000, 3 << 27,
            32000, 4 << 27,
            44100, 5 << 27,
            48000, 6 << 27
    );

    private int sampleRateHz = 16000;
    private int numberSamples;
    private final EnumMap<FrequencyBand, List<FrequencyPeak>> frequencyBandToSoundPeaks = new EnumMap<>(FrequencyBand.class);

    public int sampleRateHz() {
        return sampleRateHz;
    }

    public void sampleRateHz(int sampleRateHz) {
        this.sampleRateHz = sampleRateHz;
    }

    public int numberSamples() {
        return numberSamples;
    }

    public void addSamples(int samples) {
        numberSamples += samples;
    }

    public void numberSamples(int numberSamples) {
        this.numberSamples = numberSamples;
    }

    public Map<FrequencyBand, List<FrequencyPeak>> frequencyBandToSoundPeaks() {
        return frequencyBandToSoundPeaks;
    }

    public void addPeak(FrequencyBand band, FrequencyPeak peak) {
        frequencyBandToSoundPeaks.computeIfAbsent(band, ignored -> new ArrayList<>()).add(peak);
    }

    public int peakCount() {
        return frequencyBandToSoundPeaks.values().stream().mapToInt(List::size).sum();
    }

    public String encodeToUri() {
        return DATA_URI_PREFIX + Base64.getEncoder().encodeToString(encodeToBinary());
    }

    public byte[] encodeToBinary() {
        ByteArrayOutputStream contents = new ByteArrayOutputStream();
        new TreeMap<>(frequencyBandToSoundPeaks).forEach((band, peaks) -> writeBand(contents, band, peaks));

        byte[] contentBytes = contents.toByteArray();
        int sizeMinusHeader = contentBytes.length + 8;
        ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + sizeMinusHeader).order(ByteOrder.LITTLE_ENDIAN);
        writeHeader(buffer, 0, sizeMinusHeader);
        buffer.putInt(0x40000000);
        buffer.putInt(sizeMinusHeader);
        buffer.put(contentBytes);

        CRC32 crc32 = new CRC32();
        crc32.update(buffer.array(), 8, buffer.array().length - 8);
        writeHeader(buffer.position(0), (int) crc32.getValue(), sizeMinusHeader);
        return buffer.array();
    }

    private void writeHeader(ByteBuffer buffer, int crc32, int sizeMinusHeader) {
        buffer.putInt(HEADER_MAGIC_1);
        buffer.putInt(crc32);
        buffer.putInt(sizeMinusHeader);
        buffer.putInt(HEADER_MAGIC_2);
        buffer.putInt(0).putInt(0).putInt(0);
        buffer.putInt(SHIFTED_ID_FROM_SAMPLE_RATE.get(sampleRateHz));
        buffer.putInt(0).putInt(0);
        buffer.putInt((int) (numberSamples + sampleRateHz * 0.24));
        buffer.putInt(HEADER_MAGIC_3);
    }

    private void writeBand(ByteArrayOutputStream contents, FrequencyBand band, List<FrequencyPeak> peaks) {
        ByteArrayOutputStream peakBytes = new ByteArrayOutputStream();
        int fftPassNumber = 0;
        for (FrequencyPeak peak : peaks) {
            if (peak.fftPassNumber() < fftPassNumber) {
                throw new IllegalArgumentException("frequency peak FFT pass decreased");
            }
            if (peak.fftPassNumber() - fftPassNumber >= 255) {
                peakBytes.write(0xFF);
                writeLittleEndianInt(peakBytes, peak.fftPassNumber());
                fftPassNumber = peak.fftPassNumber();
            }
            peakBytes.write(peak.fftPassNumber() - fftPassNumber);
            writeLittleEndianShort(peakBytes, peak.peakMagnitude());
            writeLittleEndianShort(peakBytes, peak.correctedPeakFrequencyBin());
            fftPassNumber = peak.fftPassNumber();
        }

        byte[] peaksPayload = peakBytes.toByteArray();
        writeLittleEndianInt(contents, BAND_TLV_BASE + band.id());
        writeLittleEndianInt(contents, peaksPayload.length);
        contents.writeBytes(peaksPayload);
        contents.writeBytes(new byte[Math.floorMod(-peaksPayload.length, 4)]);
    }

    private static void writeLittleEndianInt(ByteArrayOutputStream stream, int value) {
        stream.write(value & 0xFF);
        stream.write((value >>> 8) & 0xFF);
        stream.write((value >>> 16) & 0xFF);
        stream.write((value >>> 24) & 0xFF);
    }

    private static void writeLittleEndianShort(ByteArrayOutputStream stream, int value) {
        stream.write(value & 0xFF);
        stream.write((value >>> 8) & 0xFF);
    }

    public static DecodedMessage decodeFromUri(String uri) {
        if (!uri.startsWith(DATA_URI_PREFIX)) {
            throw new IllegalArgumentException("Not a valid audio/vnd.shazam.sig data URI");
        }
        return decodeFromBinary(Base64.getDecoder().decode(uri.substring(DATA_URI_PREFIX.length())));
    }

    public static DecodedMessage decodeFromBinary(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        buffer.position(8);
        CRC32 crc32 = new CRC32();
        crc32.update(data, 8, data.length - 8);
        buffer.position(0);

        int magic1 = buffer.getInt();
        int checksum = buffer.getInt();
        int sizeMinusHeader = buffer.getInt();
        int magic2 = buffer.getInt();
        buffer.position(28);
        int shiftedSampleRateId = buffer.getInt();
        buffer.position(40);
        int samplesPlusDividedSampleRate = buffer.getInt();
        buffer.getInt();

        if (magic1 != HEADER_MAGIC_1 || magic2 != HEADER_MAGIC_2) {
            throw new IllegalArgumentException("Wrong magic string specified in header");
        }
        if (sizeMinusHeader != data.length - HEADER_SIZE) {
            throw new IllegalArgumentException("Wrong size specified in header");
        }
        if (((int) crc32.getValue()) != checksum) {
            throw new IllegalArgumentException("Wrong checksum specified in header");
        }

        DecodedMessage result = new DecodedMessage();
        result.sampleRateHz(SAMPLE_RATE_FROM_SHIFTED_ID.get(shiftedSampleRateId));
        result.numberSamples((int) (samplesPlusDividedSampleRate - result.sampleRateHz * 0.24));

        int firstType = buffer.getInt();
        int firstLength = buffer.getInt();
        if (firstType != 0x40000000 || firstLength != data.length - HEADER_SIZE) {
            throw new IllegalArgumentException("Unexpected first chunk format");
        }
        while (buffer.hasRemaining()) {
            int bandId = buffer.getInt() - BAND_TLV_BASE;
            int peaksSize = buffer.getInt();
            int limit = buffer.position() + peaksSize;
            FrequencyBand band = FrequencyBand.fromId(bandId);
            int fftPassNumber = 0;
            while (buffer.position() < limit) {
                int fftPassOffset = Byte.toUnsignedInt(buffer.get());
                if (fftPassOffset == 0xFF) {
                    fftPassNumber = buffer.getInt();
                    continue;
                }
                fftPassNumber += fftPassOffset;
                int peakMagnitude = Short.toUnsignedInt(buffer.getShort());
                int correctedBin = Short.toUnsignedInt(buffer.getShort());
                result.addPeak(band, new FrequencyPeak(fftPassNumber, peakMagnitude, correctedBin, result.sampleRateHz));
            }
            buffer.position(limit + Math.floorMod(-peaksSize, 4));
        }
        return result;
    }
}
