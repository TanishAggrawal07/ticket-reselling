package com.tanish.retix;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements
        HomeFragment.OnFragmentInteractionListener,
        ProfileFragment.OnProfileInteractionListener {

    private static final String PREFS_NAME    = "ReTixPrefs";
    private static final String KEY_DARK_MODE = "dark_mode_enabled";

    private BottomNavigationView bottomNavigation;
    private FragmentManager fragmentManager;

    private HomeFragment homeFragment;
    private SellFragment sellFragment;
    private WalletFragment walletFragment;
    private ProfileFragment profileFragment;

    // Flag to track whether we need to refresh home data on resume
    private boolean needsHomeRefresh = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initFragments();
        setupBottomNavigation();

        if (savedInstanceState == null) {
            loadFragment(homeFragment);
            bottomNavigation.setSelectedItemId(R.id.nav_home);
        }
    }

    private void initFragments() {
        homeFragment    = HomeFragment.newInstance();
        sellFragment    = SellFragment.newInstance();
        walletFragment  = WalletFragment.newInstance();
        profileFragment = ProfileFragment.newInstance();
        fragmentManager = getSupportFragmentManager();
    }

    private void setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if      (id == R.id.nav_home)    { loadFragment(homeFragment);    return true; }
            else if (id == R.id.nav_sell)    { loadFragment(sellFragment);    return true; }
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
        bottomNavigation.setSelectedItemId(R.id.nav_sell);
    }

    /** Settings icon in the Home toolbar → open SettingsActivity */
    @Override
    public void onSettingsFromHomeClick() {
        needsHomeRefresh = true;
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onTicketClick(Ticket ticket) {
        needsHomeRefresh = true;
        Intent intent = new Intent(this, TicketDetailActivity.class);
        intent.putExtra("ticket", ticket);
        startActivity(intent);
    }

    // ── ProfileFragment.OnProfileInteractionListener ──────────────────────────

    @Override
    public void onSettingsClick() {
        needsHomeRefresh = false;
        startActivity(new Intent(this, SettingsActivity.class));
    }

    @Override
    public void onMyListingsClick() {
        bottomNavigation.setSelectedItemId(R.id.nav_sell);
    }

    @Override
    public void onMyPurchasesClick() {
        bottomNavigation.setSelectedItemId(R.id.nav_wallet);
    }

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    protected void onResume() {
        super.onResume();
        // Only refresh home data when returning from a detail/external screen,
        // not on every resume (e.g., fragment switches don't need a reload).
        if (needsHomeRefresh && homeFragment != null) {
            homeFragment.refreshData();
            needsHomeRefresh = false;
        }
    }
}
