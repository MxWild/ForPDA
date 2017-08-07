package forpdateam.ru.forpda.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import forpdateam.ru.forpda.App
import forpdateam.ru.forpda.async.async
import forpdateam.ru.forpda.client.Client
import forpdateam.ru.forpda.ext.logger
import forpdateam.ru.forpda.fragments.news.NewsHelper
import forpdateam.ru.forpda.utils.allItems
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.realm.RealmConfiguration

/**
 * Created by isanechek on 7/2/17.
 */
class NewsRepository {

    private val newsResponse: MutableLiveData<Response<List<News>>> by lazy { MutableLiveData<Response<List<News>>>() }
    private val newsDetailsResponse: MutableLiveData<Response<News>> by lazy { MutableLiveData<Response<News>>() }
    private val commentsResponse: MutableLiveData<Response<List<Comment>>> by lazy { MutableLiveData<Response<List<Comment>>>() }
    private  val config: RealmConfiguration by lazy { App.getInstance().newsRealmConfiguration }

    companion object {
        private val TAG: String = "NewsRepository"
        private var INSTANCE: NewsRepository? = null
        @JvmStatic
        fun createInstance() { INSTANCE = NewsRepository() }
        fun getInstance() : NewsRepository = INSTANCE ?: throw IllegalStateException("No Created NewsRepository Instance!!!")
    }

    fun loadData(request: Request): LiveData<Response<List<News>>> {
        readFromRoom(request)
        readFromNetwork(request)
        return newsResponse
    }

    fun refresh(request: Request) {
        readFromNetwork(request)
    }

    fun loadMore(request: Request) {
        readFromNetwork(request, true)
    }

    private fun loadDataTest(request: Request) = async {
        logger("$TAG START TEST")
        val cache = News().allItems(config)
        if (cache.isNotEmpty()) {
            logger("$TAG DATA IS NOT EMPTY ${cache.size}")
            newsResponse.value = Response.loading(cache, null, Response.LOAD_DATA_FROM_NETWORK)
            val result = await { NewsApi4K.getListNews(request) }
            if (result.isNotEmpty()) {
                logger("$TAG DATA IS NOT EMPTY ${result.size}")
                logger("$TAG DATA IS NOT EMPTY ${result.size}")
                newsResponse.value = Response.loading(cache, null, Response.WORKING_WITH_DATA)
                val response = NewsHelper.mappingNewsItemsToNews(result, request.category)



                newsResponse.value = Response.success(response, null)
            } else {
                logger("$TAG DATA IS EMPTY")
                newsResponse.value = Response.error(Response.DATA_IS_EMPTY, null)
            }
        }
    }.onError {
        logger("Read From Network Error ${it.message}")
        newsResponse.value = Response.error(Response.DATA_IS_EMPTY_NETWORK, null)
    }

    private fun compareData(oldData: List<News>, newData: List<News>) : List<News> = newData.filterNot { n -> oldData.any { it.url == n.url } }.toList()

    private fun com(item: News, items: List<News>) : Boolean {
        return items.any { it.url == item.url }
    }

    private fun readFromNetwork(request: Request, loadMore: Boolean = false) = async {
        // send action show progress
        // OldData - this is data from db
        val oldData = newsResponse.value?.data
        newsResponse.value = Response.loading(oldData, null, Response.LOAD_DATA_FROM_NETWORK)
        // async request data from network
        val result = await { NewsApi4K.getListNews(request) }
        if (result.isNotEmpty()) {
            /*
            * Шлем второй раз oldData если есть.
            * Ничего страшного. Где надо об этом знают и UI дергать не будет.))
            */
            newsResponse.value = Response.loading(oldData, null, Response.WORKING_WITH_DATA)
            // mapping data
            val response = NewsHelper.mappingNewsItemsToNews(result, request.category)
            // send data to UI
            newsResponse.value = Response.success(response, null)
        } else {
            newsResponse.value = Response.error(Response.DATA_IS_EMPTY, null)
        }

    }.onError {
        logger("Read From Network Error ${it.message}")
        newsResponse.value = Response.error(Response.DATA_IS_EMPTY_NETWORK, null)
    }

    private fun readFromRoom(request: Request) {

    }

    private fun checkNewData(oldData: List<News>?, newData: List<News>?) {

    }

    private fun load(oldData: List<News>?, newData: List<News>?) {}



     //Details
    fun loadDetailsData2(request: Request) : LiveData<Response<News>> {
        requestFromNetwork(request)
        return newsDetailsResponse
    }

    private fun requestFromNetwork(request: Request) = async {
        if (networkStatus()) {
            newsDetailsResponse.value = Response.loading(null, null, Response.LOAD_DATA_FROM_NETWORK)
            when {
                request.url == null -> newsDetailsResponse.value = Response.error(Response.BAD_REQUEST, null)
                else -> {
                    val result = await {NewsApi4K.getSource(request.url)}
                    if (result != NewsApi4K.EMPTY_OR_NULL_RESPONSE_FROM_NETWORK) {
                        newsDetailsResponse.value = Response.loading(null, null, Response.WORKING_WITH_DATA)
                        val response = await { NewsApi4K.getNews(result) }
                        newsDetailsResponse.value = Response.loading(response, null, null)
                    }
                }
            }
        } else { newsDetailsResponse.value = Response.error(Response.NO_NETWORK, null) }
    }

    private fun networkStatus() : Boolean = Client.getInstance().networkState


    // comments
    fun loadComments(request: Request) : LiveData<Response<List<Comment>>> {
        loadCommentsFromNetwork(request)
        return commentsResponse
    }

    private fun loadCommentsFromNetwork(request: Request) {
        if (networkStatus()) {

        } else { newsDetailsResponse.value = Response.error(Response.NO_NETWORK, null) }
    }

}