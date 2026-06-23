package com.sam.stt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.sam.stt.model.TransferSession
import com.sam.stt.model.TransferStatus

@Database(
    entities = [TransferSession::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class STTDatabase : RoomDatabase() {
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: STTDatabase? = null

        fun getDatabase(context: Context): STTDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    STTDatabase::class.java,
                    "stt_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class Converters {
    @androidx.room.TypeConverter
    fun fromTransferStatus(value: TransferStatus): String = value.name

    @androidx.room.TypeConverter
    fun toTransferStatus(value: String): TransferStatus = TransferStatus.valueOf(value)
}
