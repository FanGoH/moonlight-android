package com.limelight.ui;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
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
        // Do not pin this Display to 60 Hz. preferredDisplayModeId on Thor's
        // GamePad LCD dismisses the Presentation; bottom taps then hit the
        // top Activity (display 0 / TV) and GamePad inject never runs.
        // Host :2 stays 1920x1080 — do not resize the virtual display.
    }

    public View getBackgroundTouchView() {
        return backgroundTouchView;
    }

    public StreamView getStreamView() {
        return streamView;
    }
}
