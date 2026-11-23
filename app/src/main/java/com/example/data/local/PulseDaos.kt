package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY displayName COLLATE NOCASE ASC")
    fun observeAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isFavorite = 1 ORDER BY displayName COLLATE NOCASE ASC")
    fun observeFavoriteContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE timesContacted > 0 ORDER BY timesContacted DESC, lastContactedAt DESC LIMIT 12")
    fun observeFrequentlyContacted(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts")
    suspend fun getAllContactsOnce(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun findByNormalizedNumber(normalized: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE systemContactId = :systemId LIMIT 1")
    suspend fun findBySystemId(systemId: Long): ContactEntity?

    @Query("SELECT * FROM contacts WHERE googleResourceName = :resourceName LIMIT 1")
    suspend fun findByGoogleResourceName(resourceName: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isFavorite = :favorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, favorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET timesContacted = timesContacted + 1, lastContactedAt = :now WHERE id = :id")
    suspend fun incrementContacted(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM contacts WHERE id = :id")
    suspend fun deleteContactById(id: Long)

    @Query("SELECT * FROM contact_groups ORDER BY name COLLATE NOCASE ASC")
    fun observeAllGroups(): Flow<List<ContactGroupEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGroup(group: ContactGroupEntity): Long

    @Delete
    suspend fun deleteGroup(group: ContactGroupEntity)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun observeAllCallLogs(): Flow<List<CallLogEntity>>

    @Query("SELECT COUNT(*) FROM call_logs")
    fun observeCallLogCount(): Flow<Int>

    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    suspend fun getAllCallLogsOnce(): List<CallLogEntity>

    @Query("SELECT simSlot FROM call_logs WHERE normalizedNumber = :normalized ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastSimSlotForNumber(normalized: String): Int?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCallLog(log: CallLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCallLogs(logs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :id")
    suspend fun deleteCallLogById(id: Long)

    @Query("DELETE FROM call_logs WHERE id IN (:ids)")
    suspend fun deleteCallLogsByIds(ids: List<Long>)

    @Query("DELETE FROM call_logs WHERE timestamp < :cutoffMillis")
    suspend fun deleteLogsOlderThan(cutoffMillis: Long): Int

    @Query("DELETE FROM call_logs")
    suspend fun clearAllLogs()
}

@Dao
interface CallerIdCacheDao {
    @Query("SELECT * FROM caller_id_cache ORDER BY updatedAt DESC")
    fun observeAllEntries(): Flow<List<CallerIdCacheEntity>>

    @Query("SELECT * FROM caller_id_cache WHERE normalizedNumber = :normalized LIMIT 1")
    suspend fun findByNumber(normalized: String): CallerIdCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: CallerIdCacheEntity)

    @Query("DELETE FROM caller_id_cache WHERE normalizedNumber = :normalized")
    suspend fun deleteByNumber(normalized: String)

    @Query("DELETE FROM caller_id_cache")
    suspend fun clearAll()
}
