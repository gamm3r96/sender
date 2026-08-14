package com.example.data

import kotlinx.coroutines.flow.Flow

class TransferRepository(private val transferDao: TransferDao) {
    val allTransfers: Flow<List<TransferRecord>> = transferDao.getAllTransfers()
    val favoriteTransfers: Flow<List<TransferRecord>> = transferDao.getFavoriteTransfers()

    suspend fun getById(id: Long): TransferRecord? = transferDao.getTransferById(id)

    suspend fun insert(record: TransferRecord): Long = transferDao.insertTransfer(record)

    suspend fun update(record: TransferRecord) = transferDao.updateTransfer(record)

    suspend fun delete(record: TransferRecord) = transferDao.deleteTransfer(record)

    suspend fun deleteById(id: Long) = transferDao.deleteById(id)

    suspend fun toggleFavorite(id: Long, current: Boolean) = transferDao.updateFavorite(id, !current)
}

class TeamKeyRepository(private val teamKeyDao: TeamKeyDao) {
    val allTeamKeys: Flow<List<TeamKey>> = teamKeyDao.getAllTeamKeys()

    suspend fun getDefaultTeamKey(): TeamKey? = teamKeyDao.getDefaultTeamKey()

    suspend fun insert(teamKey: TeamKey): Long = teamKeyDao.insertTeamKey(teamKey)

    suspend fun update(teamKey: TeamKey) = teamKeyDao.updateTeamKey(teamKey)

    suspend fun delete(teamKey: TeamKey) = teamKeyDao.deleteTeamKey(teamKey)

    suspend fun setDefault(id: Long) {
        teamKeyDao.clearDefaultTeamKey()
        teamKeyDao.setDefaultTeamKey(id)
    }
}
