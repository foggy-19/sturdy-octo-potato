package com.panonit.sturdyoctopotato.controllers

import com.panonit.sturdyoctopotato.database.model.Note
import com.panonit.sturdyoctopotato.database.repository.NoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Clock
import kotlin.time.Instant

@RestController
@RequestMapping(value = ["/notes"])
class NoteController(
    private val repository: NoteRepository
) {
    private val ownerId: String
        get() = SecurityContextHolder.getContext().authentication.principal as String

    @PostMapping
    fun save(
        @Valid @RequestBody body: NoteRequest
    ): NoteResponse {
        val note = repository.save(body.toNote())
        return note.toNoteResponse()
    }

    @GetMapping
    fun findAllByOwnerId(): List<NoteResponse> {
        return repository.findAllByOwnerId(ObjectId(ownerId)).map { note -> note.toNoteResponse() }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(
        @PathVariable("id") id: String
    ) {
        val note = repository.findById(ObjectId(id)).getOrNull()
            ?: throw IllegalArgumentException("note not found")

        if (note.ownerId.toHexString() != ownerId) {
            throw IllegalArgumentException("Owner does not belong to this note")
        }

        repository.deleteById(ObjectId(id))
    }

    data class NoteRequest(
        val id: String?,
        @field:NotBlank(message = "Title can't be blank")
        val title: String,
        val content: String,
        val color: Long,
    )

    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant,
    )

    private fun NoteRequest.toNote(): Note {
        return Note(
            id = id?.let { ObjectId(it) } ?: ObjectId.get(),
            title = title,
            content = content,
            color = color,
            createdAt = Clock.System.now(),
            ownerId = ObjectId(ownerId)
        )
    }

    private fun Note.toNoteResponse(): NoteResponse {
        return NoteResponse(
            id = id.toHexString(),
            title = title,
            content = content,
            color = color,
            createdAt = createdAt,
        )
    }
}