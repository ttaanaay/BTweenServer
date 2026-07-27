package com.btween.app.data.local.mapper

import com.btween.app.data.local.entity.QuoteEntity
import com.btween.app.domain.model.Category
import com.btween.app.domain.model.Quote
import com.btween.app.domain.model.SourceType

fun QuoteEntity.toDomain(category: Category?): Quote = Quote(
    id = id,
    text = text,
    sourceTitle = sourceTitle,
    sourceType = SourceType.fromName(sourceType),
    speaker = speaker,
    author = author,
    category = category,
    tags = tags,
    note = note,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastViewedAt = lastViewedAt
)

fun Quote.toEntity(): QuoteEntity = QuoteEntity(
    id = id,
    text = text,
    sourceTitle = sourceTitle,
    sourceType = sourceType.name,
    speaker = speaker,
    author = author,
    categoryId = category?.id,
    tags = tags,
    note = note,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastViewedAt = lastViewedAt
)
