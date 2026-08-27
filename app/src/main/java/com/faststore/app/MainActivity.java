package com.faststore.app;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private final Fragment homeFragment = new HomeFragment();
    private final Fragment searchFragment = new SearchFragment();
    private final Fragment cartFragment = new CartFragment();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, cartFragment, "cart").hide(cartFragment)
                    .add(R.id.fragmentContainer, searchFragment, "search").hide(searchFragment)
                    .add(R.id.fragmentContainer, homeFragment, "home")
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment target;
            int id = item.getItemId();
            if (id == R.id.nav_search) {
                target = searchFragment;
            } else if (id == R.id.nav_cart) {
                target = cartFragment;
            } else {
                target = homeFragment;
            }
            showOnly(target);
            return true;
        });
    }

    private void showOnly(Fragment target) {
        androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        for (Fragment f : new Fragment[]{homeFragment, searchFragment, cartFragment}) {
            if (f == target) ft.show(f); else ft.hide(f);
        }
        ft.commit();
    }
}
