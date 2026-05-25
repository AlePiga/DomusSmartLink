package com.example.myapplication.ui.theme
import kotlinx.serialization.Serializable

@Serializable
data class Sensore(
    val id: Int,
    val nome: String,
    val status: Int,
    val type: String
)