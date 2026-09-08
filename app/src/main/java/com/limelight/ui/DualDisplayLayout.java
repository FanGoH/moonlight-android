package com.limelight.ui;

import android.app.Activity;
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
 * two-panel devices). A phone or other single-screen device stacks both
 * streams on one panel so the TV and GamePad are visible at once.
 */
public class DualDisplayLayout {
    public enum Mode {
        AUTO,
        DUAL_PANEL,
        STACKED,
        PRIMARY_ONLY
    }

    public final Mode requested;
    public final Mode effective;
    public final Display secondaryDisplay;
    public final int width1;
    public final int height1;
    public final int bitrate1;

    private DualDisplayLayout(Mode requested, Mode effective, Display secondaryDisplay,
                              int width1, int height1, int bitrate1) {
        this.requested = requested;
        this.effective = effective;
        this.secondaryDisplay = secondaryDisplay;
        this.width1 = width1;
        this.height1 = height1;
        this.bitrate1 = bitrate1;
    }

    public boolean wantsSecondStream() {
        return effective == Mode.DUAL_PANEL || effective == Mode.STACKED;
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
        return Mode.AUTO;
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
        if (hostMaxVideoStreams < 2 || effective == Mode.PRIMARY_ONLY) {
            effective = Mode.PRIMARY_ONLY;
            secondary = null;
        }

        int width1;
        int height1;
        if (effective == Mode.DUAL_PANEL && secondary != null) {
            int[] size = displaySize(secondary);
            width1 = size[0];
            height1 = size[1];
        }
        else {
            // Cemu GamePad-sized default; readable under a 16:9 TV pane on a phone.
            width1 = 1280;
            height1 = 720;
        }
        if (height1 % 2 != 0) {
            height1 &= ~1;
        }
        int bitrate1 = Math.max(2000, prefs.bitrate / 4);
        return new DualDisplayLayout(requested, effective, secondary, width1, height1, bitrate1);
    }

    public void apply(LinearLayout streamContainer, StreamView primary, StreamView secondaryView,
                      int primaryWidth, int primaryHeight) {
        if (effective == Mode.STACKED) {
            streamContainer.setOrientation(LinearLayout.VERTICAL);
            streamContainer.setGravity(Gravity.CENTER);
            setWeighted(primary, 5);
            setWeighted(secondaryView, 3);
            secondaryView.setVisibility(View.VISIBLE);
            primary.setDesiredAspectRatio((double) primaryWidth / Math.max(1, primaryHeight));
            secondaryView.setDesiredAspectRatio((double) width1 / Math.max(1, height1));
        }
        else {
            setMatchParent(primary);
            secondaryView.setVisibility(View.GONE);
        }
    }

    private static void setWeighted(StreamView view, int weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, weight);
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Display.Mode mode = display.getMode();
            return new int[] { mode.getPhysicalWidth(), mode.getPhysicalHeight() };
        }
        DisplayMetrics metrics = new DisplayMetrics();
        display.getRealMetrics(metrics);
        return new int[] { metrics.widthPixels, metrics.heightPixels };
    }
}
