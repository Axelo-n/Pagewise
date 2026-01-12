package com.example.pagewise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.pagewise.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PagewiseTheme {
                val context = LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }
                val bookDao = db.bookDao()

                // STATE NAVIGASI
                var currentScreen by remember { mutableStateOf("HOME") }
                // Variabel buat nyimpen buku mana yang lagi mau diedit
                var selectedBookToEdit by remember { mutableStateOf<Book?>(null) }

                if (currentScreen == "HOME") {
                    HomeScreen(
                        bookDao = bookDao,
                        onAddClick = {
                            selectedBookToEdit = null // Pastikan kosong buat nambah baru
                            currentScreen = "ADD_OR_EDIT"
                        },
                        onEditClick = { book ->
                            selectedBookToEdit = book // Isi data buku yang mau diedit
                            currentScreen = "ADD_OR_EDIT"
                        }
                    )
                } else {
                    // Layar ini sekarang REUSABLE (Bisa Add, Bisa Edit)
                    AddBookScreen(
                        bookDao = bookDao,
                        bookToEdit = selectedBookToEdit, // Kirim datanya kesini
                        onNavigateBack = { currentScreen = "HOME" }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    bookDao: BookDao,
    onAddClick: () -> Unit,
    onEditClick: (Book) -> Unit // Callback pas card diklik
) {
    val bookList by bookDao.getAllBooks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // --- STATE DELETE DIALOG ---
    // Kalau ini ada isinya (gak null), dialog muncul
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Book?") },
            text = { Text("Are you sure you want to delete '${bookToDelete?.title}'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            bookToDelete?.let { bookDao.deleteBook(it) }
                            bookToDelete = null // Tutup dialog
                        }
                    }
                ) { Text("Yes, Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = { Header() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = UiDark,
                contentColor = White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(painter = painterResource(id = R.drawable.ic_plus_button), contentDescription = "Add", modifier = Modifier.size(24.dp))
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = White) {
            Image(painter = painterResource(id = R.drawable.bg_ui), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
                ) {
                    items(bookList) { bookItem ->
                        BookCard(
                            book = bookItem,
                            onClick = { onEditClick(bookItem) }, // Klik Card -> Edit
                            onDeleteClick = { bookToDelete = bookItem } // Klik Sampah -> Muncul Dialog
                        )
                    }
                }
            }
        }
    }
}

// ... Header tetep sama ...
@Composable
fun Header() {
    Surface(modifier = Modifier.fillMaxWidth(), color = UiDark) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.statusBarsPadding().fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            Text(text = "Pagewise", color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 24.sp, lineHeight = 24.sp)
            Text(text = "Welcome back, Michael!", color = Color.White, style = MaterialTheme.typography.labelMedium, fontSize = 16.sp, lineHeight = 18.sp, modifier = Modifier.offset(y = (-6).dp))
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,      // Callback klik card
    onDeleteClick: () -> Unit // Callback klik sampah
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = UiLight),
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .shadow(elevation = 10.dp)
            .clickable { onClick() } // Bikin Card bisa diklik buat edit
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(90.dp).fillMaxHeight().zIndex(1f).background(Color.Gray)) {
                if (book.imagePath != null) {
                    AsyncImage(
                        model = book.imagePath,
                        contentDescription = "Book Cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Column(modifier = Modifier.padding(horizontal = 10.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
                Row(modifier = Modifier.fillMaxWidth().height(25.dp)) {
                    Box(modifier = Modifier.fillMaxHeight().weight(1f), contentAlignment = Alignment.CenterStart) {
                        Image(painter = painterResource(id = R.drawable.bg_bookmark), contentDescription = null, modifier = Modifier.fillMaxSize().offset(x = (-25).dp), contentScale = ContentScale.Fit, alignment = Alignment.CenterStart)
                        Text(text = book.status, color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 14.sp, lineHeight = 14.sp)
                    }

                    // --- TOMBOL TRASH ---
                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .width(25.dp)
                        .clip(shape = RoundedCornerShape(13.dp))
                        .background(White)
                        .clickable { onDeleteClick() }, // Klik Trash -> Panggil fungsi delete
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_trash_can), tint = UiDark, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
                Text(text = book.title, style = MaterialTheme.typography.titleMedium, fontSize = 24.sp, lineHeight = 24.sp, color = UiDark, maxLines = 1)
                Text(text = book.subtitle, style = MaterialTheme.typography.labelMedium, fontSize = 14.sp, lineHeight = 14.sp, color = UiMedium, modifier = Modifier.offset(y = (-6).dp), maxLines = 1)
                val progressValue = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages.toFloat() else 0f
                ProgressBar(progress = progressValue)
                Text(text = "Page ${book.currentPage}/${book.totalPages}", style = MaterialTheme.typography.labelMedium, fontSize = 14.sp, lineHeight = 14.sp, color = UiDark)
            }
        }
    }
}

// ... ProgressBar tetep sama ...
@Composable
fun ProgressBar(progress: Float) {
    Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(White)) {
        if (progress > 0f) Box(modifier = Modifier.fillMaxHeight().weight(progress).clip(RoundedCornerShape(5.dp)).background(UiDark))
        if (progress < 1f) Box(modifier = Modifier.fillMaxHeight().weight(1f - progress).background(White))
    }
}

// --- UPDATE PREVIEW BIAR GAK ERROR ---
class FakeBookDao : BookDao {
    override fun getAllBooks() = flowOf(listOf(
        Book(1, "Preview Title", "Subtitle", "Reading", 50, 200)
    ))

    override suspend fun insertBook(book: Book) {}
    override suspend fun deleteBook(book: Book) {}
    override suspend fun updateBook(book: Book) {}

    // Wajib ada karena getBookById ada di interface asli
    override suspend fun getBookById(id: Int): Book? {
        return Book(1, "Preview Title", "Subtitle", "Reading", 50, 200)
    }

    // 👇 TAMBAHKAN INI (PENYEBAB ERROR) 👇
    override suspend fun getReadingBook(): Book? {
        // Balikin buku dummy biar preview gak error
        return Book(1, "Funiculi Funicula", "Reading Preview", "Reading", 50, 200)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewHome() {
    PagewiseTheme {
        HomeScreen(bookDao = FakeBookDao(), onAddClick = {}, onEditClick = {})
    }
}