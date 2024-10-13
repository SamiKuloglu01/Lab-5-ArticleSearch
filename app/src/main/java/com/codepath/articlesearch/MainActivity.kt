package com.codepath.articlesearch

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.codepath.articlesearch.databinding.ActivityMainBinding
import com.codepath.asynchttpclient.AsyncHttpClient
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import org.json.JSONException

private const val TAG = "MainActivity"
private val SEARCH_API_KEY = BuildConfig.API_KEY
private val ARTICLE_SEARCH_URL = "https://api.nytimes.com/svc/search/v2/articlesearch.json?api-key=${SEARCH_API_KEY}"

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var articleAdapter: ArticleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        articleAdapter = ArticleAdapter(this, emptyList())
        binding.articles.apply {
            adapter = articleAdapter
            layoutManager = LinearLayoutManager(this@MainActivity).also {
                val dividerItemDecoration = DividerItemDecoration(this@MainActivity, it.orientation)
                addItemDecoration(dividerItemDecoration)
            }
        }

        fetchArticles()
    }

    private fun fetchArticles() {
        val client = AsyncHttpClient()
        client.get(ARTICLE_SEARCH_URL, object : JsonHttpResponseHandler() {
            override fun onFailure(
                statusCode: Int,
                headers: Headers?,
                response: String?,
                throwable: Throwable?
            ) {
                Log.e(TAG, "Failed to fetch articles: $statusCode")

                // Ensure UI updates happen on the main thread
                runOnUiThread {
                    // Hide progress bar on failure
                    binding.progressBar.visibility = View.GONE
                }
            }

            override fun onSuccess(statusCode: Int, headers: Headers, json: JSON) {
                Log.i(TAG, "Successfully fetched articles: $json")
                try {
                    // Parse the JSON response using Kotlin Serialization
                    val parsedJson = Json { ignoreUnknownKeys = true }
                        .decodeFromString<SearchNewsResponse>(json.jsonObject.toString())

                    // Get the list of articles from the parsed response
                    val articles = parsedJson.response?.docs

                    // Ensure UI updates happen on the main thread
                    runOnUiThread {
                        if (articles != null) {
                            // Update the adapter with the new articles
                            articleAdapter.updateArticles(articles)

                            // Hide progress bar on success
                            binding.progressBar.visibility = View.GONE
                        }
                    }

                } catch (e: JSONException) {
                    Log.e(TAG, "Exception: $e")
                    // Ensure UI updates happen on the main thread
                    runOnUiThread {
                        // Hide progress bar if there is an exception
                        binding.progressBar.visibility = View.GONE
                    }
                }
            }
        })
    }
}
