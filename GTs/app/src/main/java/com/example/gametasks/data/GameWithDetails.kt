package com.example.gametasks.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class GameWithDetails(
    @Embedded val game: GameEntity,

    @Relation(
        parentColumn = "game_id",
        entityColumn = "genre_id",
        associateBy = Junction(GameGenreCrossRef::class)
    )
    val genres: List<GenreEntity>,

    @Relation(
        parentColumn = "game_id",
        entityColumn = "platform_id",
        associateBy = Junction(GamePlatformCrossRef::class)
    )
    val platforms: List<PlatformEntity>
)