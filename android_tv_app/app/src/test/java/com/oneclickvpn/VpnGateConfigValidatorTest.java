package com.oneclickvpn;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VpnGateConfigValidatorTest {
    private static final String SAMPLE_IP = "219.100.37.165";

    @Test
    public void currentVpnGateProfileIsAcceptedAndHardened() throws Exception {
        String hardened = VpnGateConfigValidator.validateAndHarden(sampleConfig(), SAMPLE_IP);

        assertTrue(hardened.contains("remote-cert-tls server"));
        assertTrue(hardened.contains("verify-x509-name opengw.net name"));
        assertTrue(hardened.contains("tls-version-min 1.2"));
        assertTrue(hardened.contains("block-ipv6"));
    }

    @Test
    public void commandDirectiveIsRejected() throws Exception {
        String unsafe = sampleConfig() + "\nscript-security 2\nup /data/local/tmp/run\n";

        assertThrows(IllegalArgumentException.class,
                () -> VpnGateConfigValidator.validateAndHarden(unsafe, SAMPLE_IP));
    }

    @Test
    public void remoteThatDoesNotMatchDirectoryIsRejected() throws Exception {
        String unsafe = sampleConfig().replace(
                "remote " + SAMPLE_IP + " 443", "remote 203.0.113.10 443");

        assertThrows(IllegalArgumentException.class,
                () -> VpnGateConfigValidator.validateAndHarden(unsafe, SAMPLE_IP));
    }

    @Test
    public void changedCaIsRejected() throws Exception {
        String unsafe = sampleConfig().replaceFirst("MIIFaz", "NIIFaz");

        assertThrows(IllegalArgumentException.class,
                () -> VpnGateConfigValidator.validateAndHarden(unsafe, SAMPLE_IP));
    }

    @Test
    public void duplicateRemoteIsRejected() throws Exception {
        String unsafe = sampleConfig() + "\nremote " + SAMPLE_IP + " 443\n";

        assertThrows(IllegalArgumentException.class,
                () -> VpnGateConfigValidator.validateAndHarden(unsafe, SAMPLE_IP));
    }

    @Test
    public void base64AndIpv4ValidationRejectMalformedValues() {
        VpnGateConfigValidator.validateBase64("QUJDRA==");

        assertThrows(IllegalArgumentException.class,
                () -> VpnGateConfigValidator.validateBase64("QUJD*==="));
        assertTrue(VpnGateConfigValidator.isIpv4("192.0.2.1"));
        assertFalse(VpnGateConfigValidator.isIpv4("192.0.2.999"));
        assertFalse(VpnGateConfigValidator.isIpv4("example.com"));
    }

    private String sampleConfig() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/vpngate-sample.ovpn")) {
            if (input == null) {
                throw new IllegalStateException("Missing VPN Gate test profile.");
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
