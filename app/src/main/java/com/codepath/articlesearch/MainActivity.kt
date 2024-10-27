package com.codepath.articlesearch

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.SearchView
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.codepath.articlesearch.databinding.ActivityMainBinding
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import org.json.JSONException

private const val TAG = "MainActivity"
private const val ARTICLE_SEARCH_URL = "https://api.nytimes.com/svc/search/v2/articlesearch.json?api-key=${BuildConfig.API_KEY}"
private const val PREFS_NAME = "ArticleSearchPrefs"
private const val CACHE_ENABLED_KEY = "cacheEnabled"

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var cacheSwitch: Switch
    private lateinit var sharedPreferences: SharedPreferences
    private var cacheEnabled = true
    private val articles = mutableListOf<Article>()

    // ConnectivityManager for monitoring network changes
    private lateinit var connectivityManager: ConnectivityManager
    private var wasInitiallyOffline = false // Flag to track initial offline state
    private var dataLoadedOffline = false // Flag to track if data currently displayed was loaded offline

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences and Switch for caching
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        cacheEnabled = sharedPreferences.getBoolean(CACHE_ENABLED_KEY, true)

        // Initialize RecyclerView and SwipeRefreshLayout
        articleAdapter = ArticleAdapter(this, articles)
        swipeRefreshLayout = binding.swipeRefreshLayout
        cacheSwitch = binding.switchCachePreference
        binding.articles.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = articleAdapter
            addItemDecoration(DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL))
        }

        // Set initial switch state and listener
        cacheSwitch.isChecked = cacheEnabled
        cacheSwitch.setOnCheckedChangeListener { _, isChecked ->
            cacheEnabled = isChecked
            sharedPreferences.edit().putBoolean(CACHE_ENABLED_KEY, cacheEnabled).apply()
        }

        // Swipe-to-Refresh listener with offline check
        swipeRefreshLayout.setOnRefreshListener {
            if (isNetworkAvailable()) {
                fetchArticlesFromApi()
            } else {
                Toast.makeText(this, "No internet connection. Please try again later.", Toast.LENGTH_SHORT).show()
                swipeRefreshLayout.isRefreshing = false // Stop the refreshing animation
            }
        }

        // SearchView setup
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { searchArticles(it) }
                return true
            }
        })

        // Initialize ConnectivityManager and register network callback
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        registerNetworkCallback()

        // Check initial connectivity status and fetch data accordingly
        wasInitiallyOffline = !isNetworkAvailable()
        fetchArticlesFromDatabaseOrApi()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isNetworkAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Load articles from Room or API if Room is empty
    private fun fetchArticlesFromDatabaseOrApi() {
        lifecycleScope.launch {
            (application as ArticleApplication).db.articleDao().getAll().collect { databaseArticles ->
                if (databaseArticles.isEmpty() || !cacheEnabled) {
                    fetchArticlesFromApi()
                } else {
                    // Show toast if starting in offline mode
                    if (wasInitiallyOffline) {
                        Toast.makeText(this@MainActivity, "No internet connectivity, showing previously fetched data", Toast.LENGTH_LONG).show()
                        dataLoadedOffline = true // Mark that data was loaded offline
                    } else {
                        dataLoadedOffline = false // Mark that data was not loaded offline
                    }
                    // Load articles from database
                    articles.clear()
                    articles.addAll(databaseArticles.map { it.toArticle() })
                    articleAdapter.notifyDataSetChanged()
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // Fetch articles from NYT API and store in Room
    private fun fetchArticlesFromApi() {
        binding.progressBar.visibility = View.VISIBLE
        val client = AsyncHttpClient()
        client.get(ARTICLE_SEARCH_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(statusCode: Int, headers: Headers?, response: String?, throwable: Throwable?) {
                Log.e(TAG, "Failed to fetch articles: $statusCode")
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                }
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                try {
                    val parsedJson = Json { ignoreUnknownKeys = true }.decodeFromString<SearchNewsResponse>(json.jsonObject.toString())
                    val apiArticles = parsedJson.response?.docs ?: emptyList()
                    val filteredArticles = apiArticles.filter { article -> article.multimedia?.isNotEmpty() == true }
                    if (cacheEnabled) saveArticlesToDatabase(filteredArticles)

                    runOnUiThread {
                        articles.clear()
                        articles.addAll(filteredArticles)
                        articleAdapter.notifyDataSetChanged()
                        binding.progressBar.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false
                        dataLoadedOffline = false // Reset flag as fresh data is loaded from API
                    }
                } catch (e: JSONException) {
                    Log.e(TAG, "Exception: $e")
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false
                    }
                }
            }
        })
    }

    // Save articles fetched from API to Room database
    private fun saveArticlesToDatabase(filteredArticles: List<Article>) {
        lifecycleScope.launch(Dispatchers.IO) {
            val articleEntities = filteredArticles.map { article ->
                ArticleEntity(
                    headline = article.headline?.main,
                    articleAbstract = article.abstract,
                    byline = article.byline?.original,
                    mediaImageUrl = article.mediaImageUrl
                )
            }
            val articleDao = (application as ArticleApplication).db.articleDao()
            articleDao.deleteAll()
            articleDao.insertAll(articleEntities)
        }
    }

    // Filter articles based on search query
    private fun searchArticles(query: String) {
        val filteredArticles = articles.filter {
            it.headline?.main?.contains(query, ignoreCase = true) == true
        }
        articleAdapter.updateArticles(filteredArticles)
    }

    // Register a network callback to monitor connectivity changes
    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (wasInitiallyOffline && dataLoadedOffline) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Internet connectivity restored", Toast.LENGTH_SHORT).show()
                        fetchArticlesFromApi() // Refresh articles only if offline data is currently displayed
                    }
                    wasInitiallyOffline = false // Reset the flag after connectivity is restored
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Internet connection lost", Toast.LENGTH_SHORT).show()
                }
                wasInitiallyOffline = true // Set flag to true if connectivity is lost
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        connectivityManager.unregisterNetworkCallback(ConnectivityManager.NetworkCallback())
    }
}
