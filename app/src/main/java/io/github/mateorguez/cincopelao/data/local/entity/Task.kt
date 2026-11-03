package io.github.mateorguez.cincopelao.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = Subject::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Task::class,
            parentColumns = ["taskId"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subjectId"), Index("parentTaskId")]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val taskId: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val isCompleted: Boolean = false,
    val archived: Boolean = false,
    val subjectId: Long? = null,
    val parentTaskId: Long? = null
)