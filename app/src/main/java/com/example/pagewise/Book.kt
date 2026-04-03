package com.example.pagewise

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books") // Ini bikin tabel namanya 'books'
data class Book(
    @PrimaryKey(autoGenerate = true) // ID nambah otomatis (1, 2, 3...)
    val id: Int = 0,
    val title: String,
    val subtitle: String,
    val status: String,
    val currentPage: Int,
    val totalPages: Int,
    val imagePath: String? = null, // Kolom baru buat simpan alamat foto
    var notes: String = ""
)