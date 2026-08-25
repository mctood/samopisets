package com.rogatka.introgram.nav

import android.graphics.BitmapFactory
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.rogatka.introgram.modals.ConfirmFolderDeleteModal
import com.rogatka.introgram.modals.NewFolderModal
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.rogatka.introgram.Chat
import com.rogatka.introgram.ChatItem
import com.rogatka.introgram.ChatTypes
import com.rogatka.introgram.Folder
import com.rogatka.introgram.SharedContentHolder
import com.rogatka.introgram.TaskStats
import com.rogatka.introgram.addFolder
import com.rogatka.introgram.countStats
import com.rogatka.introgram.deleteAllBgImages
import com.rogatka.introgram.deleteFolder
import com.rogatka.introgram.deleteImageFile
import com.rogatka.introgram.getAllChats
import com.rogatka.introgram.getAllFolders
import com.rogatka.introgram.getSettings
import com.rogatka.introgram.loadBitmapFromFile
import com.rogatka.introgram.modals.AboutModal
import com.rogatka.introgram.modals.ConfirmAllBackgroundsDeleteModal
import com.rogatka.introgram.moveChatToFolder
import com.rogatka.introgram.randomUID
import com.rogatka.introgram.saveBitmapToFile
import com.rogatka.introgram.setSetting
import com.rogatka.introgram.topBarColors
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File


@Composable
fun TopBarFolder(
    color: Color = Color(0x11FFFFFF),
    selected: Boolean = false,
    onClick: (() -> Unit),
    content: @Composable (RowScope.() -> Unit)
) {
    val isDark = isSystemInDarkTheme()
    val borderColorStatic = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface;
    val borderColor by animateColorAsState(
        targetValue = if (selected) borderColorStatic else Color.Transparent, // Прозрачный, когда не выбрано
        animationSpec = tween(durationMillis = 300), // Длительность анимации в мс
        label = "BorderColorAnimation"
    )

    Surface(
        modifier = Modifier
            .padding(8.dp, 8.dp, 0.dp, 8.dp)
            .clip(RoundedCornerShape(percent = 50))
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(percent = 50)
            ),
        shape = RoundedCornerShape(percent = 50),
        color = color,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, folder: Int = 0) {
    val sharedText = SharedContentHolder.sharedText
    var shareMode by remember { mutableStateOf(sharedText != null) }
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val folders: List<Folder> = remember { getAllFolders(context) }
    val foldersListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val settings = getSettings(context)


    var showAllChatsFolder by remember {mutableStateOf(getSettings(context).showAllFolders)}

    fun scrollToFolder(folderIndex: Int) {
        coroutineScope.launch {
            val layoutInfo = foldersListState.layoutInfo
            val viewportWidth = layoutInfo.viewportEndOffset

            val itemWidth = 120

            val offset = viewportWidth / 2 - itemWidth / 2

            foldersListState.animateScrollToItem(folderIndex, -offset)
        }
    }

    val allFolderIds = remember {
        if (showAllChatsFolder)
            (listOf(-1, 0) + folders.map { it.id }).toMutableStateList()
        else
            (listOf(-1) + folders.map { it.id }).toMutableStateList()
    }

    var folderIndex by rememberSaveable {
        mutableIntStateOf(
            allFolderIds.indexOf(
                if (showAllChatsFolder) folder
                else {
                    if (folder == 0) {
                        if (folders.isNotEmpty()) {
                            folders[0].id
                        }
                        else -1
                    }
                    else folder
                }
            )
        )
    }

    val backgroundPath = remember { mutableStateOf(settings.mainBackgroundPath ?: "") }
    var bgLoading by remember { mutableStateOf(false) }
    val hazeState = rememberHazeState()

    val pickBackground =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                bgLoading = true
                if (backgroundPath.value.isNotEmpty()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        deleteImageFile(context = context, filename = backgroundPath.value)
                    }
                }
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        // 2. Загрузка Bitmap с автоматическим закрытием потока
                        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        } ?: return@launch

                        // 3. Сохранение файла
                        val imageId = randomUID()
                        val newFilename = "${imageId}.bg.png"

                        if (saveBitmapToFile(context, bitmap, newFilename).isNotEmpty()) {
                            withContext(Dispatchers.Main) {
                                backgroundPath.value = newFilename
                                bgLoading = false
                                setSetting(context) { it.apply { this.mainBackgroundPath = newFilename } }
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

    var folderId by rememberSaveable { mutableIntStateOf(allFolderIds[folderIndex]) }

    val chats: List<Chat> = remember(folderId) {
        if (folderId == -1) {
            getAllChats(context).filter { it.type == ChatTypes.TODO }
        }
        else getAllChats(context, folderId)
    }

    var expanded by remember { mutableStateOf(false) }



    /** MODALS **/

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showDeleteFolderDialog by remember { mutableStateOf(false) }
    var showAboutModal by remember { mutableStateOf(false) }
    var showAllBackgroundsDeleteModal by remember { mutableStateOf(false) }


    ConfirmAllBackgroundsDeleteModal(
        show = showAllBackgroundsDeleteModal,
        onDismiss = { showAllBackgroundsDeleteModal = false },
        onConfirm = {
            coroutineScope.launch(Dispatchers.IO) {
                deleteAllBgImages(context = context)
            }
            showAllBackgroundsDeleteModal = false
        })

    ConfirmFolderDeleteModal(
        show = showDeleteFolderDialog,
        onDismiss = { showDeleteFolderDialog = false },
        onConfirm = {
            deleteFolder(context, folderId)
            for (chat in getAllChats(context, folderId)) {
                moveChatToFolder(context, chat, null)
            }
            showDeleteFolderDialog = false
            navController.navigate("main/0")
        })

    NewFolderModal (
        show = showNewFolderDialog,
        onDismiss = { showNewFolderDialog = false },
        onConfirm = { name, icon ->
            val newFolder = Folder(
                id = randomUID(),
                name = name,
                icon = icon
            )
            addFolder(context, newFolder)
            showNewFolderDialog = false
            navController.navigate("main/${newFolder.id}")
        })

    AboutModal(
        show = showAboutModal,
        onConfirm = { showAboutModal = false }
    )

    /** END MODALS **/

    var totalDragX = 0f
    Scaffold(
        modifier = Modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (totalDragX > 100) {

                            if (folderIndex > 0) {
                                folderIndex--
                            }
                        } else if (totalDragX < -100) {

                            if (folderIndex < allFolderIds.lastIndex) {
                                folderIndex++
                            }
                        }

                        folderId = allFolderIds[folderIndex]
                        totalDragX = 0f

                        scrollToFolder(folderIndex)
                    },
                    onDrag = { change, dragAmount ->
                        totalDragX += dragAmount.x
                        change.consume()
                    }
                )
        },
        floatingActionButton = {
            Column(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .hazeEffect(hazeState)
                    .background(MaterialTheme.colorScheme.surface.copy(0.7f))
                    .clickable(onClick = {
                        navController.navigate("details/${folderId}")
                    }),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally

            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        },
        topBar = {}
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(state = hazeState)
        ) {
            if (backgroundPath.value.isNotEmpty()) {
                AsyncImage(
                    model = File(context.filesDir, backgroundPath.value),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0x55000000))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .hazeEffect(state = hazeState)
            ) {
                CenterAlignedTopAppBar(
                    colors = topBarColors(),
                    title = {
                        Text(
                            if (shareMode) "Куда переслать?" else "Самописец",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("Создать папку") }, leadingIcon = {
                                Icon(
                                    Icons.Default.Folder, contentDescription = "Создать папку"
                                )
                            }, onClick = {
                                expanded = false
                                showNewFolderDialog = true
                            })
                            if (folderId > 0)
                                DropdownMenuItem(text = { Text("Удалить папку") }, leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete, contentDescription = "Удалить папку"
                                    )
                                }, onClick = {
                                    expanded = false
                                    showDeleteFolderDialog = true
                                })

                            DropdownMenuItem(text = { Text(if (showAllChatsFolder) "Скрыть Все чаты" else "Показать Все чаты") }, leadingIcon = {
                                Icon(
                                    Icons.Default.RemoveRedEye, contentDescription = "Переключатель"
                                )
                            }, onClick = {
                                expanded = false
                                showAllChatsFolder = !showAllChatsFolder
                                setSetting(context) { it.apply { this.showAllFolders = showAllChatsFolder } }
                                if (!showAllChatsFolder) {
                                    allFolderIds.remove(0)

                                    if (folderId == 0) {
                                        if (folders.isNotEmpty()) {
                                            folderId = folders[0].id
                                            folderIndex = allFolderIds.indexOf(folderId)
                                        } else {
                                            folderId = -1
                                            folderIndex = 0
                                        }
                                    }
                                } else {
                                    if (!allFolderIds.contains(0)) {
                                        allFolderIds.add(1, 0)
                                    }

                                }
                                folderIndex = allFolderIds.indexOf(folderId)
                            })

                            DropdownMenuItem(text = { Text(if (backgroundPath.value.isEmpty()) "Установить фон" else "Изменить фон") }, leadingIcon = {
                                Icon(
                                    Icons.Default.LocalFlorist, contentDescription = "Фон"
                                )
                            }, onClick = {
                                expanded = false
                                pickBackground.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            })


                            if (backgroundPath.value.isNotEmpty())
                                DropdownMenuItem(text = { Text("Убрать фон") }, leadingIcon = {
                                    Icon(
                                        Icons.Default.Close, contentDescription = "Фон"
                                    )
                                }, onClick = {
                                    expanded = false

                                    coroutineScope.launch(Dispatchers.IO) {
                                        val oldPath = backgroundPath.value

                                        backgroundPath.value = ""

                                        deleteImageFile(context, oldPath)
                                    }
                                    setSetting(context) {it.apply { this.mainBackgroundPath = null }}
                                })

                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Удалить все фоны") }, leadingIcon = {
                                Icon(
                                    Icons.Default.ImageNotSupported, contentDescription = "Удалить все фоны"
                                )
                            }, onClick = {
                                expanded = false
                                showAllBackgroundsDeleteModal = true
                            })
                            DropdownMenuItem(text = { Text("О программе") }, leadingIcon = {
                                Icon(
                                    Icons.Default.Info, contentDescription = "О программе"
                                )
                            }, onClick = {
                                expanded = false
                                showAboutModal = true
                            })
                        }
                    },
                    actions = {
                        IconButton(onClick = { navController.navigate("search/${folderId}") }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
                LazyRow(
                    state = foldersListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(topBarColors().containerColor)
                ) {
                    item {
                        TopBarFolder(selected = folderId ==  -1, color = Color(0x22FFFFFF), onClick = {
                            folderId = -1
                            folderIndex = allFolderIds.indexOf(folderId)
                            scrollToFolder(folderIndex)
                        }) {
                            Icon(Icons.Filled.Checklist, contentDescription = "1", modifier = Modifier.size(24.dp))
                        }
                    }

                    item {
                        if (showAllChatsFolder) TopBarFolder(selected = folderId == 0, onClick = {
                            folderId = 0
                            folderIndex = allFolderIds.indexOf(folderId)
                            scrollToFolder(folderIndex)
                        }) {
                            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "1", modifier = Modifier.size(16.dp))
                            Text("Все чаты", modifier = Modifier.padding(start = 12.dp))
                        }
                    }

                    items(folders) { folder ->
                        TopBarFolder(selected = folderId == folder.id, onClick = {
                            folderId = folder.id
                            folderIndex = allFolderIds.indexOf(folder.id)
                            scrollToFolder(folderIndex)
                        }) {
                            Icon(folder.icon.icon, contentDescription = "Folder Icon", modifier = Modifier.size(16.dp))
                            Text(folder.name, modifier = Modifier.padding(start = 12.dp))
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(1.dp)
                                .padding(end = 8.dp)
                        )
                    }
                }
            }



            if (chats.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().fillMaxHeight().weight(1f),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Тут ничего нет \uD83D\uDC38",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Text(
                        "Добавьте чаты, используя кнопку ниже.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        "Для добавления папки кликните на меню сверху",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            else LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp),
            ) {
                item {
                    if (folderId == -1) {
                        val stats = countStats(context)
                        TaskStats(stats.done, stats.total, hazeState)
                    }
                }

                items(chats) { chat ->
                    ChatItem(
                        chat = chat,
                        shareMode = shareMode,
                        sharedText = sharedText,
                        navController = navController,
                        folderId = folderId
                    )
                }
            }
        }






    }
}
