package com.codepath.articlesearch

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class ArticleAdapter(private val context: Context, private var articles: List<Article>) :
    RecyclerView.Adapter<ArticleAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_article, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val article = articles[position]
        holder.bind(article)
        val animation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        holder.itemView.startAnimation(animation)
    }

    override fun getItemCount() = articles.size

    fun updateArticles(newArticles: List<Article>) {
        articles = newArticles
        notifyDataSetChanged()
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {
        private val mediaImageView: ImageView = itemView.findViewById(R.id.mediaImage)
        private val titleTextView: TextView = itemView.findViewById(R.id.mediaTitle)
        private val abstractTextView: TextView = itemView.findViewById(R.id.mediaAbstract)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(article: Article) {
            titleTextView.text = article.headline?.main
            abstractTextView.text = article.abstract

            // Correct URL formatting for Glide
            val imageUrl = if (article.mediaImageUrl.startsWith("http")) {
                article.mediaImageUrl
            } else {
                "https://www.nytimes.com/${article.mediaImageUrl}"
            }
            Log.d("ArticleAdapter", "Loading image URL: $imageUrl") // Log the URL for debugging

            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_background) // Placeholder shown while loading
                .error(R.drawable.ic_launcher_background)       // Error placeholder if loading fails
                .diskCacheStrategy(DiskCacheStrategy.ALL)       // Caches both full-size and transformed images
                .into(mediaImageView)

        }

        override fun onClick(v: View?) {
            val article = articles[adapterPosition]
            val intent = Intent(context, DetailActivity::class.java).apply {
                putExtra("ARTICLE_EXTRA_BYLINE", article.byline?.original)
                putExtra("ARTICLE_EXTRA_TITLE", article.headline?.main)
                putExtra("ARTICLE_EXTRA_ABSTRACT", article.abstract)
                putExtra("ARTICLE_EXTRA_MEDIA_URL", article.mediaImageUrl)
            }
            context.startActivity(intent)
        }
    }
}
