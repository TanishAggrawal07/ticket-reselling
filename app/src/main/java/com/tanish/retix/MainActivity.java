package com.tanish.retix;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.tanish.retix.databinding.ActivityMainBinding;

/**
 * MainActivity - Main app container with BottomNavigationView.
 * Hosts Home, Tickets, Wallet, and Profile fragments.
 * Uses ViewBinding for type-safe view access.
 */
public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnFragmentInteractionListener {

    private static final String PREFS_NAME    = "ReTixPrefs";
    private static final String KEY_IS_FIRST_TIME = "is_first_time";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private ActivityMainBinding binding;
    private FragmentManager fragmentManager;

    private HomeFragment homeFragment;
    private TicketsFragment ticketsFragment;
    private WalletFragment walletFragment;
    private ProfileFragment profileFragment;

    // Flag to track whether we need to refresh home data on resume
    private boolean needsHomeRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Check auth state and route before showing UI
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean isFirstTime = prefs.getBoolean(KEY_IS_FIRST_TIME, true);
        boolean isLoggedIn = ApiClient.isLoggedIn();

        // Sync local flag with actual token state
        if (!isLoggedIn && prefs.getBoolean(KEY_IS_LOGGED_IN, false)) {
            prefs.edit().putBoolean(KEY_IS_LOGGED_IN, false).apply();
        }

        // Route to appropriate screen if not logged in or first time
        if (isFirstTime) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        } else if (!isLoggedIn) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        super.onCreate(savedInstanceState);

        // Initialize ViewBinding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initFragments();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(homeFragment);
            binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initFragments() {
        homeFragment    = HomeFragment.newInstance();
        ticketsFragment = TicketsFragment.newInstance();
        walletFragment  = WalletFragment.newInstance();
        profileFragment = ProfileFragment.newInstance();
        fragmentManager = getSupportFragmentManager();
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_home)    { loadFragment(homeFragment);    return true; }
            else if (id == R.id.nav_tickets)    { loadFragment(ticketsFragment);    return true; }
            else if (id == R.id.nav_wallet)  { loadFragment(walletFragment);  return true; }
            else if (id == R.id.nav_profile) { loadFragment(profileFragment); return true; }
            return false;
        });
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction tx = fragmentManager.beginTransaction();
        tx.setCustomAnimations(R.anim.fade_in, R.anim.fade_out);
        tx.replace(R.id.fragment_container, fragment);
        tx.commit();
    }

    // ── HomeFragment.OnFragmentInteractionListener ────────────────────────────

    @Override
    public void onSellTicketClick() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_tickets);
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        needsHomeRefresh = true;
        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("ticket", ticket);
        startActivity(intent);
    }


    /**
     * Switches the bottom navigation to the Home tab and refreshes ticket data.
     * Called by SellFragment after a ticket is successfully listed.
     */
    public void switchToHome() {
        binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
        if (homeFragment != null) homeFragment.refreshData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (needsHomeRefresh && homeFragment != null) {
            homeFragment.refreshData();
            needsHomeRefresh = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
