package io.github.mateorguez.cincopelao.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val subjectId: Long = 0,
    val name: String = "",
    val course: String = "",
    val abbrev: String = ""
)