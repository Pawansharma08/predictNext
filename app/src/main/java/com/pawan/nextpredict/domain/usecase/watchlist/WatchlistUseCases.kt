package com.pawan.nextpredict.domain.usecase.watchlist

import com.pawan.nextpredict.domain.model.Watchlist
import com.pawan.nextpredict.domain.model.WatchlistStock
import com.pawan.nextpredict.domain.repository.WatchlistRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAllWatchlistsUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    operator fun invoke(): Flow<List<Watchlist>> = repository.getAllWatchlists()
}

class GetWatchlistItemsUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    operator fun invoke(watchlistId: Long): Flow<List<WatchlistStock>> =
        repository.getWatchlistItems(watchlistId)
}

class IsSymbolInWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    operator fun invoke(watchlistId: Long, symbol: String): Flow<Boolean> =
        repository.isSymbolInWatchlist(watchlistId, symbol)
}

class CreateWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(name: String): Long =
        repository.createWatchlist(name)
}

class DeleteWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(watchlistId: Long) =
        repository.deleteWatchlist(watchlistId)
}

class RenameWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(watchlistId: Long, newName: String) =
        repository.renameWatchlist(watchlistId, newName)
}

class AddStockToWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(watchlistId: Long, symbol: String, companyName: String) =
        repository.addStockToWatchlist(watchlistId, symbol, companyName)
}

class RemoveStockFromWatchlistUseCase @Inject constructor(
    private val repository: WatchlistRepository,
) {
    suspend operator fun invoke(watchlistId: Long, symbol: String) =
        repository.removeStockFromWatchlist(watchlistId, symbol)
}
