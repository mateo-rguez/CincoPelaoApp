package io.github.mateorguez.cincopelao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "events",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["subjectId"])]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val eventId: Int = 0,

    val subjectId: Int,

    val name: String,

    val type: String,

    val modal: String? = null,

    val isPuntuable: Boolean = false,

    val weight: Float? = null,

    val score: Float? = null,

    val minScore: Float? = null,

    val noPuntuableDone: Boolean = false,

    val date: Long
)