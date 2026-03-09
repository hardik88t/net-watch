package com.netwatch.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.netwatch.app.data.local.dao.AnnotationDao
import com.netwatch.app.data.local.dao.NetworkEventDao
import com.netwatch.app.data.local.dao.NetworkProfileDao
import com.netwatch.app.data.local.dao.SpeedTestDao
import com.netwatch.app.data.local.dao.StateSnapshotDao
import com.netwatch.app.data.local.entity.AnnotationEntity
import com.netwatch.app.data.local.entity.NetworkEventEntity
import com.netwatch.app.data.local.entity.NetworkProfileEntity
import com.netwatch.app.data.local.entity.SpeedTestResultEntity
import com.netwatch.app.data.local.entity.StateSnapshotEntity

@Database(
    entities = [
        StateSnapshotEntity::class,
        NetworkEventEntity::class,
        SpeedTestResultEntity::class,
        AnnotationEntity::class,
        NetworkProfileEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
@TypeConverters(AppTypeConverters::class)
abstract class NetWatchDatabase : RoomDatabase() {
    abstract fun stateSnapshotDao(): StateSnapshotDao
    abstract fun networkEventDao(): NetworkEventDao
    abstract fun speedTestDao(): SpeedTestDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun networkProfileDao(): NetworkProfileDao

    companion object {
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE network_events ADD COLUMN isException INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
