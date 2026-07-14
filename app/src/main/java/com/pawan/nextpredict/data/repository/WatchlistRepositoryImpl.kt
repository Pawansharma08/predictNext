package com.pawan.nextpredict.data.repository

import com.pawan.nextpredict.core.database.dao.WatchlistDao
import com.pawan.nextpredict.core.database.dao.WatchlistItemDao
import com.pawan.nextpredict.core.database.entity.WatchlistEntity
import com.pawan.nextpredict.core.database.entity.WatchlistItemEntity
import com.pawan.nextpredict.domain.model.Watchlist
import com.pawan.nextpredict.domain.model.WatchlistStock
import com.pawan.nextpredict.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchlistRepositoryImpl @Inject constructor(
    private val watchlistDao: WatchlistDao,
    private val watchlistItemDao: WatchlistItemDao,
) : WatchlistRepository {

    override fun getAllWatchlists(): Flow<List<Watchlist>> =
        watchlistDao.getAllWatchlists().map { entities ->
            entities.map { entity ->
                Watchlist(
                    id = entity.id,
                    name = entity.name,
                    sortOrder = entity.sortOrder,
                    items = emptyList(), // Items loaded separately
                )
            }
        }

    override fun getWatchlistItems(watchlistId: Long): Flow<List<WatchlistStock>> =
        watchlistItemDao.getItemsForWatchlist(watchlistId).map { entities ->
            entities.map { entity ->
                WatchlistStock(
                    symbol = entity.symbol,
                    companyName = entity.companyName,
                    sortOrder = entity.sortOrder,
                )
            }
        }

    override fun isSymbolInWatchlist(watchlistId: Long, symbol: String): Flow<Boolean> =
        watchlistItemDao.isSymbolInWatchlist(watchlistId, symbol)

    override suspend fun createWatchlist(name: String): Long {
        return watchlistDao.insertWatchlist(
            WatchlistEntity(name = name, sortOrder = 0)
        )
    }

    override suspend fun renameWatchlist(watchlistId: Long, newName: String) {
        val existing = watchlistDao.getWatchlistById(watchlistId) ?: return
        watchlistDao.updateWatchlist(existing.copy(name = newName))
    }

    override suspend fun deleteWatchlist(watchlistId: Long) {
        val existing = watchlistDao.getWatchlistById(watchlistId) ?: return
        watchlistDao.clearWatchlistItems(watchlistId)
        watchlistDao.deleteWatchlist(existing)
    }

    override suspend fun addStockToWatchlist(
        watchlistId: Long,
        symbol: String,
        companyName: String,
    ) {
        watchlistItemDao.insertItem(
            WatchlistItemEntity(
                watchlistId = watchlistId,
                symbol = symbol,
                companyName = companyName,
                sortOrder = 0,
            )
        )
    }

    override suspend fun removeStockFromWatchlist(watchlistId: Long, symbol: String) {
        watchlistItemDao.deleteItem(watchlistId, symbol)
    }

    override suspend fun reorderWatchlistItem(
        watchlistId: Long,
        symbol: String,
        newOrder: Int,
    ) {
        // Reorder is handled via deleteAndReinsert in production
        val item = WatchlistItemEntity(
            watchlistId = watchlistId,
            symbol = symbol,
            companyName = "",
            sortOrder = newOrder,
        )
        watchlistItemDao.insertItem(item)
    }
}
