package xmltv;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;

/** Pure channel-number and station-ID mapping shared by importer tests and runtime. */
final class ChannelMapper {
    private ChannelMapper() {
    }

    static int providerScope(String providerId) {
        if (providerId == null || providerId.trim().length() == 0) return 999;
        long parsed;
        try {
            parsed = Long.parseLong(providerId.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("provider.id must be a positive integer", e);
        }
        return parsed >= 1 && parsed <= 999 ? (int) parsed : 999;
    }

    static int stationId(String providerId, String xmltvId, Set<String> numbers) {
        int provider = providerScope(providerId);
        int local;
        if (numbers == null || numbers.isEmpty()) {
            if (xmltvId == null || xmltvId.length() == 0) {
                throw new IllegalArgumentException("channel ID is required when no channel number exists");
            }
            local = crc16(xmltvId.getBytes(StandardCharsets.UTF_8));
        } else {
            String number = numbers.iterator().next().trim().replace('-', '.');
            if (!number.matches("[0-9]{1,4}(?:\\.[0-9]{1,2})?")) {
                throw new IllegalArgumentException("unsupported SageTV channel number: " + number);
            }
            String[] parts = number.split("\\.", -1);
            int major = Integer.parseInt(parts[0]);
            int minor = parts.length == 1 ? 0 : Integer.parseInt(
                    parts[1].length() == 1 ? parts[1] + "0" : parts[1]);
            if (minor > 99) throw new IllegalArgumentException("channel minor number exceeds 99");
            local = major * 100 + minor;
            if (local > 999999) throw new IllegalArgumentException("channel number exceeds SageTV station-ID range");
        }
        return 1000000000 + provider * 1000000 + local;
    }

    static LinkedHashSet<String> normalizeNumbers(Set<String> input, int offset, String separator) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (input == null) return result;
        String outputSeparator = "-".equals(separator) ? "-" : ".";
        for (String original : input) {
            String number = original.trim().replace('-', '.');
            if (!number.matches("[0-9]+(?:\\.[0-9]+)?")) {
                throw new IllegalArgumentException("invalid channel number: " + original);
            }
            String[] parts = number.split("\\.", -1);
            long major = Long.parseLong(parts[0]) + offset;
            if (major < 0 || major > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("channel offset produces an invalid number: " + original);
            }
            result.add(Long.toString(major)
                    + (parts.length == 2 ? outputSeparator + parts[1] : ""));
        }
        return result;
    }

    static int crc16(byte[] buffer) {
        int crc = 0x0000;
        int polynomial = 0x1021;
        for (byte value : buffer) {
            for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
                boolean bit = ((value >> (7 - bitIndex) & 1) == 1);
                boolean high = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (high ^ bit) crc ^= polynomial;
            }
        }
        return crc & 0xffff;
    }
}
