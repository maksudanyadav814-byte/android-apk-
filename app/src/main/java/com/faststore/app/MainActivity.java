package com.faststore.app;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * FastStore - Screens file (2 of 2 Java files in this project).
 * Contains the Home/Search/Cart bottom-nav shell (MainActivity, top level)
 * plus every other screen as a public static nested class:
 * ProductDetailActivity, WebViewActivity, ZoomImageActivity, StaticPageActivity,
 * HomeFragment, SearchFragment, CartFragment.
 * Data/model/adapter code lives in AppData.java.
 */
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
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, cartFragment, "cart").hide(cartFragment)
                    .add(R.id.fragmentContainer, searchFragment, "search").hide(searchFragment)
                    .add(R.id.fragmentContainer, homeFragment, "home")
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment target = (id == R.id.nav_search) ? searchFragment : (id == R.id.nav_cart) ? cartFragment : homeFragment;
            androidx.fragment.app.FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            for (Fragment f : new Fragment[]{homeFragment, searchFragment, cartFragment}) {
                if (f == target) ft.show(f); else ft.hide(f);
            }
            ft.commit();
            return true;
        });
    }

    /** Exit confirmation (Yes/No) instead of closing the app instantly on back press. */
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit FastStore?")
                .setMessage("Are you sure you want to close the app?")
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String asset = null, title = null;
        if (id == R.id.action_privacy) { asset = "privacy_policy.html"; title = "Privacy Policy"; }
        else if (id == R.id.action_terms) { asset = "terms_conditions.html"; title = "Terms & Conditions"; }
        else if (id == R.id.action_disclaimer) { asset = "disclaimer.html"; title = "Disclaimer"; }
        else if (id == R.id.action_contact) { asset = "contact_us.html"; title = "Contact Us"; }
        else if (id == R.id.action_about) { asset = "about.html"; title = "About FastStore"; }

        if (asset != null) {
            Intent intent = new Intent(this, StaticPageActivity.class);
            intent.putExtra("asset", asset);
            intent.putExtra("title", title);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // =====================================================================
    // HOME FRAGMENT - categories + product grid, server-side paging & filter
    // =====================================================================
    public static class HomeFragment extends Fragment {
        private RecyclerView recyclerView, categoryRecycler;
        private SwipeRefreshLayout swipeRefresh;
        private ProgressBar progressBar;
        private AppData.ProductAdapter adapter;
        private AppData.CategoryAdapter categoryAdapter;
        private final List<AppData.Product> productList = new ArrayList<>();
        private RequestQueue requestQueue;
        private int currentPage = 1;
        private boolean isLoading = false;
        private boolean hasMore = true;
        private String selectedCategory = "All";

        @Nullable @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_home, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            recyclerView = view.findViewById(R.id.recyclerView);
            categoryRecycler = view.findViewById(R.id.categoryRecycler);
            swipeRefresh = view.findViewById(R.id.swipeRefresh);
            progressBar = view.findViewById(R.id.progressBar);

            adapter = new AppData.ProductAdapter(requireContext(), productList);
            GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 2);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.setAdapter(adapter);

            categoryRecycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

            requestQueue = Volley.newRequestQueue(requireContext());

            // SwipeRefreshLayout only triggers when the list is scrolled to the top (built-in androidx behavior)
            swipeRefresh.setOnRefreshListener(() -> {
                currentPage = 1;
                hasMore = true;
                productList.clear();
                loadProducts(true);
                loadCategories();
            });

            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                    if (dy <= 0 || isLoading || !hasMore) return;
                    int visible = layoutManager.getChildCount();
                    int total = layoutManager.getItemCount();
                    int firstVisible = layoutManager.findFirstVisibleItemPosition();
                    if ((visible + firstVisible) >= total - 2) {
                        loadProducts(false);
                    }
                }
            });

            loadCategories();
            loadProducts(false);
        }

        private void loadCategories() {
            String url = AppData.API_BASE + "?action=categories";
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        List<String> categories = new ArrayList<>();
                        categories.add("All");
                        try {
                            if (response.has("categories")) {
                                JSONArray arr = response.getJSONArray("categories");
                                for (int i = 0; i < arr.length(); i++) categories.add(arr.getString(i));
                            }
                        } catch (Exception ignored) { }

                        categoryAdapter = new AppData.CategoryAdapter(categories, selectedCategory, category -> {
                            if (category.equals(selectedCategory)) return;
                            selectedCategory = category;
                            currentPage = 1;
                            hasMore = true;
                            productList.clear();
                            adapter.notifyDataSetChanged();
                            loadProducts(false);
                        });
                        categoryRecycler.setAdapter(categoryAdapter);
                    },
                    error -> { /* categories are optional - grid still works without chips */ });
            requestQueue.add(request);
        }

        private void loadProducts(boolean isRefresh) {
            isLoading = true;
            if (!isRefresh) progressBar.setVisibility(View.VISIBLE);

            StringBuilder urlBuilder = new StringBuilder(AppData.API_BASE + "?action=products&page=" + currentPage);
            if (!selectedCategory.equals("All")) {
                try {
                    urlBuilder.append("&category=").append(URLEncoder.encode(selectedCategory, StandardCharsets.UTF_8.toString()));
                } catch (Exception ignored) { }
            }

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, urlBuilder.toString(), null,
                    response -> {
                        try {
                            if (response.has("products")) {
                                List<AppData.Product> fetched = AppData.ProductParser.parseList(response.getJSONArray("products"));
                                if (fetched.isEmpty()) {
                                    hasMore = false;
                                } else {
                                    productList.addAll(fetched);
                                    currentPage++;
                                }
                                adapter.notifyDataSetChanged();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                    },
                    error -> {
                        isLoading = false;
                        progressBar.setVisibility(View.GONE);
                        swipeRefresh.setRefreshing(false);
                        if (getContext() != null) Toast.makeText(getContext(), "Failed to load products", Toast.LENGTH_SHORT).show();
                    });

            requestQueue.add(request);
        }
    }

    // =====================================================================
    // SEARCH FRAGMENT - server-side search (action=search)
    // =====================================================================
    public static class SearchFragment extends Fragment {
        private EditText editSearch;
        private RecyclerView searchRecycler;
        private TextView txtEmpty;
        private ProgressBar progressBar;
        private AppData.ProductAdapter adapter;
        private final List<AppData.Product> results = new ArrayList<>();
        private RequestQueue requestQueue;
        private final Handler debounceHandler = new Handler(Looper.getMainLooper());
        private Runnable pendingSearch;

        @Nullable @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_search, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            editSearch = view.findViewById(R.id.editSearch);
            searchRecycler = view.findViewById(R.id.searchRecycler);
            txtEmpty = view.findViewById(R.id.txtSearchEmpty);
            progressBar = view.findViewById(R.id.searchProgress);

            requestQueue = Volley.newRequestQueue(requireContext());
            adapter = new AppData.ProductAdapter(requireContext(), results);
            searchRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 2));
            searchRecycler.setAdapter(adapter);

            editSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (pendingSearch != null) debounceHandler.removeCallbacks(pendingSearch);
                    String query = s.toString();
                    pendingSearch = () -> performSearch(query);
                    debounceHandler.postDelayed(pendingSearch, 350);
                }
                @Override public void afterTextChanged(Editable s) { }
            });
        }

        private void performSearch(String query) {
            if (query == null || query.trim().isEmpty()) {
                results.clear();
                adapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText("Type something to find great deals ✨");
                return;
            }
            progressBar.setVisibility(View.VISIBLE);
            txtEmpty.setVisibility(View.GONE);

            String encoded;
            try { encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString()); }
            catch (Exception e) { encoded = query.trim(); }

            String url = AppData.API_BASE + "?action=search&q=" + encoded;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        results.clear();
                        try {
                            if (response.has("products")) results.addAll(AppData.ProductParser.parseList(response.getJSONArray("products")));
                        } catch (Exception ignored) { }
                        progressBar.setVisibility(View.GONE);
                        if (results.isEmpty()) {
                            txtEmpty.setVisibility(View.VISIBLE);
                            txtEmpty.setText("No products found for \"" + query + "\" 😕");
                        } else {
                            txtEmpty.setVisibility(View.GONE);
                        }
                        adapter.notifyDataSetChanged();
                    },
                    error -> {
                        progressBar.setVisibility(View.GONE);
                        txtEmpty.setVisibility(View.VISIBLE);
                        txtEmpty.setText("Search failed - check your connection");
                    });
            requestQueue.add(request);
        }
    }

    // =====================================================================
    // CART FRAGMENT - Cart / Liked tabs, pull-to-refresh reload
    // =====================================================================
    public static class CartFragment extends Fragment {
        private RecyclerView cartRecycler;
        private TextView txtEmpty;
        private TabLayout tabLayout;
        private SwipeRefreshLayout swipeRefresh;
        private AppData.CartAdapter adapter;
        private final List<AppData.Product> items = new ArrayList<>();
        private boolean showingLiked = false;

        @Nullable @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_cart, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            cartRecycler = view.findViewById(R.id.cartRecycler);
            txtEmpty = view.findViewById(R.id.txtCartEmpty);
            tabLayout = view.findViewById(R.id.cartTabLayout);
            swipeRefresh = view.findViewById(R.id.cartSwipeRefresh);

            adapter = new AppData.CartAdapter(requireContext(), items, this::refreshEmptyState, showingLiked);
            cartRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
            cartRecycler.setAdapter(adapter);

            swipeRefresh.setOnRefreshListener(() -> {
                loadItems();
                swipeRefresh.setRefreshing(false);
            });

            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override public void onTabSelected(TabLayout.Tab tab) {
                    showingLiked = tab.getPosition() == 1;
                    adapter.setLikedMode(showingLiked);
                    loadItems();
                }
                @Override public void onTabUnselected(TabLayout.Tab tab) { }
                @Override public void onTabReselected(TabLayout.Tab tab) { }
            });

            loadItems();
        }

        @Override
        public void onResume() {
            super.onResume();
            loadItems();
        }

        private void loadItems() {
            items.clear();
            items.addAll(showingLiked ? AppData.CartManager.getFavoriteItems(requireContext()) : AppData.CartManager.getCartItems(requireContext()));
            adapter.notifyDataSetChanged();
            refreshEmptyState();
        }

        private void refreshEmptyState() {
            if (items.isEmpty()) {
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText(showingLiked ? "No liked products yet ❤️\nTap the heart icon on any product!" : "Your cart is empty 🛍️\nGo add some cool stuff!");
            } else {
                txtEmpty.setVisibility(View.GONE);
            }
        }
    }

    // =====================================================================
    // PRODUCT DETAIL ACTIVITY - fetches action=product_details (+ related)
    // =====================================================================
    public static class ProductDetailActivity extends AppCompatActivity {
        private AppData.Product product;
        private RequestQueue requestQueue;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_product_detail);

            String id = getIntent().getStringExtra("id");
            if (id == null) { finish(); return; }

            Toolbar toolbar = findViewById(R.id.toolbarDetail);
            toolbar.setNavigationOnClickListener(v -> finish());

            requestQueue = Volley.newRequestQueue(this);
            loadDetail(id);
        }

        private void loadDetail(String id) {
            String url = AppData.API_BASE + "?action=product_details&id=" + id;
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    response -> {
                        try {
                            if (response.optBoolean("success", false) && response.has("product")) {
                                product = AppData.ProductParser.parse(response.getJSONObject("product"));
                                List<AppData.Product> related = response.has("related")
                                        ? AppData.ProductParser.parseList(response.getJSONArray("related"))
                                        : new ArrayList<>();
                                populateUI(related);
                            } else {
                                Toast.makeText(this, "Product not found", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Could not load product", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    },
                    error -> {
                        Toast.makeText(this, "Failed to load product details", Toast.LENGTH_SHORT).show();
                        finish();
                    });
            requestQueue.add(request);
        }

        private void populateUI(List<AppData.Product> related) {
            ViewPager2 imagePager = findViewById(R.id.imagePager);
            LinearLayout dotsIndicator = findViewById(R.id.dotsIndicator);
            TextView txtName = findViewById(R.id.txtDetailName);
            TextView txtCategory = findViewById(R.id.txtDetailCategory);
            TextView txtPrice = findViewById(R.id.txtDetailPrice);
            TextView txtOldPrice = findViewById(R.id.txtDetailOldPrice);
            TextView txtDiscount = findViewById(R.id.txtDetailDiscount);
            TextView txtDescription = findViewById(R.id.txtDetailDescription);
            ImageButton btnFavorite = findViewById(R.id.btnDetailFavorite);
            android.widget.Button btnAddToCart = findViewById(R.id.btnDetailAddToCart);
            android.widget.Button btnBuyNow = findViewById(R.id.btnDetailBuyNow);
            RecyclerView relatedRecycler = findViewById(R.id.relatedRecycler);

            List<String> images = new ArrayList<>();
            if (product.getPicture() != null && !product.getPicture().trim().isEmpty()) images.add(product.getPicture());
            imagePager.setAdapter(new AppData.ImagePagerAdapter(this, images));
            setupDots(dotsIndicator, images.size(), 0);
            imagePager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override public void onPageSelected(int position) { setupDots(dotsIndicator, images.size(), position); }
            });

            txtName.setText(product.getName());
            txtCategory.setText(product.getCategory().isEmpty() ? "" : "Category: " + product.getCategory());
            txtCategory.setVisibility(product.getCategory().isEmpty() ? View.GONE : View.VISIBLE);
            txtPrice.setText(product.currencySymbol() + product.getPrice());

            int discount = product.getDiscountPercent();
            if (discount > 0) {
                txtOldPrice.setVisibility(View.VISIBLE);
                txtOldPrice.setText(product.currencySymbol() + product.getOldPrice());
                txtOldPrice.setPaintFlags(txtOldPrice.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                txtDiscount.setVisibility(View.VISIBLE);
                txtDiscount.setText(discount + "% OFF");
            } else {
                txtOldPrice.setVisibility(View.GONE);
                txtDiscount.setVisibility(View.GONE);
            }

            txtDescription.setText(product.getSpecsText());

            updateFavoriteIcon(btnFavorite);
            btnFavorite.setOnClickListener(v -> {
                AppData.CartManager.toggleFavorite(this, product);
                updateFavoriteIcon(btnFavorite);
            });

            btnAddToCart.setOnClickListener(v -> {
                AppData.CartManager.addToCart(this, product);
                Toast.makeText(this, "Added to Cart! 🛒", Toast.LENGTH_SHORT).show();
            });

            btnBuyNow.setOnClickListener(v -> AppData.openBuyNow(this, product));

            relatedRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            relatedRecycler.setAdapter(new AppData.RelatedProductAdapter(this, related));
        }

        private void setupDots(LinearLayout dotsIndicator, int count, int selected) {
            if (count <= 1) { dotsIndicator.setVisibility(View.GONE); return; }
            dotsIndicator.setVisibility(View.VISIBLE);
            dotsIndicator.removeAllViews();
            for (int i = 0; i < count; i++) {
                android.widget.ImageView dot = new android.widget.ImageView(this);
                android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
                shape.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                shape.setSize(16, 16);
                shape.setColor(i == selected ? 0xFFFF6B35 : 0x55FFFFFF);
                dot.setImageDrawable(shape);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
                params.setMargins(4, 0, 4, 0);
                dot.setLayoutParams(params);
                dotsIndicator.addView(dot);
            }
        }

        private void updateFavoriteIcon(ImageButton btnFavorite) {
            btnFavorite.setImageResource(AppData.CartManager.isFavorite(this, product.getId()) ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        }
    }

    // =====================================================================
    // WEBVIEW ACTIVITY - in-app browser for Buy Now / product links
    // =====================================================================
    public static class WebViewActivity extends AppCompatActivity {
        private WebView webView;
        private String currentUrl;
        private ValueCallback<Uri[]> filePathCallback;

        private final ActivityResultLauncher<Intent> fileChooserLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (filePathCallback == null) return;
                    Uri[] resultsArr = null;
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        String dataString = result.getData().getDataString();
                        if (dataString != null) {
                            resultsArr = new Uri[]{Uri.parse(dataString)};
                        } else if (result.getData().getClipData() != null) {
                            int count = result.getData().getClipData().getItemCount();
                            resultsArr = new Uri[count];
                            for (int i = 0; i < count; i++) resultsArr[i] = result.getData().getClipData().getItemAt(i).getUri();
                        }
                    }
                    filePathCallback.onReceiveValue(resultsArr);
                    filePathCallback = null;
                });

        private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(), grantResults -> { });

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_webview);
            requestRuntimePermissionsIfNeeded();

            Toolbar toolbar = findViewById(R.id.toolbarWeb);
            toolbar.setNavigationOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); else finish(); });
            setSupportActionBar(toolbar);
            toolbar.setOverflowIcon(ContextCompat.getDrawable(this, R.drawable.ic_more_vert));

            ProgressBar progressBar = findViewById(R.id.webProgress);
            webView = findViewById(R.id.webView);

            String url = getIntent().getStringExtra("url");
            String title = getIntent().getStringExtra("title");
            currentUrl = url;
            if (title != null) toolbar.setTitle(title);

            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            settings.setLoadWithOverviewMode(true);
            settings.setUseWideViewPort(true);
            settings.setSupportZoom(true);
            settings.setBuiltInZoomControls(true);
            settings.setDisplayZoomControls(false);
            settings.setMediaPlaybackRequiresUserGesture(false);
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            CookieManager.getInstance().setAcceptCookie(true);
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

            webView.setWebChromeClient(new WebChromeClient() {
                @Override
                public void onProgressChanged(WebView view, int newProgress) {
                    progressBar.setProgress(newProgress);
                    progressBar.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                }

                @Override
                public void onReceivedTitle(WebView view, String pageTitle) {
                    if (getIntent().getStringExtra("title") == null && pageTitle != null) toolbar.setTitle(pageTitle);
                }

                @Override
                public void onPermissionRequest(final PermissionRequest request) {
                    runOnUiThread(() -> request.grant(request.getResources()));
                }

                @Override
                public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> callback, FileChooserParams params) {
                    filePathCallback = callback;
                    try {
                        fileChooserLauncher.launch(params.createIntent());
                    } catch (Exception e) {
                        filePathCallback = null;
                        Toast.makeText(WebViewActivity.this, "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    return true;
                }
            });

            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, @NonNull android.webkit.WebResourceRequest request) {
                    String loadUrl = request.getUrl().toString();
                    if (isExternalAppLink(loadUrl)) { openExternally(loadUrl); return true; }
                    currentUrl = loadUrl;
                    return false;
                }

                @Override
                public void onPageFinished(WebView view, String finishedUrl) {
                    super.onPageFinished(view, finishedUrl);
                    currentUrl = finishedUrl;
                }
            });

            webView.setDownloadListener((dUrl, userAgent, contentDisposition, mimeType, contentLength) -> {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(dUrl));
                    request.setMimeType(mimeType);
                    request.addRequestHeader("User-Agent", userAgent);
                    request.setDescription("Downloading file...");
                    String fileName = URLUtil.guessFileName(dUrl, contentDisposition, mimeType);
                    request.setTitle(fileName);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) dm.enqueue(request);
                    Toast.makeText(this, "Download started ⬇️", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Could not start download", Toast.LENGTH_SHORT).show();
                }
            });

            if (url != null) webView.loadUrl(url);
        }

        private void requestRuntimePermissionsIfNeeded() {
            List<String> toRequest = new ArrayList<>();
            for (String perm : new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}) {
                if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) toRequest.add(perm);
            }
            if (!toRequest.isEmpty()) permissionLauncher.launch(toRequest.toArray(new String[0]));
        }

        private boolean isExternalAppLink(String url) {
            if (url == null) return false;
            String lower = url.toLowerCase();
            return lower.startsWith("tel:") || lower.startsWith("mailto:") || lower.startsWith("sms:")
                    || lower.startsWith("intent:") || lower.startsWith("upi:")
                    || lower.contains("wa.me") || lower.contains("whatsapp.com")
                    || lower.contains("facebook.com") || lower.contains("fb.com")
                    || lower.contains("youtube.com") || lower.contains("youtu.be")
                    || lower.contains("instagram.com") || lower.contains("twitter.com") || lower.contains("x.com")
                    || lower.contains("t.me") || lower.contains("telegram.me");
        }

        private void openExternally(String url) {
            try {
                Intent intent = url.toLowerCase().startsWith("intent:")
                        ? Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        : new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            } catch (ActivityNotFoundException | java.net.URISyntaxException e) {
                Toast.makeText(this, "No app found to open this link", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            getMenuInflater().inflate(R.menu.webview_menu, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(@NonNull MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_open_browser) {
                openExternally(currentUrl != null ? currentUrl : webView.getUrl());
                return true;
            } else if (id == R.id.action_share) {
                String shareUrl = currentUrl != null ? currentUrl : webView.getUrl();
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareUrl);
                startActivity(Intent.createChooser(shareIntent, "Share via"));
                return true;
            } else if (id == R.id.action_reload) {
                webView.reload();
                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        @Override
        public void onBackPressed() {
            if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
        }
    }

    // =====================================================================
    // ZOOM IMAGE ACTIVITY - full-screen pinch-zoom gallery viewer
    // =====================================================================
    public static class ZoomImageActivity extends AppCompatActivity {
        private List<String> images = new ArrayList<>();
        private int currentIndex = 0;
        private AppData.ZoomableImageView imageView;
        private TextView counter;

        @SuppressWarnings("unchecked")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_zoom_image);

            Object imgs = getIntent().getSerializableExtra("images");
            if (imgs instanceof List) images = (List<String>) imgs;
            currentIndex = getIntent().getIntExtra("index", 0);

            android.widget.FrameLayout container = findViewById(R.id.zoomImageContainer);
            imageView = new AppData.ZoomableImageView(this);
            container.addView(imageView, new android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT, android.widget.FrameLayout.LayoutParams.MATCH_PARENT));

            counter = findViewById(R.id.txtZoomCounter);
            ImageButton btnClose = findViewById(R.id.btnCloseZoom);
            ImageButton btnPrev = findViewById(R.id.btnZoomPrev);
            ImageButton btnNext = findViewById(R.id.btnZoomNext);

            btnClose.setOnClickListener(v -> finish());
            btnPrev.setOnClickListener(v -> { if (currentIndex > 0) { currentIndex--; loadImage(); } });
            btnNext.setOnClickListener(v -> { if (currentIndex < images.size() - 1) { currentIndex++; loadImage(); } });

            if (images.size() <= 1) { btnPrev.setVisibility(View.GONE); btnNext.setVisibility(View.GONE); }
            loadImage();
        }

        private void loadImage() {
            imageView.resetZoom();
            if (!images.isEmpty()) {
                com.bumptech.glide.Glide.with(this).load(images.get(currentIndex)).into(imageView);
                counter.setText((currentIndex + 1) + " / " + images.size());
                counter.setVisibility(images.size() > 1 ? View.VISIBLE : View.GONE);
            }
        }
    }

    // =====================================================================
    // STATIC PAGE ACTIVITY - Privacy / Terms / Disclaimer / Contact / About
    // =====================================================================
    public static class StaticPageActivity extends AppCompatActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_webview);

            Toolbar toolbar = findViewById(R.id.toolbarWeb);
            toolbar.setNavigationOnClickListener(v -> finish());
            findViewById(R.id.webProgress).setVisibility(View.GONE);

            String title = getIntent().getStringExtra("title");
            String asset = getIntent().getStringExtra("asset");
            if (title != null) toolbar.setTitle(title);

            WebView webView = findViewById(R.id.webView);
            webView.getSettings().setJavaScriptEnabled(false);
            if (asset != null) webView.loadUrl("file:///android_asset/" + asset);
        }
    }
}
