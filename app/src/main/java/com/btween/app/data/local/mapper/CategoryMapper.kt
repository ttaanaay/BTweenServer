package com.btween.app.data.local.mapper

import com.btween.app.data.local.dao.CategoryWithCount
import com.btween.app.data.local.entity.CategoryEntity
import com.btween.app.domain.model.Category

fun CategoryEntity.toDomain(quoteCount: Int = 0): Category = Category(
    id = id,
    name = name,
    colorHex = colorHex,
    isDefault = isDefault,
    createdAt = createdAt,
    quoteCount = quoteCount
)

fun CategoryWithCount.toDomain(): Category = Category(
    id = id,
    name = name,
    colorHex = colorHex,
    isDefault = isDefault,
    createdAt = createdAt,
    quoteCount = quoteCount
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    colorHex = colorHex,
    isDefault = isDefault,
    createdAt = createdAt
)
