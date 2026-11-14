package com.limelight.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.TextView;
import android.widget.Toast;

import com.limelight.R;

/**
 * Cycles reported controller type: Host → Switch → Xbox.
 * Same pref as Settings ({@code list_gamepad_report_type}). Next stream launch picks it up.
 */
public final class PadProfileCycle {
    public static final String PREF_KEY = "list_gamepad_report_type";

    private PadProfileCycle() {
    }

    public static String current(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String value = prefs.getString(PREF_KEY, "host");
        return value != null ? value : "host";
    }

    public static String nextValue(String current) {
        if ("host".equals(current)) {
            return "nintendo";
        }
        if ("nintendo".equals(current)) {
            return "xbox";
        }
        // xbox, auto, ps, unknown, or anything else → host
        return "host";
    }

    public static int labelRes(String value) {
        if ("nintendo".equals(value)) {
            return R.string.pad_mode_switch;
        }
        if ("xbox".equals(value)) {
            return R.string.pad_mode_xbox;
        }
        if ("ps".equals(value)) {
            return R.string.pad_mode_ps;
        }
        if ("auto".equals(value)) {
            return R.string.pad_mode_auto;
        }
        // host (default) and unknown
        return R.string.pad_mode_host;
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
                    context.getString(R.string.pad_mode_toast, context.getString(labelRes(next))),
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
