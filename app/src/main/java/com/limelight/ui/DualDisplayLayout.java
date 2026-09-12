package com.limelight.ui;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import com.limelight.preferences.PreferenceConfiguration;

/**
 * Picks how two Sunshine DS video streams are shown.
 *
 * Auto uses a second Android display when one exists (AYN Thor and similar
 * two-panel devices). A phone or a clamshell that only exposes one Display
 * (Odin 2 Portal) stacks both streams on that screen. GamePad only is for a
 * single-screen handheld (AYN Odin) that should show the virtual GamePad
 * display rather than the TV. Dual-panel and stacked are alternate layouts
 * of the same two GameStream videos; they are not combined.
 *
 * Second-screen resolution auto-detects the other Android panel, or this
 * device's current display for GamePad-only / stacked. The TV stream stays
 * on Moonlight's resolution setting.
 */
public class DualDisplayLayout {
    public enum Mode {
        AUTO,
        DUAL_PANEL,
        STACKED,
        PRIMARY_ONLY,
        GAMEPAD_ONLY
    }

    public enum StackLayout {
        TV_TOP,
        GAMEPAD_TOP,
        SIDE_TV,
        SIDE_GAMEPAD,
        LARGE_TV,
        LARGE_GAMEPAD
    }

    public final Mode requested;
    public final Mode effective;
    public final StackLayout stackLayout;
    public final Display secondaryDisplay;
    public final int width1;
    public final int height1;
    public final int bitrate1;
    public final boolean stretchSecondary;

    private DualDisplayLayout(Mode requested, Mode effective, StackLayout stackLayout,
                              Display secondaryDisplay, int width1, int height1, int bitrate1,
                              boolean stretchSecondary) {
        this.requested = requested;
        this.effective = effective;
        this.stackLayout = stackLayout;
        this.secondaryDisplay = secondaryDisplay;
        this.width1 = width1;
        this.height1 = height1;
        this.bitrate1 = bitrate1;
        this.stretchSecondary = stretchSecondary;
    }

    public boolean wantsSecondStream() {
        return effective == Mode.DUAL_PANEL || effective == Mode.STACKED;
    }

    /**
     * Odin / single-screen GamePad: one GameStream video slot, capturing the
     * host's second display (Cemu/Azahar panel) instead of the TV.
     */
    public boolean wantsGamepadAsPrimary() {
        return effective == Mode.GAMEPAD_ONLY;
    }

    public static Mode parse(String value) {
        if ("dual_panel".equals(value)) {
            return Mode.DUAL_PANEL;
        }
        if ("stacked".equals(value)) {
            return Mode.STACKED;
        }
        if ("primary_only".equals(value)) {
            return Mode.PRIMARY_ONLY;
        }
        if ("gamepad_only".equals(value)) {
            return Mode.GAMEPAD_ONLY;
        }
        return Mode.AUTO;
    }

    public static StackLayout parseStackLayout(String value) {
        if ("gamepad_top".equals(value)) {
            return StackLayout.GAMEPAD_TOP;
        }
        if ("side_tv".equals(value)) {
            return StackLayout.SIDE_TV;
        }
        if ("side_gamepad".equals(value)) {
            return StackLayout.SIDE_GAMEPAD;
        }
        if ("large_tv".equals(value)) {
            return StackLayout.LARGE_TV;
        }
        if ("large_gamepad".equals(value)) {
            return StackLayout.LARGE_GAMEPAD;
        }
        return StackLayout.TV_TOP;
    }

    public static DualDisplayLayout resolve(Activity activity, PreferenceConfiguration prefs,
                                            int hostMaxVideoStreams) {
        Mode requested = parse(prefs.dualDisplayMode);
        Display secondary = findSecondaryDisplay(activity);
        Mode effective = requested;
        if (requested == Mode.AUTO) {
            effective = secondary != null ? Mode.DUAL_PANEL : Mode.STACKED;
        }
        if (requested == Mode.DUAL_PANEL && secondary == null) {
            effective = Mode.STACKED;
        }
        if (effective == Mode.GAMEPAD_ONLY) {
            // One GameStream slot capturing the host GamePad display. MaxVideoStreams
            // is the video/1 capability; do not require it here or an Odin client
            // whose intent extra is missing silently streams the TV instead.
            secondary = null;
        }
        else if (hostMaxVideoStreams < 2 || effective == Mode.PRIMARY_ONLY) {
            effective = Mode.PRIMARY_ONLY;
            secondary = null;
        }

        int[] size = resolveSecondSize(activity, prefs, effective, secondary);
        int width1 = size[0];
        int height1 = size[1];
        if (width1 % 2 != 0) {
            width1 &= ~1;
        }
        if (height1 % 2 != 0) {
            height1 &= ~1;
        }
        int bitrate1 = Math.max(2000, prefs.bitrate / 4);
        // Host top can be 4K while :2 / GamePad stays 1920x1080. Fill the
        // Thor/Odin bottom panel (stretch). Letterbox was the white bars.
        boolean stretch = prefs.stretchSecondScreen;
        if (effective == Mode.DUAL_PANEL) {
            stretch = true;
        }
        return new DualDisplayLayout(requested, effective, parseStackLayout(prefs.stackLayout),
                secondary, width1, height1, bitrate1, stretch);
    }

    /**
     * Host HDMI and :2 are 60 Hz gamescope. A 120 fps encode request makes
     * x264 emit duplicates and the two Thor panels beat against each other.
     */
    public static int encodeFps(int requestedFps, DualDisplayLayout layout) {
        if (layout != null && layout.wantsSecondStream() && requestedFps > 60) {
            return 60;
        }
        return requestedFps;
    }

    public void apply(LinearLayout streamContainer, StreamView primary, StreamView secondaryView,
                      int primaryWidth, int primaryHeight) {
        if (effective != Mode.STACKED) {
            setMatchParent(primary);
            secondaryView.setVisibility(View.GONE);
            return;
        }

        boolean vertical = stackLayout == StackLayout.TV_TOP ||
                stackLayout == StackLayout.GAMEPAD_TOP ||
                stackLayout == StackLayout.LARGE_TV ||
                stackLayout == StackLayout.LARGE_GAMEPAD;
        boolean gamepadFirst = stackLayout == StackLayout.GAMEPAD_TOP ||
                stackLayout == StackLayout.SIDE_GAMEPAD ||
                stackLayout == StackLayout.LARGE_GAMEPAD;
        int tvWeight = 5;
        int gpWeight = 3;
        if (stackLayout == StackLayout.SIDE_TV || stackLayout == StackLayout.SIDE_GAMEPAD) {
            tvWeight = 1;
            gpWeight = 1;
        }
        else if (stackLayout == StackLayout.LARGE_TV) {
            tvWeight = 7;
            gpWeight = 2;
        }
        else if (stackLayout == StackLayout.LARGE_GAMEPAD) {
            tvWeight = 2;
            gpWeight = 7;
        }

        streamContainer.setOrientation(vertical ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        streamContainer.setGravity(Gravity.CENTER);
        setWeighted(primary, tvWeight, vertical);
        setWeighted(secondaryView, gpWeight, vertical);
        secondaryView.setVisibility(View.VISIBLE);
        primary.setDesiredAspectRatio((double) primaryWidth / Math.max(1, primaryHeight));
        secondaryView.applyFill(width1, height1, stretchSecondary);

        streamContainer.removeView(primary);
        streamContainer.removeView(secondaryView);
        if (gamepadFirst) {
            streamContainer.addView(secondaryView);
            streamContainer.addView(primary);
        }
        else {
            streamContainer.addView(primary);
            streamContainer.addView(secondaryView);
        }
    }

    private static int[] resolveSecondSize(Activity activity, PreferenceConfiguration prefs,
                                           Mode effective, Display secondary) {
        String spec = prefs.secondScreenRes;
        if (spec == null || spec.isEmpty() || "auto".equals(spec)) {
            // Stretching a 16:9 GamePad onto Thor's bottom panel needs the
            // 1080p bitstream. Auto's panel native size (1080×1240) makes
            // Sunshine letterbox/crop that 16:9 capture.
            if (prefs.stretchSecondScreen) {
                return new int[] { prefs.width, prefs.height };
            }
            if (effective == Mode.DUAL_PANEL && secondary != null) {
                return displaySize(secondary);
            }
            return activityDisplaySize(activity);
        }
        if ("stream".equals(spec)) {
            return new int[] { prefs.width, prefs.height };
        }
        int x = spec.indexOf('x');
        if (x > 0 && x < spec.length() - 1) {
            try {
                int w = Integer.parseInt(spec.substring(0, x));
                int h = Integer.parseInt(spec.substring(x + 1));
                if (w > 0 && h > 0) {
                    return new int[] { w, h };
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return activityDisplaySize(activity);
    }

    private static int[] activityDisplaySize(Activity activity) {
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        int width = Math.max(2, metrics.widthPixels);
        int height = Math.max(2, metrics.heightPixels);
        if (activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE ||
                activity.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
            if (height > width) {
                int tmp = width;
                width = height;
                height = tmp;
            }
        }
        return new int[] { width, height };
    }

    private static void setWeighted(StreamView view, int weight, boolean vertical) {
        LinearLayout.LayoutParams params = vertical ?
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, weight) :
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight);
        view.setLayoutParams(params);
    }

    private static void setMatchParent(StreamView view) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        view.setLayoutParams(params);
    }

    public static Display findSecondaryDisplay(Activity activity) {
        DisplayManager manager = (DisplayManager) activity.getSystemService(Activity.DISPLAY_SERVICE);
        if (manager == null) {
            return null;
        }
        Display def = activity.getWindowManager().getDefaultDisplay();
        Display presentation = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            Display[] presentationDisplays = manager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION);
            if (presentationDisplays != null && presentationDisplays.length > 0) {
                presentation = presentationDisplays[0];
            }
        }
        if (presentation != null && presentation.getDisplayId() != def.getDisplayId()) {
            return presentation;
        }
        for (Display display : manager.getDisplays()) {
            if (display.getDisplayId() != def.getDisplayId() && display.isValid()) {
                return display;
            }
        }
        return null;
    }

    private static int[] displaySize(Display display) {
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        return new int[] { Math.max(2, metrics.widthPixels), Math.max(2, metrics.heightPixels) };
    }
}
