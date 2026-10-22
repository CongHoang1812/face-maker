package com.conght.facemakerapp.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.conght.facemakerapp.data.model.Creation
import com.conght.facemakerapp.data.model.CreationType

@Entity(tableName = "creations")
data class CreationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                 // CreationType.name
    @ColumnInfo(name = "image_path") val imagePath: String,
    @ColumnInfo(name = "config_json") val configJson: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun CreationEntity.toModel() = Creation(
    id = id,
    type = CreationType.valueOf(type),
    imagePath = imagePath,
    configJson = configJson,
    createdAt = createdAt,
)
