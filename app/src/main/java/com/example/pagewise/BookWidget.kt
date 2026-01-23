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

    LaunchedEffect(Unit) {
        if (previewBook == null) {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(context)
                val fetchedBook = db.bookDao().getReadingBook()

                // LOGIKA LOAD GAMBAR (SUPPORT URI & FILE)
                if (fetchedBook != null && !fetchedBook.imagePath.isNullOrEmpty()) {
                    try {
                        val imagePath = fetchedBook.imagePath
                        val options = BitmapFactory.Options()
                        options.inSampleSize = 4 // Tetap perkecil biar hemat memori

                        // Cek apakah ini URI (content://) atau File biasa
                        if (imagePath.startsWith("content://")) {
                            // CARA 1: Load dari URI (Galeri)
                            val uri = imagePath.toUri()
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                coverBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                            }
                        } else {
                            // CARA 2: Load dari File Biasa
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

    // CONTAINER UTAMA (CARD TANPA SHADOW)
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(110.dp)
            .padding(0.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // BACKGROUND KARTU (Solid Color + Rounded)
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(textPrimary)
                .cornerRadius(10.dp)
        ) {
            if (book != null) {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. COVER IMAGE (Kiri)
                    Box(
                        modifier = GlanceModifier
                            .width(65.dp)
                            .fillMaxHeight()
                            .cornerRadius(0.dp)
                            .background(textSubtitle),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverBitmap != null) {
                            Image(
                                provider = ImageProvider(coverBitmap!!),
                                contentDescription = "Cover",
                                contentScale = ContentScale.Fit,
                                modifier = GlanceModifier.fillMaxSize().cornerRadius(0.dp)
                            )
                        }
                    }

                    // 2. KONTEN KANAN
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- JUDUL ---
                        Text(
                            text = book!!.title,
                            modifier = GlanceModifier.padding(bottom = 0.dp),
                            style = TextStyle(
                                color = whiteProvider,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            maxLines = 1
                        )

                        // --- SUBTITLE ---
                        Text(
                            text = book!!.subtitle,
                            modifier = GlanceModifier.padding(top = 0.dp),
                            style = TextStyle(
                                color = bgCard,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1
                        )

                        Spacer(modifier = GlanceModifier.height(8.dp))

                        // --- PROGRESS BAR ---
                        val progress = if (book!!.totalPages > 0) book!!.currentPage.toFloat() / book!!.totalPages.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = GlanceModifier.fillMaxWidth().height(8.dp),
                            color = whiteProvider,
                            backgroundColor = textSubtitle
                        )

                        Spacer(modifier = GlanceModifier.height(4.dp))

                        // --- TEXT HALAMAN ---
                        Text(
                            text = "${book!!.currentPage}/${book!!.totalPages}",
                            style = TextStyle(
                                color = whiteProvider,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            } else {
                // EMPTY STATE
                Box(
                    modifier = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No Reading Book", style = TextStyle(color = textSubtitle))
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
