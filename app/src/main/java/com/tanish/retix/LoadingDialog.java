package com.tanish.retix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class LoadingDialog {

    private AlertDialog dialog;
    private TextView messageText;
    private final Context context;

    public LoadingDialog(Context context) {
        this.context = context;
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_loading, null);
        messageText = view.findViewById(R.id.tv_loading_message);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(view);
        builder.setCancelable(false);
        dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    public void show() {
        if (dialog != null && !dialog.isShowing() && !isContextDestroyed()) {
            dialog.show();
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (Exception ignored) {
                // Activity may already be destroyed; safe to ignore
            }
        }
    }

    public void setMessage(String message) {
        if (messageText != null) {
            messageText.setText(message);
        }
    }

    public void setMessage(int resId) {
        if (messageText != null) {
            messageText.setText(resId);
        }
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    private boolean isContextDestroyed() {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            return activity.isFinishing() || activity.isDestroyed();
        }
        return false;
    }
}
