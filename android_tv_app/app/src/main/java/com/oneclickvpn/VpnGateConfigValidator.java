package com.oneclickvpn;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

final class VpnGateConfigValidator {
    static final int MAX_BASE64_CHARS = 64 * 1024;
    static final int MAX_DECODED_BYTES = 48 * 1024;

    private static final int MAX_CONFIG_LINES = 1000;
    private static final int MAX_LINE_CHARS = 2048;
    private static final String EXPECTED_SERVER_NAME = "opengw.net";
    private static final String EXPECTED_CA_SHA256 =
            "96BCEC06264976F37460779ACF28C5A7CFE8A3C0AAE11A8FFCEE05C0BDDF08C6";

    private VpnGateConfigValidator() {
    }

    static void validateBase64(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalArgumentException("VPN profile is empty.");
        }
        if (encoded.length() > MAX_BASE64_CHARS) {
            throw new IllegalArgumentException("VPN profile encoding is too large.");
        }
        if ((encoded.length() & 3) != 0) {
            throw new IllegalArgumentException("VPN profile encoding has an invalid length.");
        }
        boolean paddingStarted = false;
        int padding = 0;
        for (int i = 0; i < encoded.length(); i++) {
            char c = encoded.charAt(i);
            if (c == '=') {
                paddingStarted = true;
                padding++;
                if (padding > 2 || i < encoded.length() - 2) {
                    throw new IllegalArgumentException("VPN profile encoding has invalid padding.");
                }
            } else if (paddingStarted || !isBase64Character(c)) {
                throw new IllegalArgumentException("VPN profile encoding contains invalid characters.");
            }
        }
    }

    static String validateAndHarden(String config, String expectedIp) {
        if (config == null || config.isEmpty()) {
            throw new IllegalArgumentException("VPN profile is empty.");
        }
        if (!isIpv4(expectedIp)) {
            throw new IllegalArgumentException("VPN server has an invalid IPv4 address.");
        }
        if (config.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("VPN profile contains a null character.");
        }

        Map<String, Integer> directiveCounts = new HashMap<>();
        Map<String, String> inlineBlocks = new HashMap<>();
        String activeBlock = null;
        StringBuilder blockContent = new StringBuilder();
        int lineCount = 0;

        for (String rawLine : config.split("\\R", -1)) {
            lineCount++;
            if (lineCount > MAX_CONFIG_LINES || rawLine.length() > MAX_LINE_CHARS) {
                throw new IllegalArgumentException("VPN profile exceeds the supported format limits.");
            }
            String line = rawLine.trim();
            if (activeBlock != null) {
                if (line.equalsIgnoreCase("</" + activeBlock + ">")) {
                    inlineBlocks.put(activeBlock, blockContent.toString());
                    activeBlock = null;
                    blockContent.setLength(0);
                } else {
                    if (line.startsWith("<") && line.endsWith(">")) {
                        throw new IllegalArgumentException("Nested VPN profile blocks are not allowed.");
                    }
                    blockContent.append(rawLine).append('\n');
                }
                continue;
            }
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }
            if (line.startsWith("<") && line.endsWith(">")) {
                activeBlock = openingBlockName(line);
                if (activeBlock == null || inlineBlocks.containsKey(activeBlock)) {
                    throw new IllegalArgumentException("VPN profile contains an unsupported or duplicate block.");
                }
                continue;
            }

            String[] parts = line.split("\\s+");
            String directive = parts[0].toLowerCase(Locale.ROOT);
            validateDirective(directive, parts, expectedIp);
            directiveCounts.put(directive, countOf(directiveCounts, directive) + 1);
        }

        if (activeBlock != null) {
            throw new IllegalArgumentException("VPN profile has an unterminated inline block.");
        }
        requireExactlyOne(directiveCounts, "client");
        requireExactlyOne(directiveCounts, "dev");
        requireExactlyOne(directiveCounts, "proto");
        requireExactlyOne(directiveCounts, "remote");
        requireExactlyOne(directiveCounts, "cipher");
        requireExactlyOne(directiveCounts, "data-ciphers");
        requireExactlyOne(directiveCounts, "auth");
        requireAtMostOne(directiveCounts, "remote-cert-tls");
        requireAtMostOne(directiveCounts, "verify-x509-name");
        requireAtMostOne(directiveCounts, "tls-version-min");
        requireExactlyOneBlock(inlineBlocks, "ca");
        requireExactlyOneBlock(inlineBlocks, "cert");
        requireExactlyOneBlock(inlineBlocks, "key");
        validateCa(inlineBlocks.get("ca"));
        validateCertificateBlock(inlineBlocks.get("cert"));
        validatePrivateKeyBlock(inlineBlocks.get("key"));

        StringBuilder hardened = new StringBuilder(config.trim());
        hardened.append("\n\n# Added by Noren VPN: authenticate VPN Gate and reduce leaks\n");
        if (!directiveCounts.containsKey("remote-cert-tls")) {
            hardened.append("remote-cert-tls server\n");
        }
        if (!directiveCounts.containsKey("verify-x509-name")) {
            hardened.append("verify-x509-name ").append(EXPECTED_SERVER_NAME).append(" name\n");
        }
        if (!directiveCounts.containsKey("tls-version-min")) {
            hardened.append("tls-version-min 1.2\n");
        }
        hardened.append("block-ipv6\n")
                .append("pull-filter ignore \"ifconfig-ipv6\"\n")
                .append("pull-filter ignore \"route-ipv6\"\n");
        return hardened.toString();
    }

    static boolean isIpv4(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String[] parts = value.split("\\.", -1);
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            if (part.isEmpty() || part.length() > 3 || (part.length() > 1 && part.charAt(0) == '0')) {
                return false;
            }
            int octet = 0;
            for (int i = 0; i < part.length(); i++) {
                char c = part.charAt(i);
                if (c < '0' || c > '9') {
                    return false;
                }
                octet = octet * 10 + (c - '0');
            }
            if (octet > 255) {
                return false;
            }
        }
        return true;
    }

    private static void validateDirective(String directive, String[] parts, String expectedIp) {
        switch (directive) {
            case "client":
            case "nobind":
            case "persist-key":
            case "persist-tun":
                requirePartCount(directive, parts, 1);
                return;
            case "dev":
                requirePartCount(directive, parts, 2);
                requireValue(directive, parts[1], "tun");
                return;
            case "proto":
                requirePartCount(directive, parts, 2);
                if (!isAllowedProto(parts[1])) {
                    throw unsupportedValue(directive);
                }
                return;
            case "remote":
                if (parts.length < 3 || parts.length > 4 || !expectedIp.equals(parts[1])) {
                    throw new IllegalArgumentException("VPN profile remote does not match the server directory.");
                }
                parseBoundedInt(parts[2], 1, 65535, "remote port");
                if (parts.length == 4 && !isAllowedProto(parts[3])) {
                    throw unsupportedValue(directive);
                }
                return;
            case "cipher":
                requirePartCount(directive, parts, 2);
                if (!isAllowedCipher(parts[1])) {
                    throw unsupportedValue(directive);
                }
                return;
            case "data-ciphers":
                requirePartCount(directive, parts, 2);
                for (String cipher : parts[1].split(":")) {
                    if (!isAllowedCipher(cipher)) {
                        throw unsupportedValue(directive);
                    }
                }
                return;
            case "auth":
                requirePartCount(directive, parts, 2);
                if (!parts[1].matches("(?i)SHA(1|256|384|512)")) {
                    throw unsupportedValue(directive);
                }
                return;
            case "resolv-retry":
                requirePartCount(directive, parts, 2);
                if (!"infinite".equalsIgnoreCase(parts[1])) {
                    parseBoundedInt(parts[1], 1, 3600, directive);
                }
                return;
            case "verb":
                requirePartCount(directive, parts, 2);
                parseBoundedInt(parts[1], 0, 4, directive);
                return;
            case "remote-cert-tls":
                requirePartCount(directive, parts, 2);
                requireValue(directive, parts[1], "server");
                return;
            case "verify-x509-name":
                requirePartCount(directive, parts, 3);
                requireValue(directive, parts[1], EXPECTED_SERVER_NAME);
                requireValue(directive, parts[2], "name");
                return;
            case "tls-version-min":
                requirePartCount(directive, parts, 2);
                if (!"1.2".equals(parts[1]) && !"1.3".equals(parts[1])) {
                    throw unsupportedValue(directive);
                }
                return;
            default:
                throw new IllegalArgumentException("VPN profile contains an unsupported directive: " + directive);
        }
    }

    private static String openingBlockName(String line) {
        if (line.length() < 3 || line.charAt(1) == '/' || line.indexOf(' ') >= 0 || line.indexOf('\t') >= 0) {
            return null;
        }
        String name = line.substring(1, line.length() - 1).toLowerCase(Locale.ROOT);
        if ("ca".equals(name) || "cert".equals(name) || "key".equals(name)) {
            return name;
        }
        return null;
    }

    private static void validateCa(String pem) {
        try {
            X509Certificate certificate = parseSingleCertificate(pem);
            if (certificate.getBasicConstraints() < 0) {
                throw new IllegalArgumentException("VPN profile CA is not a CA certificate.");
            }
            byte[] actual = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
            if (!MessageDigest.isEqual(actual, hexToBytes(EXPECTED_CA_SHA256))) {
                throw new IllegalArgumentException("VPN profile CA is not the trusted VPN Gate CA.");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("VPN profile CA certificate is invalid.", e);
        }
    }

    private static void validateCertificateBlock(String pem) {
        try {
            parseSingleCertificate(pem);
        } catch (Exception e) {
            throw new IllegalArgumentException("VPN profile client certificate is invalid.", e);
        }
    }

    private static X509Certificate parseSingleCertificate(String pem) throws Exception {
        if (countOccurrences(pem, "-----BEGIN CERTIFICATE-----") != 1
                || countOccurrences(pem, "-----END CERTIFICATE-----") != 1) {
            throw new IllegalArgumentException("VPN profile certificate block must contain one certificate.");
        }
        CertificateFactory factory = CertificateFactory.getInstance("X.509");
        return (X509Certificate) factory.generateCertificate(
                new ByteArrayInputStream(pem.getBytes(StandardCharsets.US_ASCII)));
    }

    private static void validatePrivateKeyBlock(String pem) {
        boolean rsa = countOccurrences(pem, "-----BEGIN RSA PRIVATE KEY-----") == 1
                && countOccurrences(pem, "-----END RSA PRIVATE KEY-----") == 1;
        boolean pkcs8 = countOccurrences(pem, "-----BEGIN PRIVATE KEY-----") == 1
                && countOccurrences(pem, "-----END PRIVATE KEY-----") == 1;
        if (rsa == pkcs8) {
            throw new IllegalArgumentException("VPN profile private key block is invalid.");
        }
    }

    private static void requireExactlyOne(Map<String, Integer> counts, String directive) {
        if (countOf(counts, directive) != 1) {
            throw new IllegalArgumentException("VPN profile must contain exactly one " + directive + " directive.");
        }
    }

    private static void requireAtMostOne(Map<String, Integer> counts, String directive) {
        if (countOf(counts, directive) > 1) {
            throw new IllegalArgumentException("VPN profile contains duplicate " + directive + " directives.");
        }
    }

    private static void requireExactlyOneBlock(Map<String, String> blocks, String block) {
        if (!blocks.containsKey(block)) {
            throw new IllegalArgumentException("VPN profile is missing the " + block + " block.");
        }
    }

    private static int countOf(Map<String, Integer> counts, String directive) {
        Integer count = counts.get(directive);
        return count == null ? 0 : count;
    }

    private static void requirePartCount(String directive, String[] parts, int expected) {
        if (parts.length != expected) {
            throw new IllegalArgumentException("VPN profile has invalid arguments for " + directive + ".");
        }
    }

    private static void requireValue(String directive, String actual, String expected) {
        if (!expected.equalsIgnoreCase(actual)) {
            throw unsupportedValue(directive);
        }
    }

    private static IllegalArgumentException unsupportedValue(String directive) {
        return new IllegalArgumentException("VPN profile has an unsupported value for " + directive + ".");
    }

    private static int parseBoundedInt(String value, int min, int max, String label) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < min || parsed > max) {
                throw new NumberFormatException();
            }
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("VPN profile has an invalid " + label + ".");
        }
    }

    private static boolean isAllowedProto(String value) {
        String proto = value.toLowerCase(Locale.ROOT);
        return "tcp".equals(proto) || "tcp-client".equals(proto)
                || "tcp4".equals(proto) || "tcp4-client".equals(proto)
                || "udp".equals(proto) || "udp4".equals(proto);
    }

    private static boolean isAllowedCipher(String value) {
        String cipher = value.toUpperCase(Locale.ROOT);
        return "AES-128-CBC".equals(cipher) || "AES-192-CBC".equals(cipher)
                || "AES-256-CBC".equals(cipher) || "AES-128-GCM".equals(cipher)
                || "AES-256-GCM".equals(cipher) || "CHACHA20-POLY1305".equals(cipher);
    }

    private static boolean isBase64Character(char c) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z'
                || c >= '0' && c <= '9' || c == '+' || c == '/';
    }

    private static int countOccurrences(String text, String token) {
        int count = 0;
        int from = 0;
        while ((from = text.indexOf(token, from)) >= 0) {
            count++;
            from += token.length();
        }
        return count;
    }

    private static byte[] hexToBytes(String hex) {
        byte[] result = new byte[hex.length() / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
        }
        return result;
    }
}
