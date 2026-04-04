package com.example.pagewise

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.pagewise.ui.theme.UiDark
import com.example.pagewise.ui.theme.UiLight
import com.example.pagewise.ui.theme.UiMedium
import com.example.pagewise.ui.theme.White
import androidx.compose.ui.text.TextRange
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    book: Book,
    onNavigateBack: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSaveNotes: (String) -> Unit
) {
    // State buat handle input teks notes dan mode edit
    var isEditingNotes by remember { mutableStateOf(false) }
    var notesValue by remember { mutableStateOf(TextFieldValue(book.notes)) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    BackHandler {
        // Kalau lagi ngedit notes, tombol back nutup mode editnya dulu
        if (isEditingNotes) {
            isEditingNotes = false
            onSaveNotes(notesValue.text)
        } else {
            // Kalau nggak lagi ngedit, baru beneran balik ke Home
            onNavigateBack()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = White,
            titleContentColor = UiDark,
            textContentColor = UiMedium,
            icon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_trash_can),
                    contentDescription = "Delete Icon",
                    tint = Color(0xFFE53935),
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
                    text = "Are you sure you want to delete '${book.title}'?\nThis action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteClick() // <--- Baru beneran dihapus pas diklik Delete
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
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = UiDark)
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Book", tint = White)
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Book", tint = Color(0xFFEF5350))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = UiDark, // Nyatu sama background atas
                    scrolledContainerColor = UiDark
                )
            )
        },
        containerColor = White
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(White),
            contentPadding = PaddingValues(bottom = 100.dp) // Spasi bawah biar lega
        ) {
            // --- HEADER SECTION (ESTETIK) ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Background Lengkung Hitam di atas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp))
                            .background(UiDark)
                    )

                    // Cover Buku Numpang di tengah (Elevated)
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                        modifier = Modifier
                            .padding(top = 60.dp)
                            .width(140.dp)
                            .height(210.dp)
                            .zIndex(1f)
                    ) {
                        AsyncImage(
                            model = book.imagePath,
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize().background(UiLight),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // --- INFO SECTION ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = UiDark,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = book.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = UiMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Badge Status Estetik
                    Surface(
                        color = UiLight,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.border(1.dp, UiMedium.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    ) {
                        Text(
                            text = book.status.uppercase(),
                            color = UiDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // --- PROGRESS SECTION ---
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reading Progress", fontWeight = FontWeight.SemiBold, color = UiDark)
                        Text("${book.currentPage} / ${book.totalPages} Pages", color = UiMedium, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val progressValue = if (book.totalPages > 0) book.currentPage.toFloat() / book.totalPages.toFloat() else 0f
                    LinearProgressIndicator(
                        progress = progressValue,
                        modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                        color = UiDark,
                        trackColor = UiLight
                    )
                }
            }

            // --- SEPARATOR ---
            item {
                Divider(modifier = Modifier.padding(horizontal = 24.dp), color = UiLight, thickness = 2.dp)
            }

            // --- NOTES SECTION ---
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Personal Notes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = UiDark)

                        if (isEditingNotes) {
                            TextButton(onClick = {
                                isEditingNotes = false
                                onSaveNotes(notesValue.text)
                            }) {
                                Icon(Icons.Rounded.Check, contentDescription = "Save", tint = UiDark)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save", color = UiDark, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            TextButton(onClick = { isEditingNotes = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = UiMedium, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit Notes", color = UiMedium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingNotes) {
                        // AREA KETIK NOTES
                        OutlinedTextField(
                            value = notesValue,
                            onValueChange = { notesValue = it },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            placeholder = { Text("Write your summaries, favorite quotes, or thoughts here...") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UiDark,
                                unfocusedBorderColor = UiMedium.copy(alpha = 0.5f),
                                focusedContainerColor = UiLight.copy(alpha = 0.2f),
                                unfocusedContainerColor = White,
                                focusedTextColor = UiDark,
                                unfocusedTextColor = UiDark,
                                cursorColor = UiDark
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    } else {
                        // AREA BACA NOTES
                        Card(
                            colors = CardDefaults.cardColors(containerColor = UiLight.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (book.notes.isEmpty()) "No notes yet. Tap 'Edit Notes' to start writing." else book.notes,
                                color = if (book.notes.isEmpty()) UiMedium else UiDark,
                                modifier = Modifier.padding(16.dp),
                                fontSize = 15.sp,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tombol Kecil buat Toolbar Notes
@Composable
fun FormatButton(text: String, onClick: () -> Unit) {
    Surface(
        color = UiLight,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.border(1.dp, UiMedium.copy(alpha = 0.2f), RoundedCornerShape(6.dp)),
        onClick = onClick
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = UiDark, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}