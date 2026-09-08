package com.limelight.ui;

import android.app.Presentation;
import android.content.Context;
import android.os.Bundle;
import android.view.Display;
import android.view.Gravity;
import android.widget.FrameLayout;

/**
 * Hosts the second Sunshine DS video stream on another Android display
 * (AYN Thor bottom panel, HDMI, or a wireless display).
 */
public class DualDisplayPresentation extends Presentation {
    private StreamView streamView;

    public DualDisplayPresentation(Context outerContext, Display display) {
        super(outerContext, display);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FrameLayout root = new FrameLayout(getContext());
        streamView = new StreamView(getContext());
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER);
        root.addView(streamView, params);
        setContentView(root);
    }

    public StreamView getStreamView() {
        return streamView;
    }
}
