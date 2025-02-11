package nl.rijksoverheid.ctr.persistence.database

import android.content.ContentValues
import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SQLiteException
import net.sqlcipher.database.SupportFactory
import nl.rijksoverheid.ctr.persistence.PersistenceManager
import nl.rijksoverheid.ctr.persistence.database.converters.HolderDatabaseConverter
import nl.rijksoverheid.ctr.persistence.database.dao.*
import nl.rijksoverheid.ctr.persistence.database.entities.*
import nl.rijksoverheid.ctr.shared.models.Environment
import nl.rijksoverheid.ctr.shared.utils.AndroidUtil
import java.io.File

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE origin ADD COLUMN doseNumber INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE credential ADD COLUMN category VARCHAR")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE event_group ADD COLUMN scope TEXT")
        database.execSQL("DROP INDEX index_event_group_provider_identifier_type")
        database.execSQL("CREATE UNIQUE INDEX index_event_group_provider_identifier_type_scope ON event_group(provider_identifier, type, scope)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {

    /**
     * Fix for possible duplicate events inserted into to the database.
     * Scope parameter was added to event group with unique constraint.
     * However scope was set to nullable which breaks the unique constraint.
     * This migration removes duplicate entries and updates null scopes to empty strings.
     * Scope is set to non nullable in table.
     */
    override fun migrate(database: SupportSQLiteDatabase) {
        // create new table for event group with scope not null
        database.execSQL("CREATE TABLE IF NOT EXISTS event_group_temp (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, wallet_id INTEGER NOT NULL, provider_identifier TEXT NOT NULL, type TEXT NOT NULL, scope TEXT NOT NULL, maxIssuedAt INTEGER NOT NULL, jsonData BLOB NOT NULL, FOREIGN KEY(wallet_id) REFERENCES wallet(id) ON UPDATE NO ACTION ON DELETE CASCADE )")

        // remove duplicate events with null scope. Update null scope to empty string
        database.execSQL(
            "DELETE FROM event_group WHERE EXISTS " +
                    "(SELECT 1 FROM event_group e2 " +
                    "WHERE event_group.provider_identifier = e2.provider_identifier " +
                    "AND event_group.type = e2.type " +
                    "AND scope IS NULL " +
                    "AND event_group.rowid < e2.rowid)"
        )
        database.execSQL("UPDATE event_group SET scope = '' WHERE scope IS NULL")

        // copy data from old event group table to new one
        database.execSQL("INSERT INTO event_group_temp (id, wallet_id, provider_identifier, type, scope, maxIssuedAt, jsonData) SELECT id, wallet_id, provider_identifier, type, scope, maxIssuedAt, jsonData FROM event_group")

        // delete old event group table and index
        database.execSQL("DROP TABLE IF EXISTS event_group")
        database.execSQL("DROP INDEX IF EXISTS index_event_group_wallet_id")
        database.execSQL("DROP INDEX IF EXISTS index_event_group_provider_identifier_type_scope")

        // rename new table and set index
        database.execSQL("ALTER TABLE event_group_temp RENAME TO event_group")
        database.execSQL("CREATE INDEX index_event_group_wallet_id ON event_group(wallet_id)")
        database.execSQL("CREATE UNIQUE INDEX index_event_group_provider_identifier_type_scope ON event_group(provider_identifier, type, scope)")
    }
}

/**
 * Remove [EventGroupEntity.maxIssuedAt] and replace it with [EventGroupEntity.expiryDate]
 */
val MIGRATION_5_6 = object: Migration(5,6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS event_group_temp (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, wallet_id INTEGER NOT NULL, provider_identifier TEXT NOT NULL, type TEXT NOT NULL, scope TEXT NOT NULL, expiryDate INTEGER, jsonData BLOB NOT NULL, FOREIGN KEY(wallet_id) REFERENCES wallet(id) ON UPDATE NO ACTION ON DELETE CASCADE )")
        database.execSQL("INSERT INTO event_group_temp (id, wallet_id, provider_identifier, type, scope, expiryDate, jsonData) SELECT id, wallet_id, provider_identifier, type, scope, null, jsonData FROM event_group")
        database.execSQL("DROP TABLE IF EXISTS event_group")
        database.execSQL("DROP INDEX IF EXISTS index_event_group_wallet_id")
        database.execSQL("DROP INDEX IF EXISTS index_event_group_provider_identifier_type_scope")
        database.execSQL("ALTER TABLE event_group_temp RENAME TO event_group")
        database.execSQL("CREATE INDEX index_event_group_wallet_id ON event_group(wallet_id)")
        database.execSQL("CREATE UNIQUE INDEX index_event_group_provider_identifier_type_scope ON event_group(provider_identifier, type, scope)")
    }
}

fun MIGRATION_6_7(persistenceManager: PersistenceManager) = object: Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS secret_key (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, green_card_id INTEGER NOT NULL, secret TEXT NOT NULL, FOREIGN KEY(green_card_id) REFERENCES green_card(id) ON UPDATE NO ACTION ON DELETE CASCADE )")
        val domesticGreenCardCursor = database.query("SELECT * FROM green_card WHERE type = 'domestic'")

        // If we have a domestic green card migrate old secret key
        if (domesticGreenCardCursor.count == 1 && persistenceManager.getDatabasePassPhrase() != null) {
            domesticGreenCardCursor.moveToFirst()
            val greenCardIdIndex = domesticGreenCardCursor.getColumnIndex("id")
            val domesticGreenCardId = domesticGreenCardCursor.getInt(greenCardIdIndex)
            val insertValues = ContentValues()
            insertValues.put("green_card_id", domesticGreenCardId)
            insertValues.put("secret", persistenceManager.getDatabasePassPhrase()) // The old database pass phrase is the new secret key
            database.insert("secret_key", 0, insertValues)
        }
    }
}

@Database(
    entities = [WalletEntity::class, EventGroupEntity::class, GreenCardEntity::class, CredentialEntity::class, OriginEntity::class, SecretKeyEntity::class],
    version = 7
)
@TypeConverters(HolderDatabaseConverter::class)
abstract class HolderDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao
    abstract fun greenCardDao(): GreenCardDao
    abstract fun credentialDao(): CredentialDao
    abstract fun eventGroupDao(): EventGroupDao
    abstract fun originDao(): OriginDao
    abstract fun secretKeyDao(): SecretKeyDao

    companion object {
        fun createInstance(
            context: Context,
            persistenceManager: PersistenceManager,
            androidUtil: AndroidUtil,
            environment: Environment
        ): HolderDatabase {
            if (persistenceManager.getDatabasePassPhrase() == null) {
                persistenceManager.saveDatabasePassPhrase(androidUtil.generateRandomKey())
            }

            // From db migration 6 to 7 there was a database encryption key migration issue.
            // This code checks if we can open the database, and if not delete the old database.
            if (environment is Environment.Prod && persistenceManager.getCheckCanOpenDatabase()) {
                try {
                    val file = File(context.filesDir.parentFile, "databases/holder-database")
                    try {
                        SQLiteDatabase.loadLibs(context)
                        SQLiteDatabase.openDatabase(file.absolutePath, persistenceManager.getDatabasePassPhrase(), null, SQLiteDatabase.OPEN_READONLY)
                    } catch (e: SQLiteException) {
                        file.delete()
                    } finally {
                        persistenceManager.setCheckCanOpenDatabase(false)
                    }
                } catch (e: Exception) {
                    // Make sure this hack never crashes
                } finally {
                    persistenceManager.setCheckCanOpenDatabase(false)
                }
            }

            return Room
                .databaseBuilder(context, HolderDatabase::class.java, "holder-database")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7(persistenceManager))
                .apply {
                    if (environment !is Environment.InstrumentationTests) {
                        val supportFactory = SupportFactory(SQLiteDatabase.getBytes(persistenceManager.getDatabasePassPhrase()?.toCharArray()))
                        openHelperFactory(supportFactory)
                    }
                }.build()
        }
    }
}
