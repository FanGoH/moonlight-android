package com.limelight.ui;

import android.app.Presentation;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;

/**
 * Hosts the second Sunshine DS video stream on another Android display
 * (AYN Thor bottom panel, HDMI, or a wireless display).
 */
public class DualDisplayPresentation extends Presentation {
    private View backgroundTouchView;
    private StreamView streamView;

    public DualDisplayPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(getContext());
        backgroundTouchView = new View(getContext());
        FrameLayout.LayoutParams fill = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(backgroundTouchView, fill);
        streamView = new StreamView(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        root.addView(streamView, params);
        setContentView(root);
        prefer60Hz();
    }

    /**
     * Thor's GamePad LCD is 120 Hz; the host capture is 60 Hz. Pinning the
     * Presentation to 60 Hz stops a 120/60 beat flicker on the bottom panel.
     */
    private void prefer60Hz() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        Display display = getDisplay();
        Window window = getWindow();
        if (display == null || window == null) {
            return;
        }
        Display.Mode best = null;
        float bestDelta = Float.MAX_VALUE;
        for (Display.Mode mode : display.getSupportedModes()) {
            float delta = Math.abs(mode.getRefreshRate() - 60f);
            if (delta < bestDelta) {
                bestDelta = delta;
                best = mode;
            }
        }
        if (best == null || bestDelta > 2f) {
            return;
        }
        WindowManager.LayoutParams attrs = window.getAttributes();
        attrs.preferredDisplayModeId = best.getModeId();
        window.setAttributes(attrs);
    }

    public View getBackgroundTouchView() {
        return backgroundTouchView;
    }

    public StreamView getStreamView() {
        return streamView;
    }
}
