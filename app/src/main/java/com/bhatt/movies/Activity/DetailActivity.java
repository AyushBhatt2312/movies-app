package com.bhatt.movies.Activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bhatt.movies.Adapter.ImageListAdapter;
import com.bhatt.movies.Domain.FilmItem;
import com.bhatt.movies.R;
import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.List;

public class DetailActivity extends AppCompatActivity {
    private static final String TAG = "DetailActivity";
    private static final String BASE_URL = "https://moviesapi.ir/api/v1/movies/";
    private static final int TIMEOUT_MS = 10000;
    private static final int MAX_RETRIES = 3;
    private static final float BACKOFF_MULT = 1.0f;

    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest;
    private ProgressBar progressBar;
    private TextView titleTxt, movieRateTxt, movieTimeTxt, movieDateTxt, movieSummaryInfo, movieActorsInfo;
    private NestedScrollView scrollView;
    private int idFilm;
    private ShapeableImageView posterNormalImg;
    private ImageView posterBigImg, backImg;
    private RecyclerView.Adapter adapterImgList;
    private RecyclerView imageRecyclerView;
    private int currentRetryCount = 0;
    private boolean isDestroyed = false;
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        idFilm = getIntent().getIntExtra("id", 0);
        if (idFilm == 0) {
            Toast.makeText(this, "Invalid movie ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initView();
        sendRequest();
    }

    private void initView() {
        progressBar = findViewById(R.id.detailloading);
        scrollView = findViewById(R.id.scrollView3);
        titleTxt = findViewById(R.id.movieNameTxt);
        movieRateTxt = findViewById(R.id.movieRateTxt);
        movieTimeTxt = findViewById(R.id.movieTimeTxt);
        movieDateTxt = findViewById(R.id.movieDateTxt);
        movieSummaryInfo = findViewById(R.id.movieSummaryInfo);
        movieActorsInfo = findViewById(R.id.movieActorInfo);
        posterNormalImg = findViewById(R.id.posterNormalImg);
        posterBigImg = findViewById(R.id.posterBigImg);
        backImg = findViewById(R.id.backImg);
        imageRecyclerView = findViewById(R.id.imageRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);

        backImg.setOnClickListener(v -> finish());

        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentRetryCount = 0;
            sendRequest();
        });

        imageRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        adapterImgList = new ImageListAdapter(this, new ArrayList<>());
        imageRecyclerView.setAdapter(adapterImgList);
    }

    private void sendRequest() {
        if (isDestroyed) return;

        mRequestQueue = Volley.newRequestQueue(this);
        progressBar.setVisibility(View.VISIBLE);
        scrollView.setVisibility(View.GONE);

        String url = BASE_URL + idFilm;
        Log.d(TAG, "Sending request to: " + url);

        mStringRequest = new StringRequest(Request.Method.GET, url,
                response -> {
                    if (isDestroyed) return;

                    Log.d(TAG, "Received response length: " + (response != null ? response.length() : 0));
                    
                    if (response == null || response.isEmpty()) {
                        showErrorState("Received empty response from server");
                        return;
                    }

                    progressBar.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    swipeRefreshLayout.setRefreshing(false);

                    try {
                        Gson gson = new Gson();
                        FilmItem item = gson.fromJson(response, FilmItem.class);
                        
                        if (item != null) {
                            Log.d(TAG, "Successfully parsed FilmItem: " + item.getTitle());
                            if (item.getImages() == null) {
                                Log.w(TAG, "Images list is null");
                                item.setImages(new ArrayList<>());
                            }
                            updateUI(item);
                        } else {
                            Log.e(TAG, "Failed to parse response into FilmItem");
                            showEmptyState();
                        }
                    } catch (JsonSyntaxException e) {
                        Log.e(TAG, "JSON parsing error: " + e.getMessage() + "\nResponse: " + response);
                        showErrorState("Error parsing movie data. Please try again later.");
                    } catch (Exception e) {
                        Log.e(TAG, "Unexpected error while parsing: " + e.getMessage(), e);
                        showErrorState("An unexpected error occurred. Please try again later.");
                    }
                },
                error -> {
                    if (isDestroyed) return;

                    progressBar.setVisibility(View.GONE);
                    swipeRefreshLayout.setRefreshing(false);
                    
                    String errorMessage;
                    if (error.networkResponse != null) {
                        int statusCode = error.networkResponse.statusCode;
                        String responseData = "";
                        try {
                            responseData = new String(error.networkResponse.data, "UTF-8");
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading error response: " + e.getMessage());
                        }
                        
                        Log.e(TAG, String.format("Network Error - Status: %d, Response: %s", statusCode, responseData));
                        
                        switch (statusCode) {
                            case 404:
                                errorMessage = "Movie not found. Please check the ID.";
                                break;
                            case 500:
                                errorMessage = "Server error. Please try again later.";
                                break;
                            case 429:
                                errorMessage = "Too many requests. Please wait a moment.";
                                break;
                            default:
                                errorMessage = "Error: " + statusCode + ". Please try again.";
                        }
                    } else if (!isNetworkAvailable()) {
                        errorMessage = "No internet connection. Please check your network.";
                    } else {
                        Log.e(TAG, "Volley error: " + error.toString(), error);
                        errorMessage = "Network error. Please try again.";
                    }
                    
                    showErrorState(errorMessage);

                    if (currentRetryCount < MAX_RETRIES) {
                        currentRetryCount++;
                        long backoffTime = (long) (Math.pow(2, currentRetryCount) * BACKOFF_MULT * 1000);
                        Log.d(TAG, "Retrying in " + backoffTime + "ms (attempt " + currentRetryCount + "/" + MAX_RETRIES + ")");
                        
                        new android.os.Handler().postDelayed(() -> {
                            if (!isDestroyed) {
                                sendRequest();
                            }
                        }, backoffTime);
                    }
                });

        // Set retry policy and tag
        mStringRequest.setRetryPolicy(new DefaultRetryPolicy(
                TIMEOUT_MS,
                MAX_RETRIES,
                BACKOFF_MULT
        ));
        mStringRequest.setTag(TAG);

        mRequestQueue.add(mStringRequest);
    }

    private void updateUI(FilmItem item) {
        if (isDestroyed) return;

        try {
            Log.d(TAG, "Updating UI with film item: " + (item != null ? item.getTitle() : "null"));
            
            if (item == null) {
                showErrorState("Received empty response from server");
                return;
            }

            // Handle poster images
            String posterUrl = item.getPoster();
            Log.d(TAG, "Poster URL: " + posterUrl);
            
            if (posterUrl != null && !posterUrl.isEmpty() && posterUrl.startsWith("http")) {
                try {
                    Glide.with(this)
                            .load(posterUrl)
                            .placeholder(R.drawable.placeholder_poster)
                            .error(R.drawable.error_poster)
                            .into(posterNormalImg);

                    Glide.with(this)
                            .load(posterUrl)
                            .placeholder(R.drawable.placeholder_poster)
                            .error(R.drawable.error_poster)
                            .into(posterBigImg);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading poster image: " + e.getMessage());
                    posterNormalImg.setImageResource(R.drawable.error_poster);
                    posterBigImg.setImageResource(R.drawable.error_poster);
                }
            } else {
                Log.w(TAG, "Invalid or empty poster URL");
                posterNormalImg.setImageResource(R.drawable.error_poster);
                posterBigImg.setImageResource(R.drawable.error_poster);
            }

            // Set text fields with null checks
            titleTxt.setText(item.getTitle() != null ? item.getTitle() : "N/A");
            movieRateTxt.setText(item.getRated() != null ? item.getRated() : "N/A");
            movieTimeTxt.setText(item.getRuntime() != null ? item.getRuntime() : "N/A");
            movieDateTxt.setText(item.getReleased() != null ? item.getReleased() : "N/A");
            movieSummaryInfo.setText(item.getPlot() != null ? item.getPlot() : "No summary available");
            movieActorsInfo.setText(item.getActors() != null ? item.getActors() : "No actors information available");

            // Handle image list
            List<String> images = item.getImages();
            Log.d(TAG, "Number of additional images: " + (images != null ? images.size() : 0));
            
            if (images != null && !images.isEmpty()) {
                // Validate image URLs
                List<String> validImageUrls = new ArrayList<>();
                for (String imageUrl : images) {
                    if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                        validImageUrls.add(imageUrl);
                    } else {
                        Log.w(TAG, "Skipping invalid image URL: " + imageUrl);
                    }
                }

                if (!validImageUrls.isEmpty()) {
                    try {
                        imageRecyclerView.setVisibility(View.VISIBLE);
                        adapterImgList = new ImageListAdapter(this, validImageUrls);
                        imageRecyclerView.setAdapter(adapterImgList);
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting up image list: " + e.getMessage());
                        imageRecyclerView.setVisibility(View.GONE);
                    }
                } else {
                    Log.w(TAG, "No valid image URLs found");
                    imageRecyclerView.setVisibility(View.GONE);
                }
            } else {
                Log.d(TAG, "No additional images to display");
                imageRecyclerView.setVisibility(View.GONE);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error updating UI: " + e.getMessage(), e);
            showErrorState("Error displaying movie details: " + e.getMessage());
        }
    }

    private void showEmptyState() {
        if (isDestroyed) return;
        Toast.makeText(this, "No movie details found", Toast.LENGTH_SHORT).show();
        scrollView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void showErrorState(String message) {
        if (isDestroyed) return;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        scrollView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    protected void onDestroy() {
        isDestroyed = true;
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(TAG);
        }
        super.onDestroy();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }
}
