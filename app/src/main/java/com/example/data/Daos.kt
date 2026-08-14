package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfer_records ORDER BY timestamp DESC")
    fun getAllTransfers(): Flow<List<TransferRecord>>

    @Query("SELECT * FROM transfer_records WHERE id = :id LIMIT 1")
    suspend fun getTransferById(id: Long): TransferRecord?

    @Query("SELECT * FROM transfer_records WHERE transferId = :transferId LIMIT 1")
    suspend fun getTransferByTransferId(transferId: String): TransferRecord?

    @Query("SELECT * FROM transfer_records WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteTransfers(): Flow<List<TransferRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(record: TransferRecord): Long

    @Update
    suspend fun updateTransfer(record: TransferRecord)

    @Delete
    suspend fun deleteTransfer(record: TransferRecord)

    @Query("DELETE FROM transfer_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE transfer_records SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)
}

@Dao
interface TeamKeyDao {
    @Query("SELECT * FROM team_keys ORDER BY isDefault DESC, createdTimestamp DESC")
    fun getAllTeamKeys(): Flow<List<TeamKey>>

    @Query("SELECT * FROM team_keys WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultTeamKey(): TeamKey?

    @Query("SELECT * FROM team_keys WHERE id = :id LIMIT 1")
    suspend fun getTeamKeyById(id: Long): TeamKey?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeamKey(teamKey: TeamKey): Long

    @Update
    suspend fun updateTeamKey(teamKey: TeamKey)

    @Delete
    suspend fun deleteTeamKey(teamKey: TeamKey)

    @Query("UPDATE team_keys SET isDefault = 0")
    suspend fun clearDefaultTeamKey()

    @Query("UPDATE team_keys SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultTeamKey(id: Long)
}
