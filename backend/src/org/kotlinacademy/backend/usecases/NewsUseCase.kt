package org.kotlinacademy.backend.usecases

import kotlinx.coroutines.experimental.runBlocking
import org.kotlinacademy.DateTime
import org.kotlinacademy.backend.Config
import org.kotlinacademy.backend.repositories.db.ArticlesDatabaseRepository
import org.kotlinacademy.backend.repositories.db.InfoDatabaseRepository
import org.kotlinacademy.backend.repositories.db.PuzzlersDatabaseRepository
import org.kotlinacademy.backend.repositories.network.notifications.NotificationsRepository
import org.kotlinacademy.data.*
import org.kotlinacademy.now

object NewsUseCase {

    suspend fun getAcceptedNewsData(): NewsData {
        val articlesDatabaseRepository by ArticlesDatabaseRepository.lazyGet()
        val infoDatabaseRepository by InfoDatabaseRepository.lazyGet()
        val puzzlersDatabaseRepository by PuzzlersDatabaseRepository.lazyGet()

        val articles = articlesDatabaseRepository.getArticles()
        val infos = infoDatabaseRepository.getInfos()
        val puzzlers = puzzlersDatabaseRepository.getPuzzlers()
        return NewsData(
                articles = articles,
                infos = infos.filter { it.accepted },
                puzzlers = puzzlers.filter { it.accepted }
        )
    }

    suspend fun getPropositions(): NewsData {
        val infoDatabaseRepository by InfoDatabaseRepository.lazyGet()
        val puzzlersDatabaseRepository by PuzzlersDatabaseRepository.lazyGet()

        val infos = infoDatabaseRepository.getInfos()
        val puzzlers = puzzlersDatabaseRepository.getPuzzlers()
        return NewsData(
                articles = emptyList(),
                infos = infos.filter { !it.accepted },
                puzzlers = puzzlers.filter { !it.accepted }
        )
    }

    suspend fun propose(infoData: InfoData) {
        val infoDatabaseRepository = InfoDatabaseRepository.get()

        val info = infoDatabaseRepository.addInfo(infoData, false)
        EmailUseCase.askForAcceptation(info)
    }

    suspend fun propose(puzzlerData: PuzzlerData) {
        val puzzlersDatabaseRepository = PuzzlersDatabaseRepository.get()

        val puzzler = puzzlersDatabaseRepository.addPuzzler(puzzlerData, false)
        EmailUseCase.askForAcceptation(puzzler)
    }

    suspend fun update(info: Info) {
        val infoDatabaseRepository = InfoDatabaseRepository.get()

        infoDatabaseRepository.updateInfo(info)
    }

    suspend fun update(puzzler: Puzzler) {
        val puzzlersDatabaseRepository = PuzzlersDatabaseRepository.get()

        puzzlersDatabaseRepository.updatePuzzler(puzzler)
    }

    suspend fun acceptInfo(id: Int) {
        val infoDatabaseRepository = InfoDatabaseRepository.get()
        val notificationsRepository = NotificationsRepository.get()

        val info = infoDatabaseRepository.getInfo(id)
        val changedInfo = info.copy(dateTime = now, accepted = true)
        infoDatabaseRepository.updateInfo(changedInfo)
        if (notificationsRepository != null) {
            val title = "New info: " + info.title
            val url = Config.baseUrl // TODO: To particular info
            NotificationsUseCase.send(title, url)
        }
    }

    suspend fun acceptPuzzler(id: Int) {
        val notificationsRepository = NotificationsRepository.get()
        val puzzlersDatabaseRepository = PuzzlersDatabaseRepository.get()

        val puzzler = puzzlersDatabaseRepository.getPuzzler(id)
        val changedPuzzler = puzzler.copy(dateTime = now, accepted = true)
        puzzlersDatabaseRepository.updatePuzzler(changedPuzzler)
        if (notificationsRepository != null) {
            val title = "New puzzler: " + puzzler.title
            val url = Config.baseUrl // TODO: To particular puzzler
            NotificationsUseCase.send(title, url)
        }
    }

    suspend fun deleteInfo(id: Int) {
        val infoDatabaseRepository = InfoDatabaseRepository.get()

        infoDatabaseRepository.deleteInfo(id)
    }

    suspend fun deletePuzzler(id: Int) {
        val puzzlersDatabaseRepository = PuzzlersDatabaseRepository.get()

        puzzlersDatabaseRepository.deletePuzzler(id)
    }

    suspend fun publishScheduled(schedule: Map<DateTime, PuzzlerData>) {
        val puzzlersDatabaseRepository by PuzzlersDatabaseRepository.lazyGet()
        val puzzlersInDatabase: List<PuzzlerData> by lazy { runBlocking { puzzlersDatabaseRepository.getPuzzlers().map { it.data } } }

        schedule.filter { (date, _) -> now > date }
                .filter { (_, puzzler) -> puzzler !in puzzlersInDatabase }
                .forEach { (_, puzzler) -> NewsUseCase.propose(puzzler) }
    }
}