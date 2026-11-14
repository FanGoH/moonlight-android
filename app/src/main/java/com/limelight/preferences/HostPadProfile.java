package com.limelight.preferences;

import com.limelight.LimeLog;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.limelight.nvstream.jni.MoonBridge;

/**
 * Optional host-side GameStream pad profile and session (Game Mode vs Desktop)
 * from fgpc (HTTP :8484).
 */
public final class HostPadProfile {
    private static final int FETCH_TIMEOUT_MS = 1200;
    private static volatile Byte cachedLiCtype = null;
    private static volatile String cachedSession = null;
    private static volatile long cacheExpiryMs = 0;
    private static volatile CountDownLatch fetchLatch = new CountDownLatch(0);

    private HostPadProfile() {}

    public static void prefetch(String hostAddress, int fgpcPort) {
        if (hostAddress == null || hostAddress.isEmpty()) {
            return;
        }
        CountDownLatch latch = new CountDownLatch(1);
        fetchLatch = latch;
        new Thread(() -> {
            try {
                fetchFromHost(hostAddress, fgpcPort);
            } finally {
                latch.countDown();
            }
        }, "HostPadProfile").start();
    }

    public static void awaitFetch(long timeoutMs) {
        try {
            fetchLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Game Mode when fgpc says so, or when the session is unknown (safer for
     * GamePad absolute taps). Desktop only when the host reports desktop.
     */
    public static boolean isGameMode() {
        String session = cachedSession;
        if (session == null || session.isEmpty() || "unknown".equals(session)) {
            return true;
        }
        return "gamemode".equals(session);
    }

    public static byte resolveReportedType(
            PreferenceConfiguration prefConfig,
            byte detectedType,
            boolean motionSensorsActive
    ) {
        String mode = prefConfig.gamepadReportType;
        if ("auto".equals(mode)) {
            if (motionSensorsActive && detectedType != MoonBridge.LI_CTYPE_PS) {
                // Stock Moonlight forces UNKNOWN when emulating motion on non-PS pads.
                return detectedType;
            }
            return detectedType;
        }
        if ("host".equals(mode)) {
            Byte host = cachedLiCtype;
            if (host != null && System.currentTimeMillis() < cacheExpiryMs) {
                return host;
            }
            if (motionSensorsActive) {
                return MoonBridge.LI_CTYPE_NINTENDO;
            }
            return detectedType;
        }
        if ("xbox".equals(mode)) {
            return MoonBridge.LI_CTYPE_XBOX;
        }
        if ("nintendo".equals(mode)) {
            return MoonBridge.LI_CTYPE_NINTENDO;
        }
        if ("ps".equals(mode)) {
            return MoonBridge.LI_CTYPE_PS;
        }
        if ("unknown".equals(mode)) {
            return MoonBridge.LI_CTYPE_UNKNOWN;
        }
        return detectedType;
    }

    private static void fetchFromHost(String hostAddress, int fgpcPort) {
        HttpURLConnection conn = null;
        try {
            InetAddress addr = InetAddress.getByName(hostAddress);
            String host = addr.getHostAddress();
            URL url = new URL("http://" + host + ":" + fgpcPort + "/api/moonlight/pad-profile");
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(FETCH_TIMEOUT_MS);
            conn.setReadTimeout(FETCH_TIMEOUT_MS);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            if (code != 200) {
                LimeLog.warning("Host pad profile HTTP " + code);
                return;
            }
            String body = readStream(conn.getInputStream());
            JSONObject obj = new JSONObject(body);
            if (!obj.optBoolean("ok", false)) {
                return;
            }
            if (obj.has("li_ctype") && !obj.isNull("li_ctype")) {
                int li = obj.getInt("li_ctype");
                cachedLiCtype = (byte) li;
            }
            String session = obj.optString("session", "");
            if (session != null && !session.isEmpty()) {
                cachedSession = session;
            }
            cacheExpiryMs = System.currentTimeMillis() + 60_000;
            LimeLog.info("Host pad profile li_ctype=" + obj.opt("li_ctype")
                    + " profile=" + obj.optString("profile")
                    + " session=" + cachedSession);
        } catch (Exception e) {
            LimeLog.warning("Host pad profile fetch failed: " + e.getMessage());
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readStream(InputStream in) throws IOException {
        BufferedInputStream bin = new BufferedInputStream(in);
        byte[] buf = new byte[4096];
        int n = bin.read(buf);
        if (n <= 0) {
            return "";
        }
        return new String(buf, 0, n, StandardCharsets.UTF_8);
    }
}
