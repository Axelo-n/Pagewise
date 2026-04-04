package com.example.pagewise

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pagewise.ui.theme.PagewiseTheme
import com.example.pagewise.ui.theme.UiDark
import com.example.pagewise.ui.theme.White
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import androidx.core.net.toUri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.horizontalScroll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    bookDao: BookDao,
    bookToEdit: Book? = null,
    onNavigateBack: () -> Unit
) {

    BackHandler {
        onNavigateBack()
    }
    // --- STATE FORM (Diisi data lama kalo mode edit) ---
    var title by remember { mutableStateOf(bookToEdit?.title ?: "") }
    var subtitle by remember { mutableStateOf(bookToEdit?.subtitle ?: "") }
    var status by remember { mutableStateOf(bookToEdit?.status ?: "Reading") }
    var currentPage by remember { mutableStateOf(bookToEdit?.currentPage?.toString() ?: "") }
    var totalPages by remember { mutableStateOf(bookToEdit?.totalPages?.toString() ?: "") }

    // Logic gambar: Kalo ada gambar lama, pake itu. Kalo user pick baru, ganti.
    var imageUri by remember { mutableStateOf(bookToEdit?.imagePath?.toUri()) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val flag = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(it, flag)
            } catch (e: Exception) {}
            imageUri = it
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                // Judul berubah dinamis
                title = { Text(if (bookToEdit == null) "Add New Book" else "Edit Book", color = White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = UiDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. INPUT GAMBAR
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.LightGray)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Selected Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Tap to change\nCover", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.DarkGray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. INPUT TEXT FIELDS
            OutlinedTextField(
                value = title, onValueChange = { title = it }, label = { Text("Book Title") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle / Author") },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedTextField(
                    value = currentPage, onValueChange = { currentPage = it }, label = { Text("Current Page") },
                    modifier = Modifier.weight(1f).padding(end = 5.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = totalPages, onValueChange = { totalPages = it }, label = { Text("Total Page") },
                    modifier = Modifier.weight(1f).padding(start = 5.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Status:", modifier = Modifier.align(Alignment.Start), style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()), // <--- INI KUNCI BIAR BISA DI-SWIPE KE SAMPING
                horizontalArrangement = Arrangement.spacedBy(8.dp) // Kasih jarak dikit antar chip biar ga dempetan
            ) {
                listOf("Reading", "Finished", "On Shelf", "Wishlist").forEach { statusOption ->
                    FilterChip(
                        selected = status == statusOption,
                        onClick = { status = statusOption },
                        label = { Text(statusOption) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // 3. TOMBOL SAVE (LOGIC INSERT vs UPDATE)
            Button(
                onClick = {
                    if (title.isNotEmpty() && totalPages.isNotEmpty()) {
                        scope.launch {
                            // Tentukan final image path-nya
                            var finalImagePath = bookToEdit?.imagePath // Default pakai path lama (kalo mode edit)

                            // Cek apakah imageUri bukan null DAN ini dari galeri (awalan content://)
                            // Kalau user milih gambar baru, kita compress & save ke HP!
                            if (imageUri != null && imageUri.toString().startsWith("content://")) {
                                val savedFilePath = compressAndSaveImage(context, imageUri!!)
                                if (savedFilePath != null) {
                                    finalImagePath = savedFilePath // Pake path yang baru di-compress
                                }
                            }

                            val bookData = Book(
                                id = bookToEdit?.id ?: 0,
                                title = title,
                                subtitle = subtitle,
                                status = status,
                                currentPage = currentPage.toIntOrNull() ?: 0,
                                totalPages = totalPages.toIntOrNull() ?: 0,
                                imagePath = finalImagePath
                            )

                            if (bookToEdit == null) {
                                // --- INSERT ---
                                bookDao.insertBook(bookData)
                            } else {
                                // --- UPDATE ---
                                bookDao.updateBook(bookData)
                            }
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UiDark)
            ) {
                Text(if (bookToEdit == null) "Save Book" else "Update Changes", fontSize = 16.sp, color = White)
            }
        }
    }
}

suspend fun compressAndSaveImage(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            // 1. Cek ukuran asli gambar tanpa memuatnya ke memori secara penuh
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // 2. Tentukan ukuran maksimal yang aman dan tetap jernih (misal max 800x1200)
            val reqWidth = 800
            val reqHeight = 1200
            var inSampleSize = 1

            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight: Int = options.outHeight / 2
                val halfWidth: Int = options.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            // 3. Load gambar asli tapi sudah di-downsample rasionya
            options.inJustDecodeBounds = false
            options.inSampleSize = inSampleSize
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return@withContext null

            // 4. Bikin file baru di penyimpanan internal aplikasi (biar gak hilang)
            val fileName = "cover_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            // 5. Compress dan simpan! (Quality 80% itu sweet spot, file kecil tapi tetep tajem)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            // Balikin path file yang udah jadi biar bisa disave ke Room Database
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AddBookScreenPreview() {
    val mockDao = object : BookDao {
        override fun getAllBooks(): Flow<List<Book>> = flowOf(emptyList())
        override suspend fun insertBook(book: Book) {}
        override suspend fun deleteBook(book: Book) {}
        override suspend fun updateBook(book: Book) {}

        override suspend fun getBookById(id: Int): Book? {
            return null
        }

        override suspend fun getReadingBook(): Book? {
            return null
        }
    }

    PagewiseTheme {
        AddBookScreen(
            bookDao = mockDao,
            onNavigateBack = {}
        )
    }
}