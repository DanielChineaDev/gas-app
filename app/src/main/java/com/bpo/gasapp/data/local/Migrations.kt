package com.bpo.gasapp.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: tabla de historial de precios. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `price_history` (" +
                "`stationId` TEXT NOT NULL, `fuel` TEXT NOT NULL, `price` REAL NOT NULL, " +
                "`timestamp` INTEGER NOT NULL, PRIMARY KEY(`stationId`, `fuel`, `timestamp`))"
        )
    }
}

/** v2 -> v3: tabla de repostajes. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `refuels` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stationId` TEXT, " +
                "`stationName` TEXT NOT NULL, `fuel` TEXT NOT NULL, `liters` REAL NOT NULL, " +
                "`amount` REAL NOT NULL, `timestamp` INTEGER NOT NULL)"
        )
    }
}

/** v3 -> v4: cuentakilómetros en repostajes. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `refuels` ADD COLUMN `odometer` REAL")
    }
}

/** v4 -> v5: vehículos y asociación de repostajes a vehículo. */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `vehicles` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`fuel` TEXT NOT NULL, `consumption` REAL NOT NULL)"
        )
        db.execSQL("ALTER TABLE `refuels` ADD COLUMN `vehicleId` INTEGER")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
