package com.example.phoenx.domain.model

data class Ranking(
    val id: String,
    val title: String,
    val itemCount: Int,
    val items: List<String>,
    val coverImageUrl: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
