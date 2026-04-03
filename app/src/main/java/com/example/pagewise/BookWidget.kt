package com.example.pagewise

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.pagewise.ui.theme.UiDark
import com.example.pagewise.ui.theme.UiLight
import com.example.pagewise.ui.theme.UiMedium
import com.example.pagewise.ui.theme.UiSemi
import com.example.pagewise.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri

class SimpleBookWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SimpleBookWidget()
}

class SimpleBookWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent(context)
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
fun WidgetContent(context: Context, previewBook: Book? = null, previewBitmap: Bitmap? = null) {
    var book by remember { mutableStateOf<Book?>(previewBook) }
    var coverBitmap by remember { mutableStateOf<Bitmap?>(previewBitmap) }

    // ... (Bagian LaunchedEffect SAMA PERSIS, tidak perlu diubah) ...
    LaunchedEffect(Unit) {
        if (previewBook == null) {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(context)
                val fetchedBook = db.bookDao().getReadingBook()

                if (fetchedBook != null && !fetchedBook.imagePath.isNullOrEmpty()) {
                    try {
                        val imagePath = fetchedBook.imagePath
                        val options = BitmapFactory.Options()
                        // ⚠️ PENTING: Jangan terlalu kecil sampleSize-nya biar gambar gak pecah pas di-stretch
                        options.inSampleSize = 2

                        if (imagePath.startsWith("content://")) {
                            val uri = imagePath.toUri()
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                coverBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                            }
                        } else {
                            val file = File(imagePath)
                            if (file.exists()) {
                                coverBitmap = BitmapFactory.decodeFile(file.absolutePath, options)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                book = fetchedBook
            }
        }
    }

    // --- COLORS ---
    val bgCard = ColorProvider(UiLight)
    val textPrimary = ColorProvider(UiDark)
    val textSubtitle = ColorProvider(UiMedium)
    val whiteProvider = ColorProvider(White)

    // CONTAINER UTAMA
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(110.dp) // Tinggi Widget Fixed
            .padding(8.dp) // Padding luar widget
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // BACKGROUND KARTU UTAMA
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(textPrimary)
                .cornerRadius(10.dp)
        ) {
            if (book != null) {
                // ROW UTAMA (Membagi Kiri & Kanan)
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // --- KOLOM KIRI (GAMBAR) ---
                    // Kita set width sekitar 75-80dp (rasio buku umum)
                    // Menggunakan ContentScale.Fit agar gambar TIDAK TERPOTONG
                    Box(
                        modifier = GlanceModifier
                            .width(74.dp) // Lebar area cover
                            .fillMaxHeight() // Tinggi mentok
                            .background(textSubtitle), // Warna background kalau rasio gambar beda
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverBitmap != null) {
                            Image(
                                provider = ImageProvider(coverBitmap!!),
                                contentDescription = "Cover",
                                // Fit: Gambar dimuat utuh, menyesuaikan tinggi/lebar tanpa terpotong
                                contentScale = ContentScale.Crop,
                                modifier = GlanceModifier.fillMaxSize()
                            )
                        } else {
                            // Placeholder kalau gambar null tapi buku ada
                            Image(
                                provider = ImageProvider(R.drawable.ic_launcher_foreground), // Ganti icon placeholder kamu
                                contentDescription = null,
                                modifier = GlanceModifier.size(30.dp),
                                colorFilter = ColorFilter.tint(whiteProvider)
                            )
                        }
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))

                    // --- KOLOM KANAN (INFO) ---
                    // defaultWeight() = Ambil sisa ruang yang ada setelah dikurangi gambar
                    Column(
                        modifier = GlanceModifier
                            .defaultWeight()
                            .fillMaxHeight()
                            .padding(horizontal = 16.dp, vertical = 10.dp), // Padding Kanan-Kiri Text
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- JUDUL ---
                        Text(
                            text = book!!.title,
                            style = TextStyle(
                                color = whiteProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            maxLines = 1
                        )

                        // --- SUBTITLE ---
                        Text(
                            text = book!!.subtitle,
                            modifier = GlanceModifier.padding(top = 2.dp),
                            style = TextStyle(
                                color = bgCard, // Warna agak redup
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = GlanceModifier.height(10.dp))

                        // --- PROGRESS BAR ---
                        val progress = if (book!!.totalPages > 0) book!!.currentPage.toFloat() / book!!.totalPages.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = GlanceModifier.fillMaxWidth().height(6.dp).cornerRadius(3.dp),
                            color = whiteProvider,
                            backgroundColor = textSubtitle
                        )

                        Spacer(modifier = GlanceModifier.height(6.dp))

                        // --- TEXT HALAMAN ---
                        Text(
                            text = "${book!!.currentPage} of ${book!!.totalPages} pages",
                            style = TextStyle(
                                color = whiteProvider,
                                fontSize = 10.sp
                            )
                        )
                    }

                    Spacer(modifier = GlanceModifier.width(16.dp))
                }
            } else {
                // EMPTY STATE
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Reading Book", style = TextStyle(color = whiteProvider))
                }
            }
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 220, heightDp = 110)
@Composable
fun SimpleBookWidgetPreview() {
    val mockBook = Book(
        id = 1,
        title = "Funiculi Funicula",
        subtitle = "Before the Coffee gets cold",
        status = "Reading",
        currentPage = 150,
        totalPages = 320,
        imagePath = null
    )
    GlanceTheme {
        WidgetContent(androidx.glance.LocalContext.current, previewBook = mockBook)
    }
}
