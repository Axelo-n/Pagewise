package com.example.pagewise

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.pagewise.ui.theme.UiDark
import com.example.pagewise.ui.theme.UiLight
import com.example.pagewise.ui.theme.UiMedium
import com.example.pagewise.ui.theme.White
import kotlinx.coroutines.launch

// 1. HAPUS 'class SearchScreen {'
// Biarkan @Composable langsung di luar / top-level

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onAddBook: (GramediaBook) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<GramediaBook>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            Surface(color = UiDark, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(8.dp, 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = White)
                    }
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search Books...") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    scope.launch {
                                        isLoading = true
                                        searchResults = searchGramediaOfficial(searchQuery)
                                        isLoading = false
                                    }
                                }
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = UiDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(end = 8.dp).height(50.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(White)) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = UiDark)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResults) { book ->
                        GramediaResultCard(book = book, onAddClick = { onAddBook(book) })
                    }
                }
            }
        }
    }
}

// Desain Card Khusus Hasil Pencarian
@Composable
fun GramediaResultCard(
    book: GramediaBook,
    onAddClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().height(115.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(80.dp).fillMaxHeight().background(UiLight)) {
                AsyncImage(
                    model = book.imageUrl,
                    contentDescription = "Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(12.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = book.title, style = MaterialTheme.typography.titleMedium, fontSize = 16.sp, color = UiDark, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = book.author, style = MaterialTheme.typography.labelMedium, fontSize = 13.sp, color = UiMedium, maxLines = 1)

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = onAddClick,
                    modifier = Modifier.align(Alignment.End).height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = UiDark),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                ) {
                    Text("Add to List", fontSize = 12.sp, color = White)
                }
            }
        }
    }
}