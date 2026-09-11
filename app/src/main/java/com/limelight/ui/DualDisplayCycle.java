package com.limelight.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;

/**
 * Cycles Dual display: top only → both (Thor panels / stacked) → bottom only.
 * Same pref as Settings ({@code list_dual_display}). Next stream launch picks it up.
 */
public final class DualDisplayCycle {
    public static final String PREF_KEY = "list_dual_display";

    private DualDisplayCycle() {
    }

    public static String current(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(PREF_KEY, "auto");
        return value != null ? value : "auto";
    }

    public static String nextValue(String current) {
        if ("primary_only".equals(current)) {
            return "auto";
        }
        if ("gamepad_only".equals(current)) {
            return "primary_only";
        }
        return "gamepad_only";
    }

    public static int labelRes(String value) {
        if ("primary_only".equals(value)) {
            return R.string.screen_mode_top;
        }
        if ("gamepad_only".equals(value)) {
            return R.string.screen_mode_bottom;
        }
        return R.string.screen_mode_both;
    }

    public static void bind(TextView button, Context context) {
        if (button == null) {
            return;
        }
        refresh(button, context);
        button.setOnClickListener(v -> {
            String next = nextValue(current(context));
            PreferenceManager.getDefaultSharedPreferences(context)
                    .edit()
                    .putString(PREF_KEY, next)
                    .apply();
            refresh(button, context);
            Toast.makeText(context,
                    context.getString(R.string.screen_mode_toast, context.getString(labelRes(next))),
                    Toast.LENGTH_SHORT).show();
        });
    }

    public static void refresh(TextView button, Context context) {
        if (button == null) {
            return;
        }
        button.setText(labelRes(current(context)));
    }
}
