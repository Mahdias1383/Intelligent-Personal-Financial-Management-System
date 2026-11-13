package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CounterpartyEntity::class,
        ObligationEntity::class,
        InstallmentEntity::class,
        LedgerSubRecordEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TarazinoDatabase : RoomDatabase() {
    abstract fun tarazinoDao(): TarazinoDao

    companion object {
        @Volatile
        private var INSTANCE: TarazinoDatabase? = null

        fun getInstance(context: Context): TarazinoDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    TarazinoDatabase::class.java,
                    "tarazino_offline_vault.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
