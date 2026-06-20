package com.example.webviewbrowser.data.session

import com.example.webviewbrowser.data.db.dao.TabDao
import com.example.webviewbrowser.data.db.entity.TabContentType
import com.example.webviewbrowser.data.db.entity.TabEntity
import javax.inject.Inject
import javax.inject.Singleton

/** セッション復元用のタブ表現（永続化詳細を隠蔽する）。 */
data class SessionTab(
    val id: String,
    val url: String,
    val title: String,
    val isActive: Boolean,
    val isHome: Boolean,
)

/**
 * タブ構成の保存/復元。Task5(ブラウザコア) が利用する port。
 */
@Singleton
class SessionRepository @Inject constructor(
    private val tabDao: TabDao,
) {
    /** 保存済みのタブ構成を position 昇順で取得する。 */
    suspend fun restore(): List<SessionTab> =
        tabDao.getAll().map { it.toSessionTab() }

    /** タブ構成をまるごと置き換えて保存する。 */
    suspend fun save(tabs: List<SessionTab>) {
        val now = System.currentTimeMillis()
        val entities = tabs.mapIndexed { index, tab ->
            TabEntity(
                id = tab.id,
                url = tab.url,
                title = tab.title,
                position = index,
                isActive = tab.isActive,
                contentType = if (tab.isHome) TabContentType.HOME else TabContentType.WEB,
                updatedAt = now,
            )
        }
        tabDao.replaceAll(entities)
    }

    private fun TabEntity.toSessionTab() = SessionTab(
        id = id,
        url = url,
        title = title,
        isActive = isActive,
        isHome = contentType == TabContentType.HOME,
    )
}
