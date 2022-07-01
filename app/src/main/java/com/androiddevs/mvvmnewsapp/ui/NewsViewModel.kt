package com.androiddevs.mvvmnewsapp.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.ConnectivityManager.*
import android.net.NetworkCapabilities.*
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Query
import com.androiddevs.mvvmnewsapp.NewsApplication
import com.androiddevs.mvvmnewsapp.models.Article
import com.androiddevs.mvvmnewsapp.models.NewsResponse
import com.androiddevs.mvvmnewsapp.repository.NewsRepository
import com.androiddevs.mvvmnewsapp.util.Resource
import kotlinx.coroutines.launch
import retrofit2.Response
import java.io.IOException

class NewsViewModel(
    app: Application,
    val newsReporsitory: NewsRepository
)    : AndroidViewModel(app) {

    val breakingNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    val breakingNewsPage = 1
    var breakingNewsResponse: NewsResponse? = null


    val searchNews: MutableLiveData<Resource<NewsResponse>> = MutableLiveData()
    val searchNewsPage = 1
    var searchNewsResponse: NewsResponse? = null

    init {
        getBreakingNews("us")
    }

    fun getBreakingNews(countryCode: String) = viewModelScope.launch {
        safeBreakingNewsCall(countryCode)
    }

    fun searchNews(searchQuery: String) = viewModelScope.launch {
        safeSearchNewsCall(searchQuery)
    }

    private fun handleBreakingNewsResponse(response: Response<NewsResponse>) : Resource<NewsResponse> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                breakingNewsPage
                if(breakingNewsResponse == null) {
                    breakingNewsResponse = resultResponse
                } else {
                    val oldArticles = breakingNewsResponse?. articles
                    val newArticles = resultResponse. articles
                    oldArticles?.addAll(newArticles)
                }

                return Resource.Success( breakingNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    private fun handleSearchNewsResponse(response: Response<NewsResponse>) : Resource<NewsResponse> {
        if(response.isSuccessful) {
            response.body()?.let { resultResponse ->
                searchNewsPage
                if(searchNewsResponse == null) {
                    searchNewsResponse = resultResponse
                } else {
                    val oldArticles = breakingNewsResponse?. articles
                    val newArticles = resultResponse. articles
                    oldArticles?.addAll(newArticles)
                }

                return Resource.Success( breakingNewsResponse ?: resultResponse)
            }
        }
        return Resource.Error(response.message())
    }

    fun saveArticle(article: Article) = viewModelScope.launch {
        newsReporsitory.upsert(article)
    }

    fun getSavedNews() = newsReporsitory.getSavedNews()

    fun deleteArticle(article: Article) = viewModelScope.launch {
        newsReporsitory.deletArticle(article)
    }

    private  suspend fun  safeSearchNewsCall(searchQuery: String) {
        searchNews.postValue(Resource.Loading())
        try{
            if(hasInternetConnection()){
                val response = newsReporsitory.searchNews(searchQuery, searchNewsPage)
                searchNews.postValue(handleSearchNewsResponse(response))
            } else {
                searchNews.postValue((Resource.Error("No Internet Connection")))
            }
        }catch (t: Throwable) {
            when(t) {
                is IOException -> searchNews.postValue(Resource.Error("Network Failure"))
                else -> searchNews.postValue(Resource.Error("Conversion Error"))
            }

        }
    }

    private  suspend fun  safeBreakingNewsCall(countryCode: String) {
        breakingNews.postValue(Resource.Loading())
        try{
            if(hasInternetConnection()){
                val response = newsReporsitory.getBreakingNews(countryCode, breakingNewsPage)
                breakingNews.postValue(handleBreakingNewsResponse(response))
            } else {
                breakingNews.postValue((Resource.Error("No Internet Connection")))
            }
        }catch (t: Throwable) {
            when(t) {
                is IOException -> breakingNews.postValue(Resource.Error("Network Failure"))
                else -> breakingNews.postValue(Resource.Error("Conversion Error"))
            }

        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivtityManager = getApplication<NewsApplication>().getSystemService(
            Context.CONNECTIVITY_SERVICE
        ) as ConnectivityManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val activeNetwork = connectivtityManager.activeNetwork ?: return false
            val capabilities = connectivtityManager.getNetworkCapabilities(activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(TRANSPORT_WIFI) -> true
                capabilities.hasTransport(TRANSPORT_CELLULAR) -> true
                capabilities.hasTransport(TRANSPORT_ETHERNET) -> true
                else -> false
            }
        } else {
            connectivtityManager.activeNetworkInfo?.run {
                return when(type) {
                    TYPE_WIFI -> true
                    TYPE_MOBILE -> true
                    TYPE_ETHERNET -> true
                    else -> true
                }
            }
        }
        return false
    }
}