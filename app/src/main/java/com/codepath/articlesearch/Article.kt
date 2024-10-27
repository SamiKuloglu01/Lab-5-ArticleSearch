package com.codepath.articlesearch

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchNewsResponse(
    @SerialName("response") val response: BaseResponse?
)

@Serializable
data class BaseResponse(
    @SerialName("docs") val docs: List<Article>?
)

@Serializable
data class Article(
    @SerialName("abstract") val abstract: String?,
    @SerialName("headline") val headline: Headline?,
    @SerialName("byline") val byline: Byline?,
    @SerialName("multimedia") val multimedia: List<MultiMedia>?
) {
    val mediaImageUrl: String
        get() {
            val url = multimedia?.firstOrNull { !it.url.isNullOrEmpty() }?.url
            // Only prepend base URL if the URL is valid and not empty
            return if (!url.isNullOrEmpty()) {
                if (url.startsWith("http")) url else "https://www.nytimes.com/$url"
            } else {
                "" // Return an empty string if there’s no valid URL
            }
        }
}

@Serializable
data class Headline(
    @SerialName("main") val main: String?
)

@Serializable
data class Byline(
    @SerialName("original") val original: String?
)

@Serializable
data class MultiMedia(
    @SerialName("url") val url: String?
)
