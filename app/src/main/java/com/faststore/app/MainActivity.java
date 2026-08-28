package com.faststore.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
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

        Toolbar toolbar = findViewById(R.id.mainToolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String asset = null;
        String title = null;

        if (id == R.id.action_privacy) {
            asset = "privacy_policy.html";
            title = "Privacy Policy";
        } else if (id == R.id.action_terms) {
            asset = "terms_conditions.html";
            title = "Terms & Conditions";
        } else if (id == R.id.action_contact) {
            asset = "contact_us.html";
            title = "Contact Us";
        } else if (id == R.id.action_about) {
            asset = "about.html";
            title = "About FastStore";
        }

        if (asset != null) {
            Intent intent = new Intent(this, StaticPageActivity.class);
            intent.putExtra("asset", asset);
            intent.putExtra("title", title);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
