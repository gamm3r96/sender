package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.crypto.CryptoManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [TransferRecord::class, TeamKey::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao
    abstract fun teamKeyDao(): TeamKeyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cipherqr_vault.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Pre-populate default team key
                        CoroutineScope(Dispatchers.IO).launch {
                            val defaultKey = CryptoManager.generateEphemeralKey()
                            val safetyNum = CryptoManager.generateSafetyNumber("Default Team", defaultKey)
                            val initialTeam = TeamKey(
                                teamName = "Core Security Team",
                                passphraseOrKey = defaultKey,
                                safetyNumber = safetyNum,
                                colorHex = 0xFF10B981,
                                memberCount = 4,
                                isDefault = true
                            )
                            INSTANCE?.teamKeyDao()?.insertTeamKey(initialTeam)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
