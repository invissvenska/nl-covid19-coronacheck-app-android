package nl.rijksoverheid.ctr.persistence.database.dao

import androidx.room.*
import nl.rijksoverheid.ctr.persistence.database.entities.OriginEntity

/*
 *  Copyright (c) 2021 De Staat der Nederlanden, Ministerie van Volksgezondheid, Welzijn en Sport.
 *   Licensed under the EUROPEAN UNION PUBLIC LICENCE v. 1.2
 *
 *   SPDX-License-Identifier: EUPL-1.2
 *
 */
@Dao
interface OriginDao {

    @Query("SELECT * FROM origin")
    suspend fun getAll(): List<OriginEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entity: List<OriginEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: OriginEntity)

    @Delete
    suspend fun delete(entity: OriginEntity)

    @Query("DELETE FROM origin")
    suspend fun deleteAll()
}
