package com.tanish.retix.util;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import com.google.android.material.snackbar.Snackbar;
import com.tanish.retix.R;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * UiUtils - Utility class for common UI operations.
 * Provides helper methods for keyboard management, animations, formatting, and more.
 */
public class UiUtils {

    private static final long DEFAULT_ANIMATION_DURATION = 300L;
    private static final float FADE_MIN_ALPHA = 0f;
    private static final float FADE_MAX_ALPHA = 1f;
    private static final float SCALE_MIN = 0.95f;
    private static final float SCALE_NORMAL = 1f;

    private UiUtils() {
        // Utility class - prevent instantiation
    }

    // ============================================================================
    // Keyboard Management
    // ============================================================================

    /**
     * Hides the soft keyboard from the given view.
     *
     * @param view The view that currently has focus
     */
    public static void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Shows the soft keyboard for the given view.
     *
     * @param view The view to receive focus
     */
    public static void showKeyboard(View view) {
        if (view == null) return;
        view.requestFocus();
        InputMethodManager imm = (InputMethodManager) view.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    // ============================================================================
    // Snackbar
    // ============================================================================

    /**
     * Shows a simple Snackbar with the given message.
     *
     * @param view    The view to find a parent from
     * @param message The message to display
     */
    public static void showSnackbar(@NonNull View view, @NonNull String message) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Shows a Snackbar with a string resource.
     *
     * @param view   The view to find a parent from
     * @param resId  The string resource to display
     */
    public static void showSnackbar(@NonNull View view, @StringRes int resId) {
        Snackbar.make(view, resId, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Shows a Snackbar with an action.
     *
     * @param view     The view to find a parent from
     * @param message  The message to display
     * @param action   The action text
     * @param listener The action listener
     */
    public static void showSnackbarWithAction(@NonNull View view, @NonNull String message,
                                               @NonNull String action,
                                               @NonNull View.OnClickListener listener) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                .setAction(action, listener)
                .show();
    }

    // ============================================================================
    // Animations - Fade
    // ============================================================================

    /**
     * Fades in the given view with animation.
     *
     * @param view The view to fade in
     */
    public static void fadeIn(@NonNull View view) {
        fadeIn(view, DEFAULT_ANIMATION_DURATION);
    }

    /**
     * Fades in the given view with custom duration.
     *
     * @param view     The view to fade in
     * @param duration Animation duration in milliseconds
     */
    public static void fadeIn(@NonNull View view, long duration) {
        if (view.getVisibility() == View.VISIBLE) return;
        view.setVisibility(View.VISIBLE);
        AlphaAnimation anim = new AlphaAnimation(FADE_MIN_ALPHA, FADE_MAX_ALPHA);
        anim.setDuration(duration);
        view.startAnimation(anim);
    }

    /**
     * Fades out the given view with animation.
     *
     * @param view The view to fade out
     */
    public static void fadeOut(@NonNull View view) {
        fadeOut(view, DEFAULT_ANIMATION_DURATION);
    }

    /**
     * Fades out the given view with custom duration.
     *
     * @param view     The view to fade out
     * @param duration Animation duration in milliseconds
     */
    public static void fadeOut(@NonNull View view, long duration) {
        if (view.getVisibility() != View.VISIBLE) return;
        AlphaAnimation anim = new AlphaAnimation(FADE_MAX_ALPHA, FADE_MIN_ALPHA);
        anim.setDuration(duration);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);
            }
            @Override public void onAnimationRepeat(Animation animation) {}
        });
        view.startAnimation(anim);
    }

    /**
     * Cross-fades between two views.
     *
     * @param hideView The view to hide
     * @param showView The view to show
     */
    public static void crossFade(@NonNull View hideView, @NonNull View showView) {
        crossFade(hideView, showView, DEFAULT_ANIMATION_DURATION);
    }

    /**
     * Cross-fades between two views with custom duration.
     *
     * @param hideView The view to hide
     * @param showView The view to show
     * @param duration Animation duration in milliseconds
     */
    public static void crossFade(@NonNull View hideView, @NonNull View showView, long duration) {
        fadeOut(hideView, duration);
        fadeIn(showView, duration);
    }

    // ============================================================================
    // Animations - Scale (for button press feedback)
    // ============================================================================

    /**
     * Animates a button press (scale down then back).
     *
     * @param view The view to animate
     */
    public static void animateButtonPress(@NonNull View view) {
        ScaleAnimation scaleDown = new ScaleAnimation(
                SCALE_NORMAL, SCALE_MIN, SCALE_NORMAL, SCALE_MIN,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleDown.setDuration(100);
        scaleDown.setFillAfter(true);

        ScaleAnimation scaleUp = new ScaleAnimation(
                SCALE_MIN, SCALE_NORMAL, SCALE_MIN, SCALE_NORMAL,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleUp.setDuration(100);
        scaleUp.setStartOffset(100);
        scaleUp.setFillAfter(true);

        view.startAnimation(scaleDown);
        view.postDelayed(() -> view.startAnimation(scaleUp), 100);
    }

    // ============================================================================
    // Formatting
    // ============================================================================

    /**
     * Formats a price as currency with the Indian Rupee symbol.
     *
     * @param price The price to format
     * @return Formatted price string (e.g., "₹1,234.56")
     */
    @NonNull
    public static String formatPrice(double price) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        formatter.setCurrency(Currency.getInstance("INR"));
        return formatter.format(price);
    }

    /**
     * Formats a price as currency without decimal places for whole numbers.
     *
     * @param price The price to format
     * @return Formatted price string (e.g., "₹1,234")
     */
    @NonNull
    public static String formatPriceCompact(double price) {
        if (price == Math.floor(price)) {
            // Whole number - no decimals
            return "₹" + NumberFormat.getNumberInstance(new Locale("en", "IN"))
                    .format((int) price);
        }
        return formatPrice(price);
    }

    /**
     * Formats a number with thousand separators.
     *
     * @param number The number to format
     * @return Formatted number string (e.g., "1,234")
     */
    @NonNull
    public static String formatNumber(int number) {
        return NumberFormat.getNumberInstance(new Locale("en", "IN")).format(number);
    }

    // ============================================================================
    // View Visibility Helpers
    // ============================================================================

    /**
     * Sets the view to visible.
     *
     * @param view The view to show
     */
    public static void show(@NonNull View view) {
        view.setVisibility(View.VISIBLE);
    }

    /**
     * Sets the view to invisible (keeps layout space).
     *
     * @param view The view to hide
     */
    public static void hide(@NonNull View view) {
        view.setVisibility(View.INVISIBLE);
    }

    /**
     * Sets the view to gone (doesn't keep layout space).
     *
     * @param view The view to remove from layout
     */
    public static void gone(@NonNull View view) {
        view.setVisibility(View.GONE);
    }

    /**
     * Toggles view visibility between visible and gone.
     *
     * @param view The view to toggle
     * @return true if view is now visible, false otherwise
     */
    public static boolean toggleVisibility(@NonNull View view) {
        if (view.getVisibility() == View.VISIBLE) {
            view.setVisibility(View.GONE);
            return false;
        } else {
            view.setVisibility(View.VISIBLE);
            return true;
        }
    }

    // ============================================================================
    // Activity Transitions
    // ============================================================================

    /**
     * Applies slide-in-right transition for activity entry.
     *
     * @param activity The activity
     */
    public static void slideInRight(@NonNull Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    /**
     * Applies slide-in-left transition (for back navigation).
     *
     * @param activity The activity
     */
    public static void slideInLeft(@NonNull Activity activity) {
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    /**
     * Applies fade transition for activity.
     *
     * @param activity The activity
     */
    public static void fade(@NonNull Activity activity) {
        activity.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    /**
     * Applies no transition for activity.
     *
     * @param activity The activity
     */
    public static void noTransition(@NonNull Activity activity) {
        activity.overridePendingTransition(0, 0);
    }
}
