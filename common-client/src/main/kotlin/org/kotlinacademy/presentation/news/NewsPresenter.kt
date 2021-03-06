package org.kotlinacademy.presentation.news

import org.kotlinacademy.common.launchUI
import org.kotlinacademy.data.*
import org.kotlinacademy.presentation.BasePresenter
import org.kotlinacademy.respositories.NewsRepository

class NewsPresenter(val view: NewsView) : BasePresenter() {

    private val repository by NewsRepository.lazyGet()

    private var visibleNews: NewsData? = null

    override fun onCreate() {
        view.loading = true
        refreshList()
    }

    fun onRefresh() {
        view.refresh = true
        refreshList()
    }

    private fun refreshList() {
        jobs += launchUI {
            try {
                val newsData = repository.getNewsData()
                if (newsData == visibleNews) return@launchUI
                visibleNews = newsData

                val news = newsData
                        .run { articles + infos + puzzlers }
                        .sortedByDescending { it.dateTime }

                view.showList(news)
            } catch (e: Throwable) {
                view.showError(e)
            } finally {
                view.refresh = false
                view.loading = false
            }
        }
    }
}