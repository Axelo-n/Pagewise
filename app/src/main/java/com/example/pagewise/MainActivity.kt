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
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inisialisasi SharedPreferences
        val sharedPrefs = getSharedPreferences("PagewisePrefs", MODE_PRIVATE)

        setContent {
            PagewiseTheme {
                val context = LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }
                val bookDao = db.bookDao()

                // 2. Ambil nama yang tersimpan (default "" kalau kosong)
                var userName by remember {
                    mutableStateOf(sharedPrefs.getString("user_name", "") ?: "")
                }

                // STATE NAVIGASI
                var currentScreen by remember { mutableStateOf("HOME") }
                var selectedBookToEdit by remember { mutableStateOf<Book?>(null) }

                // 3. LOGIKA PENENTUAN LAYAR: Input Nama vs App Utama
                if (userName.isEmpty()) {
                    // Jika nama kosong, paksa user isi nama dulu
                    WelcomeScreen(onSaveName = { inputName ->
                        sharedPrefs.edit { putString("user_name", inputName) }
                        userName = inputName
                    })
                } else {
                    // Jika nama sudah ada, lanjut ke App
                    if (currentScreen == "HOME") {
                        HomeScreen(
                            bookDao = bookDao,
                            userName = userName, // Kirim nama ke Home
                            onAddClick = {
                                selectedBookToEdit = null
                                currentScreen = "ADD_OR_EDIT"
                            },
                            onEditClick = { book ->
                                selectedBookToEdit = book
                                currentScreen = "ADD_OR_EDIT"
                            }
                        )
                    } else {
                        AddBookScreen(
                            bookDao = bookDao,
                            bookToEdit = selectedBookToEdit,
                            onNavigateBack = { currentScreen = "HOME" }
                        )
                    }
                }
            }
        }
    }
}

// --- LAYAR INPUT NAMA (HANYA MUNCUL SEKALI) ---
@Composable
fun WelcomeScreen(onSaveName: (String) -> Unit) {
    var nameInput by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = White
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.bg_ui),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier.fillMaxSize().padding(30.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Welcome to Pagewise!", style = MaterialTheme.typography.titleMedium, color = White, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(0.dp))
                Text(text = "What should we call you?", color = White, style = MaterialTheme.typography.labelMedium, fontSize = 18.sp)

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Enter your name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UiDark,
                        unfocusedBorderColor = UiMedium,
                        focusedLabelColor = UiLight,
                        cursorColor = UiDark,
                        unfocusedContainerColor = White.copy(alpha = 0.8f),
                        focusedContainerColor = White.copy(alpha = 0.9f)
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { if (nameInput.isNotBlank()) onSaveName(nameInput) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UiDark),
                    enabled = nameInput.isNotBlank()
                ) {
                    Text("Get Started", color = White)
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    bookDao: BookDao,
    userName: String,
    onAddClick: () -> Unit,
    onEditClick: (Book) -> Unit
) {
    val bookList by bookDao.getAllBooks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
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
                            bookToDelete = null
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
        topBar = { Header(userName) },
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
                            onClick = { onEditClick(bookItem) },
                            onDeleteClick = { bookToDelete = bookItem }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Header(userName: String) {
    Surface(modifier = Modifier.fillMaxWidth(), color = UiDark) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.statusBarsPadding().fillMaxWidth().height(60.dp).padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
        ) {
            Text(text = "Pagewise", color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 24.sp, lineHeight = 24.sp)
            Text(text = "Welcome back, $userName!", color = Color.White, style = MaterialTheme.typography.labelMedium, fontSize = 16.sp, lineHeight = 18.sp, modifier = Modifier.offset(y = (-6).dp))
        }
    }
}

@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = UiLight),
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .shadow(elevation = 10.dp)
            .clickable { onClick() }
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

                    Box(modifier = Modifier
                        .fillMaxHeight()
                        .width(25.dp)
                        .clip(shape = RoundedCornerShape(13.dp))
                        .background(White)
                        .clickable { onDeleteClick() },
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

@Composable
fun ProgressBar(progress: Float) {
    Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(White)) {
        if (progress > 0f) Box(modifier = Modifier.fillMaxHeight().weight(progress).clip(RoundedCornerShape(5.dp)).background(UiDark))
        if (progress < 1f) Box(modifier = Modifier.fillMaxHeight().weight(1f - progress).background(White))
    }
}

class FakeBookDao : BookDao {
    override fun getAllBooks() = flowOf(listOf(
        Book(1, "Preview Title", "Subtitle", "Reading", 50, 200)
    ))

    override suspend fun insertBook(book: Book) {}
    override suspend fun deleteBook(book: Book) {}
    override suspend fun updateBook(book: Book) {}

    override suspend fun getBookById(id: Int): Book? {
        return Book(1, "Preview Title", "Subtitle", "Reading", 50, 200)
    }

    override suspend fun getReadingBook(): Book? {
        return Book(1, "Funiculi Funicula", "Reading Preview", "Reading", 50, 200)
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewHome() {
    PagewiseTheme {
        HomeScreen(
            bookDao = FakeBookDao(), onAddClick = {}, onEditClick = {}, userName = "test"
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewWelcome() {
    PagewiseTheme {
        WelcomeScreen(onSaveName = {})
    }
}