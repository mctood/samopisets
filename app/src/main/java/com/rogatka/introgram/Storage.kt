package com.rogatka.introgram

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.SportsVolleyball
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import coil.imageLoader
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

val gson = Gson()
val emptyChats: String = gson.toJson(Chats(chats = mutableListOf()))
val emptyFolders: String = gson.toJson(Folders(folders = mutableListOf()))

val emptySettings: String = gson.toJson(Settings())
fun randomUID(): Int {
    return (100000000..999999999).random()
}

val gradients = listOf(
    listOf(Color(0xFFFFC871), Color(0xFFFF803B)),
    listOf(Color(0xFF71FF7F), Color(0xFF1BAE24)),
    listOf(Color(0xFF9ED7FF), Color(0xFF2344C8)),
)

fun getGradientByUID(uid: Int): List<Color> {
    return gradients[uid % gradients.size]
}

suspend fun saveBitmapToFile(
    context: Context,
    bitmap: Bitmap,
    filename: String
): String = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, filename)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
    file.absolutePath
}

val MONTHS = listOf(
    "января",
    "февраля",
    "марта",
    "апреля",
    "мая",
    "июня",
    "июля",
    "августа",
    "сентября",
    "октября",
    "ноября",
    "декабря",
)


const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"

fun currentDateToString(): String {
    val currentDateTime = LocalDateTime.now()
    val formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN)

    return currentDateTime.format(formatter)
}


fun stringToDate(string: String): Map<String, Int>? {
    val format = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())

    try {
        val date: Date = format.parse(string)!!

        val calendar = Calendar.getInstance().apply { time = date }
        return mapOf(
            "year" to calendar.get(Calendar.YEAR),
            "month" to calendar.get(Calendar.MONTH),
            "day" to calendar.get(Calendar.DAY_OF_MONTH),
            "hours" to calendar.get(Calendar.HOUR_OF_DAY),
            "minutes" to calendar.get(Calendar.MINUTE),
        )

    } catch(_: ParseException) {
        return null
    }
}

fun datesDifferent(m1: Message, m2: Message): Boolean {
    val m1Date = stringToDate(m1.dateTime)
    val m2Date = stringToDate(m2.dateTime)

    if (m1Date.isNullOrEmpty() || m2Date.isNullOrEmpty()) return true

    return m1Date["day"] != m2Date["day"] || m1Date["month"] != m2Date["month"] || m1Date["year"] != m2Date["year"]
}



fun loadBitmapFromFile(context: Context, filename: String?): Bitmap? {
    if (filename.isNullOrEmpty()) return null
    val filepath = File(context.filesDir, filename).path
    return try {
        BitmapFactory.decodeFile(filepath)
    } catch (_: Exception) {
        null
    }
}

fun deleteImageFile(
    context: Context,
    filename: String?
): Boolean {
    if (filename.isNullOrEmpty()) return false

    return try {
        val file = File(context.filesDir, filename)

        context.imageLoader.memoryCache?.clear()

        val deleted = file.delete()

        Log.d("DeleteImage", "delete ${file.absolutePath}: $deleted")

        deleted
    } catch (e: Exception) {
        Log.e("DeleteImage", "err")
        e.printStackTrace()
        false
    }
}

fun deleteAllBgImages(context: Context) {
    context.imageLoader.memoryCache?.clear()

    context.filesDir.listFiles()
        ?.filter { it.isFile && it.name.endsWith(".bg.png") }
        ?.forEach { file ->
            Log.d("DeleteImage", "delete ${file.absolutePath}: ${file.delete()}")
        }

    getAllChats(context)
        .forEach { chat ->
            changeChatBackground(context, chat, "")
        }
}


data class Message(
    val id: Int,
    var content: String,
    val dateTime: String,
    var done: Boolean = false,
    val isSystem: Boolean = false,
) {
    fun time(): String {
        return this.dateTime
            .substringAfter(" ")
            .substringBeforeLast(":")
    }
}

enum class ChatTypes() {
    CLASSIC,
    TODO
}


enum class FolderIcon(val icon: ImageVector) {
    STAR(Icons.Default.Star),
    FOLDER(Icons.Default.Folder),
    FLOWER(Icons.Default.LocalFlorist),
    BALL(Icons.Default.SportsVolleyball),
    WORK(Icons.Default.Work),
    HEART(Icons.Default.Favorite),
    FACE(Icons.Default.Face),
    CODE(Icons.Default.Code);
}


data class Chat(
    val type: ChatTypes? = ChatTypes.CLASSIC,
    var imagePath: String,
    val messages: MutableList<Message>,
    var name: String,
    val id: Int,
    var backgroundPath: String?,
    var folder: Folder? = null
)

data class Chats(
    val chats: MutableList<Chat>
)

data class Folder(
    val id: Int,
    val name: String,
    val icon: FolderIcon
)

data class Folders(
    val folders: MutableList<Folder>
)




fun writeJsonToFile(context: Context, filename: String, jsonData: String) {
    try {
        context.openFileOutput(filename, Context.MODE_PRIVATE).use { stream ->
            stream.write(jsonData.toByteArray())
        }
    } catch (e: IOException) {
        throw IOException("Не удалось записать файл: ${e.message}")
    }
}

fun readJsonFromFile(context: Context, filename: String): String? {
    return try {
        context.openFileInput(filename).bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getAllChatMessages(context: Context, chatId: Int): MutableList<Message>? {
    val chat = getChatByID(context, chatId)
    return chat?.messages
}

fun dateMessage(baseMessage: Message): Message {
    val date = stringToDate(baseMessage.dateTime)!!
    val dateDifferentText = "${date["day"]} ${MONTHS[date["month"]!!]}"

    return Message(
        id = randomUID(),
        content = dateDifferentText,
        dateTime = baseMessage.dateTime,
        isSystem = true,
    )
}


fun newMessage(context: Context, message: Message, chat: Chat?) {
    if (chat == null) return
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val i = chats.chats.indexOfFirst { it.id == chat.id }
    if (chats.chats[i].messages.isEmpty() || datesDifferent(chats.chats[i].messages[0], message)) {
        chats.chats[i].messages.add(0, dateMessage(message))
    }
    chats.chats[i].messages.add(0, message)
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun renameChat(context: Context, name: String, chat: Chat?) {
    if (chat == null) return
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    chats.chats[chatIndex].name = name
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun changeChatPhoto(context: Context, chat: Chat?, imagePath: String) {
    if (chat == null) return
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    deleteImageFile(context, chats.chats[chatIndex].imagePath)
    chats.chats[chatIndex].imagePath = imagePath
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun changeChatBackground(context: Context, chat: Chat?, backgroundPath: String) {
    if (chat == null) return
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    deleteImageFile(context, chats.chats[chatIndex].backgroundPath)
    chats.chats[chatIndex].backgroundPath = backgroundPath
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun deleteChat(context: Context, chat: Chat) {
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    chats.chats.removeAt(chatIndex)
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun getAllChats(context: Context, folderId: Int = 0): List<Chat> {
    val chats: Chats = gson.fromJson(readJsonFromFile(context, "chats.json") ?: emptyChats, Chats::class.java)
    if (folderId != 0) {
        return chats.chats.filter { it.folder?.id == folderId }
    }
    return chats.chats
}

fun getChatByID(context: Context, id: Int): Chat? {
    val json = readJsonFromFile(context, "chats.json")
    val chats: Chats = gson.fromJson(json ?: emptyChats, Chats::class.java)
    return chats.chats.find { it.id == id }
}


fun addChat(context: Context, chat: Chat) {
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    chats.chats.add(chat)
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}


fun getAllFolders(context: Context): List<Folder> {
    val folders: Folders = gson.fromJson(readJsonFromFile(context, "folders.json") ?: emptyFolders,
        Folders::class.java)
    return folders.folders
}


fun addFolder(context: Context, folder: Folder) {
    val folders: Folders = gson.fromJson(
        readJsonFromFile(context, "folders.json") ?: emptyFolders,
        Folders::class.java
    )
    folders.folders.add(folder)
    writeJsonToFile(context, "folders.json", gson.toJson(folders))
}

fun deleteFolder(context: Context, folderId: Int) {
    val folders: Folders = gson.fromJson(
        readJsonFromFile(context, "folders.json") ?: emptyFolders,
        Folders::class.java
    )
    val folderIndex = folders.folders.indexOfFirst { it.id == folderId }
    folders.folders.removeAt(folderIndex)

    writeJsonToFile(context, "folders.json", gson.toJson(folders))
}

fun moveChatToFolder(context: Context, chat: Chat?, folder: Folder?) {
    if (chat == null) return
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    chats.chats[chatIndex].folder = folder
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun checkMessageInTodoChat(context: Context, chat: Chat?, message: Message?, state: Boolean) {
    if (chat == null) return
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    val messageIndex = chats.chats[chatIndex].messages.indexOfFirst { it.id == message?.id }
    chats.chats[chatIndex].messages[messageIndex].done = state

    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

fun removeMessage(context: Context, chat: Chat, message: Message) {
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    val messageIndex = chats.chats[chatIndex].messages.indexOfFirst { it.id == message.id }

    chats.chats[chatIndex].messages.removeAt(messageIndex)
    if (chats.chats[chatIndex].messages.first().isSystem) {
        chats.chats[chatIndex].messages.removeAt(0)
    }

    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}


fun editMessage(context: Context, chat: Chat, message: Message, text: String) {
    val chats: Chats = gson.fromJson(
        readJsonFromFile(context, "chats.json") ?: emptyChats,
        Chats::class.java
    )
    val chatIndex = chats.chats.indexOfFirst { it.id == chat.id }
    val messageIndex = chats.chats[chatIndex].messages.indexOfFirst { it.id == message.id }

    chats.chats[chatIndex].messages[messageIndex].content = text
    writeJsonToFile(context, "chats.json", gson.toJson(chats))
}

data class Stats(
    val total: Int,
    val done: Int,
)
fun countStats(context: Context, specialChat: Chat? = null): Stats {
    var total = 0
    var done = 0

    if (specialChat != null) {
        val totalMessages = specialChat.messages.filter {!it.isSystem}
        total += totalMessages.size
        done += totalMessages.filter { it.done }.size
    }
    else {
        val chats = getAllChats(context)
        chats.filter { it.type == ChatTypes.TODO }.forEach { chat ->
            val totalMessages = chat.messages.filter {!it.isSystem}
            total += totalMessages.size
            done += totalMessages.filter { it.done }.size
        }
    }

    return Stats(
        total = total,
        done = done
    )
}

data class Settings(
    var showAllFolders: Boolean = true,
    var mainBackgroundPath: String? = null
)

fun getSettings(context: Context): Settings {
    val settings: Settings = gson.fromJson(
        readJsonFromFile(context, "settings.json") ?: emptySettings,
        Settings::class.java
    )
    return settings
}

fun setSetting(context: Context, lambda: (settings: Settings) -> Settings) {
    val settings = lambda(getSettings(context))
    writeJsonToFile(context, "settings.json", gson.toJson(settings))
}