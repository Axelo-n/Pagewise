package com.example.pagewise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.edit
import coil.compose.AsyncImage
import com.example.pagewise.ui.theme.PagewiseTheme
import com.example.pagewise.ui.theme.UiDark
import com.example.pagewise.ui.theme.UiLight
import com.example.pagewise.ui.theme.UiMedium
import com.example.pagewise.ui.theme.White
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPrefs = getSharedPreferences("PagewisePrefs", MODE_PRIVATE)

        setContent {
            PagewiseTheme {
                val context = LocalContext.current
                val db = remember { AppDatabase.getDatabase(context) }
                val bookDao = db.bookDao()

                var userName by remember {
                    mutableStateOf(sharedPrefs.getString("user_name", "") ?: "")
                }

                val scope = rememberCoroutineScope()
                var currentScreen by remember { mutableStateOf("HOME") }
                var selectedBook by remember { mutableStateOf<Book?>(null) }

                if (userName.isEmpty()) {
                    WelcomeScreen(onSaveName = { inputName ->
                        sharedPrefs.edit { putString("user_name", inputName) }
                        userName = inputName
                    })
                } else {
                    if (currentScreen == "HOME") {
                        HomeScreen(
                            bookDao = bookDao,
                            userName = userName,
                            onAddClick = {
                                selectedBook = null
                                currentScreen = "ADD_OR_EDIT"
                            },
                            onEditClick = { book ->
                                selectedBook = book
                                currentScreen = "DETAIL"
                            },
                            onSearchClick = {
                                currentScreen = "SEARCH"
                            }
                        )
                    } else if (currentScreen == "DETAIL" && selectedBook != null) {
                        BookDetailScreen(
                            book = selectedBook!!,
                            onNavigateBack = { currentScreen = "HOME" },
                            onEditClick = { currentScreen = "ADD_OR_EDIT" },
                            onDeleteClick = {
                                scope.launch {
                                    bookDao.deleteBook(selectedBook!!)
                                    currentScreen = "HOME"
                                }
                            },
                            onSaveNotes = { newNotes ->
                                scope.launch {
                                    val updatedBook = selectedBook!!.copy(notes = newNotes)
                                    bookDao.updateBook(updatedBook)
                                    selectedBook = updatedBook // Update state layar
                                }
                            }
                        )
                    } else if (currentScreen == "SEARCH") {
                        // --- LAYAR BARU ---
                        SearchScreen(
                            onNavigateBack = { currentScreen = "HOME" },
                            onAddBook = { scrapedBook ->
                                scope.launch {
                                    val localImagePath = downloadAndSaveImage(context, scrapedBook.imageUrl)
                                    // Masukin data hasil scrape ke database lokal
                                    bookDao.insertBook(
                                        Book(
                                            id = 0, // Auto generate
                                            title = scrapedBook.title,
                                            subtitle = scrapedBook.author,
                                            status = "On Shelf", // DEFAULT STATUS
                                            currentPage = 0,     // PAGES DIKOSONGIN (0)
                                            totalPages = 0,      // PAGES DIKOSONGIN (0)
                                            imagePath = localImagePath
                                        )
                                    )
                                    currentScreen = "HOME" // Langsung balik ke home setelah sukses nambah
                                }
                            }
                        )
                    } else {
                        AddBookScreen(
                            bookDao = bookDao,
                            bookToEdit = selectedBook,
                            onNavigateBack = { currentScreen = "HOME" }
                        )
                    }
                }
            }
        }
    }
}

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
                        // --- BORDER ---
                        focusedBorderColor = UiDark,
                        unfocusedBorderColor = UiMedium,

                        // --- LABEL ("Enter your name") ---
                        focusedLabelColor = UiDark,    // UBAH: Jangan UiLight biar kontras
                        unfocusedLabelColor = UiMedium, // TAMBAH: Biar pas lagi ga diklik tetep kelihatan

                        // --- TEKS KETIKAN USER ---
                        focusedTextColor = UiDark,     // TAMBAH: Biar teks yang diketik warnanya gelap
                        unfocusedTextColor = UiDark,   // TAMBAH: Biar pas kursor pindah teks tetep gelap

                        // --- LAINNYA ---
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
    onEditClick: (Book) -> Unit,
    onSearchClick: () -> Unit
) {
    val bookList by bookDao.getAllBooks().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    if (bookToDelete != null) {
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            shape = RoundedCornerShape(20.dp), // Bikin sudutnya membulat estetik
            containerColor = White, // Background putih bersih
            titleContentColor = UiDark,
            textContentColor = UiMedium,
            // Tambahin icon trash di atas biar ala-ala aplikasi modern
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trash_can),
                    contentDescription = "Delete Icon",
                    tint = Color(0xFFE53935), // Merah yang enak dilihat (Material Red)
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Delete Book?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${bookToDelete?.title}'?\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                // Tombol Delete dibikin Solid Red biar tegas
                Button(
                    onClick = {
                        scope.launch {
                            bookToDelete?.let { bookDao.deleteBook(it) }
                            bookToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935),
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                // Tombol Cancel dibikin TextButton aja (ghost button) warna gelap
                TextButton(
                    onClick = { bookToDelete = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = UiDark
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        topBar = { Header(userName) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(16.dp)) {

                // TOMBOL BARU: SEARCH GRAMEDIA
                FloatingActionButton(
                    onClick = onSearchClick,
                    containerColor = UiLight, // Beda warna dikit biar hierarkinya jelas
                    contentColor = UiDark,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search Gramedia", modifier = Modifier.size(24.dp))
                }

                // TOMBOL LAMA: MANUAL ADD (+)
                FloatingActionButton(
                    onClick = onAddClick,
                    containerColor = UiDark,
                    contentColor = White,
                    shape = RoundedCornerShape(16.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_plus_button), contentDescription = "Add", modifier = Modifier.size(24.dp))
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = White) {
            Image(painter = painterResource(id = R.drawable.bg_ui), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)

            // OPTIMASI: Column redundan dihapus, LazyColumn langsung jadi child dari Surface
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 20.dp, bottom = 100.dp)
            ) {
                // OPTIMASI: Tambah key = { it.id } agar rendering list super cepat
                items(items = bookList, key = { it.id }) { bookItem ->
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
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp), // OPTIMASI: Pindah dari Modifier.shadow ke properti elevation bawaan
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
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

// OPTIMASI: Ganti Row manual dengan LinearProgressIndicator yang dirender via Canvas (Jauh lebih ringan)
@Composable
fun ProgressBar(progress: Float) {
    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp)),
        color = UiDark,
        trackColor = White
    )
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
            bookDao = FakeBookDao(), userName = "Vin", onAddClick = {},
            onEditClick = {},
        ) {}
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PreviewWelcome() {
    PagewiseTheme {
        WelcomeScreen(onSaveName = {})
    }
}