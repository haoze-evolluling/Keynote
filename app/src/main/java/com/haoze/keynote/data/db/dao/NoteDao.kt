package com.haoze.keynote.data.db.dao

import androidx.room.*
import com.haoze.keynote.data.db.entity.NoteEntity
import com.haoze.keynote.data.db.entity.NoteTagCrossRef
import com.haoze.keynote.data.db.entity.NoteWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY updatedAt DESC")
    fun getAllActiveNotesWithTags(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getAllDeletedNotes(): Flow<List<NoteWithTags>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :noteId")
    fun getNoteWithTagsById(noteId: Long): Flow<NoteWithTags?>

    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%'
           OR id IN (
               SELECT n.id FROM notes n
               INNER JOIN note_tag_cross_ref ntc ON n.id = ntc.noteId
               INNER JOIN tags t ON t.id = ntc.tagId
               WHERE t.name LIKE '%' || :query || '%'
           ))
        ORDER BY updatedAt DESC
    """)
    fun searchNotesWithTags(query: String): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0 AND createdAt BETWEEN :start AND :end
        ORDER BY createdAt DESC
    """)
    fun getActiveNotesByDateRange(start: Long, end: Long): Flow<List<NoteWithTags>>

    @Transaction
    @Query("""
        SELECT * FROM notes
        WHERE isDeleted = 0
        AND createdAt BETWEEN :start AND :end
        AND id IN (
            SELECT noteId FROM note_tag_cross_ref WHERE tagId IN (:tagIds)
        )
        ORDER BY createdAt DESC
    """)
    fun getActiveNotesByDateRangeAndTags(start: Long, end: Long, tagIds: List<Long>): Flow<List<NoteWithTags>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Delete
    suspend fun deleteCrossRef(crossRef: NoteTagCrossRef)

    @Query("DELETE FROM note_tag_cross_ref WHERE noteId = :noteId AND tagId = :tagId")
    suspend fun deleteTagFromNote(noteId: Long, tagId: Long)

    @Query("UPDATE notes SET isDeleted = 1, deletedAt = :deletedAt, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun softDeleteNote(noteId: Long, deletedAt: Long = System.currentTimeMillis(), updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, deletedAt = NULL, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun restoreNote(noteId: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE isDeleted = 1 AND deletedAt < :expireTime")
    suspend fun deleteExpiredTrashNotes(expireTime: Long)

    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun permanentlyDeleteNote(noteId: Long)
}
