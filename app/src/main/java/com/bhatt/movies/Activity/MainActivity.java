package com.bhatt.movies.Activity;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.bhatt.movies.Adapter.FilmListAdapter;
import com.bhatt.movies.Domain.FilmItem;
import com.bhatt.movies.Domain.ListFilm;
import com.bhatt.movies.R;
import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {
    private RecyclerView.Adapter adapterNewMovies, adapterUpComming;
    private RecyclerView recyclerViewNewMovies, recyclerViewUpComming;
    private RequestQueue mRequestQueue;
    private StringRequest mStringRequest, mStringRequest2;
    private ProgressBar loading1, loading2;
    private SwipeRefreshLayout swipeRefreshLayout;
    private static final int MAX_RETRIES = 3;
    private int currentRetryCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        setupSwipeRefresh();
        sendRequest1();
        sendRequest2();
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            currentRetryCount = 0;
            sendRequest1();
            sendRequest2();
        });
    }

    private void sendRequest1() {
        mRequestQueue = Volley.newRequestQueue(this);
        loading1.setVisibility(View.VISIBLE);
        mStringRequest = new StringRequest(Request.Method.GET, "https://moviesapi.ir/api/v1/movies?page=1", 
            response -> {
                Gson gson = new Gson();
                loading1.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                ListFilm items = gson.fromJson(response, ListFilm.class);
                if (items != null && items.getData() != null && !items.getData().isEmpty()) {
                    adapterNewMovies = new FilmListAdapter(items);
                    recyclerViewNewMovies.setAdapter(adapterNewMovies);
                } else {
                    showEmptyState();
                }
            }, 
            error -> {
                loading1.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "sendRequest1: " + error.toString());
                
                if (currentRetryCount < MAX_RETRIES) {
                    currentRetryCount++;
                    sendRequest1();
                } else {
                    showErrorState();
                }
            }
        );
        mRequestQueue.add(mStringRequest);
    }

    private void sendRequest2() {
        mRequestQueue = Volley.newRequestQueue(this);
        loading2.setVisibility(View.VISIBLE);
        mStringRequest2 = new StringRequest(Request.Method.GET, "https://moviesapi.ir/api/v1/movies?page=3", 
            response -> {
                Gson gson = new Gson();
                loading2.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                ListFilm items = gson.fromJson(response, ListFilm.class);
                if (items != null && items.getData() != null && !items.getData().isEmpty()) {
                    adapterUpComming = new FilmListAdapter(items);
                    recyclerViewUpComming.setAdapter(adapterUpComming);
                } else {
                    showEmptyState();
                }
            }, 
            error -> {
                loading2.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);
                Log.e(TAG, "sendRequest2: " + error.toString());
                
                if (currentRetryCount < MAX_RETRIES) {
                    currentRetryCount++;
                    sendRequest2();
                } else {
                    showErrorState();
                }
            }
        );
        mRequestQueue.add(mStringRequest2);
    }

    private void showEmptyState() {
        Toast.makeText(this, "No movies found", Toast.LENGTH_SHORT).show();
    }

    private void showErrorState() {
        Toast.makeText(this, "Failed to load movies. Please try again later.", Toast.LENGTH_SHORT).show();
    }

    private void initView() {
        recyclerViewNewMovies = findViewById(R.id.view1);
        recyclerViewNewMovies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        recyclerViewUpComming = findViewById(R.id.view2);
        recyclerViewUpComming.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        
        loading1 = findViewById(R.id.loading1);
        loading2 = findViewById(R.id.loading2);
        
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
    }
}