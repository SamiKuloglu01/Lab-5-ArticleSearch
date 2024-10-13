package com.codepath.articlesearch

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

const val ARTICLE_EXTRA = "ARTICLE_EXTRA"
private const val TAG = "ArticleAdapter"

class ArticleAdapter(private val context: Context, private var articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    // Creates a new view for each item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false)
        return ViewHolder(view)
    }

    // Binds data to each view
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        holder.bind(article)

        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)

        // Apply the animation to the entire item view
        holder.itemView.startAnimation(animation)
    }

    // Returns the number of items in the list
    override fun getItemCount() = articles.size

    // Updates the adapter with a new list of articles
    fun updateArticles(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()
    }

    // ViewHolder class to hold reference to views
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val mediaImageView = itemView.findViewById<ImageView>(R.id.mediaImage)
        private val titleTextView = itemView.findViewById<TextView>(R.id.mediaTitle)
        private val abstractTextView = itemView.findViewById<TextView>(R.id.mediaAbstract)

        init {
            itemView.setOnClickListener(this) // Set click listener for the view
        }

        // Helper method to bind the article data to the view
        fun bind(article: Article) {
            titleTextView.text = article.headline?.main
            abstractTextView.text = article.abstract

            // Load image using Glide
            Glide.with(context)
                .load(article.mediaImageUrl)
                .placeholder(R.drawable.ic_launcher_background) // Set a placeholder image while loading
                .error(R.drawable.ic_launcher_background) // Set an error image if the load fails
                .into(mediaImageView)
        }

        override fun onClick(v: View?) {
            val article = articles[adapterPosition]
            // Create an intent to navigate to the DetailActivity

            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("ARTICLE_EXTRA_BYLINE", article.byline?.original)
                putExtra("ARTICLE_EXTRA_TITLE", article.headline?.main)
                putExtra("ARTICLE_EXTRA_ABSTRACT", article.abstract)
                putExtra("ARTICLE_EXTRA_MEDIA_URL", article.mediaImageUrl)
            }

            context.startActivity(intent) // Start the DetailActivity
        }

    }
}
