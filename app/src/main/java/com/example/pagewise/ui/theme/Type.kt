package com.example.pagewise.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.pagewise.R

// 1. Daftarkan semua file statis yang kamu punya
val AfacadFamily = FontFamily(
    Font(R.font.afacad_regular, FontWeight.Normal),
    Font(R.font.afacad_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.afacad_medium, FontWeight.Medium),
    Font(R.font.afacad_mediumitalic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.afacad_semibold, FontWeight.SemiBold),
    Font(R.font.afacad_semibolditalic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.afacad_bold, FontWeight.Bold),
    Font(R.font.afacad_bolditalic, FontWeight.Bold, FontStyle.Italic)
)

// 2. Terapkan ke Typography
val Typography = Typography(
    // Style untuk "Pagewise" (24sp, SemiBold)
    titleMedium = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),
    // Style untuk "Welcome back" (16sp, Normal)
    bodyLarge = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    // Default untuk teks lainnya
    labelMedium = TextStyle(
        fontFamily = AfacadFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)