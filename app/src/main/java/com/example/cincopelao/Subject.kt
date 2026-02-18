package com.example.cincopelao

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val subjectId: Int = 0,
    val name: String = "",
    val course: String = "",
    val abbrev: String = ""
)