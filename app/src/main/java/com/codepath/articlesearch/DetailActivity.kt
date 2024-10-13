package com.codepath.articlesearch

import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class DetailActivity : AppCompatActivity() {

    private lateinit var mediaImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var bylineTextView: TextView
    private lateinit var abstractTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        initializeViews()
        populateArticleDetails()
        applyFadeInAnimation()
    }

    private fun initializeViews() {
        mediaImageView = findViewById(R.id.mediaImage)
        titleTextView = findViewById(R.id.mediaTitle)
        bylineTextView = findViewById(R.id.mediaByline)
        abstractTextView = findViewById(R.id.mediaAbstract)
    }

    private fun populateArticleDetails() {
        val title = intent.getStringExtra("ARTICLE_EXTRA_TITLE")
        val byline = intent.getStringExtra("ARTICLE_EXTRA_BYLINE")
        val abstract = intent.getStringExtra("ARTICLE_EXTRA_ABSTRACT")
        val mediaUrl = intent.getStringExtra("ARTICLE_EXTRA_MEDIA_URL")

        setArticleText(title, byline, abstract)
        loadMediaImage(mediaUrl)
    }

    private fun setArticleText(title: String?, byline: String?, abstract: String?) {
        titleTextView.text = title
        bylineTextView.text = byline
        abstractTextView.text = abstract
    }

    private fun loadMediaImage(mediaUrl: String?) {
        if (!mediaUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(mediaUrl)
                .into(mediaImageView)
        }
    }

    private fun applyFadeInAnimation() {
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        mediaImageView.startAnimation(fadeInAnimation)
        titleTextView.startAnimation(fadeInAnimation)
        bylineTextView.startAnimation(fadeInAnimation)
        abstractTextView.startAnimation(fadeInAnimation)
    }
}
