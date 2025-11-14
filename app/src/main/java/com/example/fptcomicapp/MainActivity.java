package com.example.fptcomicapp;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.fptcomicapp.util.AuthUtil;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottomNav);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();

        // Setup Bottom Nav
        NavigationUI.setupWithNavController(bottomNav, navController);

        // Check admin role and show/hide admin menu item
        checkAdminRole();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-check admin role when activity resumes
        checkAdminRole();
    }

    private void checkAdminRole() {
        AuthUtil.checkAdminStatus(isAdmin -> {
            MenuItem adminMenuItem = bottomNav.getMenu().findItem(R.id.adminDashboardFragment);
            if (adminMenuItem != null) {
                adminMenuItem.setVisible(isAdmin);
            }
        });
    }

    @Override
    public void onBackPressed() {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
    }
}