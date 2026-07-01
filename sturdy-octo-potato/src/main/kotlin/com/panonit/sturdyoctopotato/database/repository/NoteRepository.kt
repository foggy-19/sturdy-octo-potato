package com.panonit.sturdyoctopotato.database.repository

import com.panonit.sturdyoctopotato.database.model.Note
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.MongoRepository

interface NoteRepository : MongoRepository<Note, ObjectId> {
    fun findAllByOwnerId(ownerId: ObjectId): List<Note>
}