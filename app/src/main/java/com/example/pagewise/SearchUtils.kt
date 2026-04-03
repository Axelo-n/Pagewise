package com.example.pagewise

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.net.URL

data class GramediaBook(
    val title: String,
    val author: String,
    val imageUrl: String
)

suspend fun searchGramediaOfficial(query: String): List<GramediaBook> {
    return withContext(Dispatchers.IO) {
        val results = mutableListOf<GramediaBook>()
        try {
            // Kita tembak pencarian Goodreads (Pusatnya database buku dunia & Indo)
            val url = "https://www.goodreads.com/search?q=${query.replace(" ", "+")}"

            // Jsoup beraksi nyaru jadi browser beneran
            val doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                .header("Accept-Language", "id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7")
                .timeout(10000) // Kasih waktu 10 detik biar ga putus di tengah jalan
                .get()

            // Ambil semua baris tabel hasil pencarian buku
            val rows = doc.select("tr[itemtype=http://schema.org/Book]")

            // Ambil maksimal 10 buku biar list-nya ga kepanjangan
            val maxResults = if (rows.size > 10) 10 else rows.size

            for (i in 0 until maxResults) {
                val row = rows[i]

                // Ambil Judul & Penulis pake Selector HTML
                val title = row.select("a.bookTitle span[itemprop=name]").text()
                val author = row.select("a.authorName span[itemprop=name]").text()
                var imageUrl = row.select("img.bookCover").attr("src")

                // TRIK RAHASIA GOODREADS: Hapus kode thumbnail biar dapet cover HD
                // Goodreads nyimpen thumbnail pake nama kayak '._SY75_' atau '._SX50_'
                // Kalo kode ini dihapus, kita otomatis dapet gambar ukuran aslinya!
                imageUrl = imageUrl.replace(Regex("\\._S[XY]\\d+_"), "")

                // Validasi data
                if (title.isNotEmpty()) {
                    results.add(GramediaBook(title, author, imageUrl))
                }
            }

            if (results.isEmpty()) {
                results.add(GramediaBook("Buku Tidak Ditemukan", "Coba tambahin nama penulisnya", ""))
            }

        } catch (e: Exception) {
            e.printStackTrace()
            results.add(GramediaBook("GAGAL SCRAPE", e.localizedMessage ?: "Unknown Error", ""))
        }
        results
    }
}

// Fungsi download gambar tetep sama biar covernya kesimpen di HP lu
suspend fun downloadAndSaveImage(context: Context, imageUrl: String): String? {
    if (imageUrl.isEmpty()) return null
    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(imageUrl).openConnection()
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            connection.connect()
            val bitmap = BitmapFactory.decodeStream(connection.getInputStream())

            val fileName = "cover_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            imageUrl
        }
    }
}