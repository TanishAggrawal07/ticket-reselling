package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    private static final String PREFS_NAME        = "ReTixPrefs";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";

    private ViewPager2 viewPager;
    private LinearLayout dotsContainer;
    private TextView btnSkip;
    private TextView btnNext;
    private TextView btnGetStarted;

    private OnboardingAdapter adapter;
    private List<OnboardingSlide> slides;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupSlides();
        setupViewPager();
        setupDots();
        setupClickListeners();
    }

    private void initViews() {
        viewPager = findViewById(R.id.view_pager);
        dotsContainer = findViewById(R.id.dots_container);
        btnSkip = findViewById(R.id.btn_skip);
        btnNext = findViewById(R.id.btn_next);
        btnGetStarted = findViewById(R.id.btn_get_started);
    }

    private void setupSlides() {
        slides = new ArrayList<>();
        slides.add(new OnboardingSlide(
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_subtitle_1),
                getTicketingIcon()
        ));
        slides.add(new OnboardingSlide(
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_subtitle_2),
                getSecurityIcon()
        ));
        slides.add(new OnboardingSlide(
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_subtitle_3),
                getTrustedIcon()
        ));
    }

    private String getTicketingIcon() {
        return "\uD83C\uDFAB"; // Ticket emoji
    }

    private String getSecurityIcon() {
        return "\uD83D\uDD10"; // Lock emoji
    }

    private String getTrustedIcon() {
        return "\u2705"; // Checkmark emoji
    }

    private void setupViewPager() {
        adapter = new OnboardingAdapter(slides);
        viewPager.setAdapter(adapter);
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position);
                updateButtons(position);
            }
        });
    }

    private void setupDots() {
        dotsContainer.removeAllViews();
        for (int i = 0; i < slides.size(); i++) {
            View dot = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dpToPx(10), dpToPx(10)
            );
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0);
            dot.setLayoutParams(params);
            dot.setBackgroundResource(i == 0 ? R.drawable.dot_active : R.drawable.dot_inactive);
            dotsContainer.addView(dot);
        }
    }

    private void updateDots(int position) {
        for (int i = 0; i < dotsContainer.getChildCount(); i++) {
            View dot = dotsContainer.getChildAt(i);
            dot.setBackgroundResource(i == position ? R.drawable.dot_active : R.drawable.dot_inactive);
        }
    }

    private void updateButtons(int position) {
        if (position == slides.size() - 1) {
            // Last slide - show Get Started, hide Next
            btnNext.setVisibility(View.GONE);
            btnGetStarted.setVisibility(View.VISIBLE);
        } else {
            // Other slides - show Next, hide Get Started
            btnNext.setVisibility(View.VISIBLE);
            btnGetStarted.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        btnSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentItem = viewPager.getCurrentItem();
                if (currentItem < slides.size() - 1) {
                    viewPager.setCurrentItem(currentItem + 1);
                }
            }
        });

        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        });
    }

    private void finishOnboarding() {
        // Mark first time as false
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_FIRST_TIME, false).apply();

        // Go to login
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // Inner class for slide data
    public static class OnboardingSlide {
        private String title;
        private String subtitle;
        private String iconEmoji;

        public OnboardingSlide(String title, String subtitle, String iconEmoji) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconEmoji = iconEmoji;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public String getIconEmoji() { return iconEmoji; }
    }

    // Adapter for ViewPager
    private class OnboardingAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {
        private List<OnboardingSlide> slides;

        OnboardingAdapter(List<OnboardingSlide> slides) {
            this.slides = slides;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_onboarding_slide, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnboardingSlide slide = slides.get(position);
            holder.title.setText(slide.getTitle());
            holder.subtitle.setText(slide.getSubtitle());

            // Set emoji icon
            holder.icon.setText(slide.getIconEmoji());
        }

        @Override
        public int getItemCount() {
            return slides.size();
        }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            TextView title;
            TextView subtitle;
            TextView icon;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.onboarding_title);
                subtitle = itemView.findViewById(R.id.onboarding_subtitle);
                icon = itemView.findViewById(R.id.onboarding_icon);
            }
        }
    }
}