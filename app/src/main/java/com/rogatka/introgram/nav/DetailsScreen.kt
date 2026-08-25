package com.rogatka.introgram.nav

import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.material3.SegmentedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.rogatka.introgram.Chat
import com.rogatka.introgram.addChat
import com.rogatka.introgram.randomUID
import com.rogatka.introgram.ChatAvatar
import com.rogatka.introgram.ChatTypes
import com.rogatka.introgram.Folder
import com.rogatka.introgram.deleteImageFile
import com.rogatka.introgram.getAllFolders
import com.rogatka.introgram.resizeToCover
import com.rogatka.introgram.saveBitmapToFile
import com.rogatka.introgram.singleLineChatName
import com.rogatka.introgram.topBarColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navController: NavController, folderId: Int = 0) {
    val context = LocalContext.current
    val name = rememberSaveable { mutableStateOf("") }
    val imagePath = remember { mutableStateOf("") }
    val exampleUID = randomUID()
    val coroutineScope = rememberCoroutineScope()
    var avatarLoading by remember { mutableStateOf(false) }
    var chatType by remember { mutableStateOf(ChatTypes.CLASSIC) }

    val folders: List<Folder> = getAllFolders(context)

    var expandedFolderSelect by remember { mutableStateOf(false) }
    var selectedFolder by remember {
        mutableStateOf(folders.find { it.id == folderId }) // Устанавливаем начальное значение
    }


    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            avatarLoading = true
            if (imagePath.value.isNotEmpty()) {
                coroutineScope.launch(Dispatchers.IO) {
                    deleteImageFile(context = context, filename = imagePath.value)
                }
            }
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // 2. Загрузка Bitmap с автоматическим закрытием потока
                    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)?.resizeToCover(128, 128)
                    } ?: return@launch

                    // 3. Сохранение файла
                    val imageId = randomUID()
                    val newFilename = "${imageId}.png"

                    if (saveBitmapToFile(context, bitmap, newFilename).isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            imagePath.value = newFilename
                            avatarLoading = false
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PhotoPicker", "Error processing image", e)
                }
            }
        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                colors = topBarColors(),
                title = {
                    Text(
                        "Добавить чат",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("main/${folderId}") }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (name.value.isEmpty()) return@IconButton;

                        addChat(
                            context,
                            Chat(
                                id = exampleUID,
                                name = name.value.singleLineChatName(),
                                messages = mutableListOf(),
                                imagePath = imagePath.value,
                                backgroundPath = null,
                                type = chatType,
                                folder = selectedFolder
                            )
                        )
                        navController.navigate("main/${folderId}")
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Search"
                        )
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally

        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    ChatAvatar(
                        chat = Chat(
                            id = exampleUID,
                            name = name.value,
                            messages = mutableListOf(),
                            imagePath = imagePath.value,
                            backgroundPath = null,
                            type = chatType,
                            folder = selectedFolder
                        ),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                        loading = avatarLoading,
                        size = 60.dp,

                    )
                    OutlinedTextField(
                        value = name.value,
                        singleLine = true,
                        maxLines = 1,
                        placeholder = {Text("Название чата")},
                        onValueChange = { name.value = it.singleLineChatName() }, modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp)
                            .background(Color.Transparent),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .fillMaxWidth()
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.baseShape,
                            onClick = { chatType = ChatTypes.CLASSIC },
                            selected = chatType == ChatTypes.CLASSIC,
                            label = { Text("Классический") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.baseShape,
                            onClick = { chatType = ChatTypes.TODO },
                            selected = chatType == ChatTypes.TODO,
                            label = { Text("Список дел") }
                        )
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = expandedFolderSelect,
                    onExpandedChange = { expandedFolderSelect = !expandedFolderSelect },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField( // Используем OutlinedTextField для консистентности
                        value = selectedFolder?.name ?: "Без папки", // Отображаем имя выбранной папки или "Без папки"
                        onValueChange = {}, // onValueChange здесь не нужен, т.к. выбор через меню
                        readOnly = true, // Делаем текстовое поле только для чтения
                        label = { Text("Выберите папку") },

                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFolderSelect)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expandedFolderSelect,
                        onDismissRequest = { expandedFolderSelect = false }
                    ) {
                        // Опция "Без папки"
                        DropdownMenuItem(
                            text = { Text("Без папки") },
                            onClick = {
                                selectedFolder = null // Сбрасываем выбор
                                expandedFolderSelect = false
                            }
                        )
                        // Опции для каждой папки
                        folders.forEach { folder ->
                            DropdownMenuItem(
                                text = { Text(folder.name) },
                                onClick = {
                                    selectedFolder = folder
                                    expandedFolderSelect = false
                                }
                            )
                        }
                    }
                }
            }

        }
    }
}
