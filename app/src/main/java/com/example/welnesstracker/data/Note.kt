package com.example.welnesstracker.data

enum class Mood(val emoji: String) {
    HAPPY("😊"),
    NEUTRAL("😐"),
    SAD("😞");

    companion object {
        fun from(name: String): Mood = values().firstOrNull { it.name == name } ?: NEUTRAL
    }
}

/** One diary entry stored in the local SQLite table `notes`. */
data class Note(val id: Long, val text: String, val mood: String, val createdAt: Long)
