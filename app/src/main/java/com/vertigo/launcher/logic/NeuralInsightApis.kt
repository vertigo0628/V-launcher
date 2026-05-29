package com.vertigo.launcher.logic

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

// --- Nager.Date (Global Holidays) ---
interface HolidayApi {
    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String
    ): List<PublicHoliday>
}

data class PublicHoliday(
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val types: List<String>
)

// --- Wikipedia (On This Day / Daily Info) ---
interface WikipediaApi {
    @GET("api/rest_v1/feed/onthisday/all/{month}/{day}")
    suspend fun getOnThisDay(
        @Path("month") month: String,
        @Path("day") day: String
    ): WikipediaResponse
}

data class WikipediaResponse(
    val selected: List<WikiEvent>? = null,
    val events: List<WikiEvent>? = null,
    val births: List<WikiEvent>? = null,
    val deaths: List<WikiEvent>? = null,
    val holidays: List<WikiEvent>? = null
)

data class WikiEvent(
    val text: String,
    val year: Int? = null,
    val pages: List<WikiPage>? = null
)

data class WikiPage(
    val titles: WikiTitles,
    val extract: String? = null,
    val thumbnail: WikiThumbnail? = null
)

data class WikiTitles(
    val normalized: String
)

data class WikiThumbnail(
    val source: String
)

data class WikiInsightItem(
    val text: String,
    val imageUrl: String? = null,
    val pageTitle: String? = null,
    val extract: String? = null
)
