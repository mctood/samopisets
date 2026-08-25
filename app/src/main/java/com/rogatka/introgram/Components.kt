package com.rogatka.introgram

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.scale
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import java.io.File


fun buildTextWithLinks(text: String): AnnotatedString {
    val regex = Regex("(https?://[\\w./?=:&%-]+)")

    return buildAnnotatedString {
        var lastIndex = 0

        regex.findAll(text).forEach { match ->
            val start = match.range.first
            val end = match.range.last + 1

            append(text.substring(lastIndex, start))

            withLink(
                LinkAnnotation.Url(
                    url = match.value,
                    styles = TextLinkStyles(
                        style = SpanStyle(
                            color = Color(0xFF64B5F6),
                            textDecoration = TextDecoration.Underline
                        )
                    )
                )
            ) {
                append(match.value)
            }

            lastIndex = end
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun TodoBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(18.dp) // Размер значка
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary), // Цвет фона значка
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Checklist,
            contentDescription = "Тип чата: Список дел",
            tint = MaterialTheme.colorScheme.onPrimary, // Цвет иконки
            modifier = Modifier.size(12.dp) // Размер иконки внутри значка
        )
    }
}


@Composable
fun ChatAvatar(
    modifier: Modifier = Modifier,
    chat: Chat,
    imagePath: String = "",
    size: Dp = 48.dp,
    loading: Boolean = false,
) {
    val context = LocalContext.current

    Box {
        Surface(
            modifier = modifier.size(size),
            shape = CircleShape,
            color = Color.Transparent,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (loading) {
                    CircularProgressIndicator()
                }
                if (imagePath.isNotEmpty()) AsyncImage(
                    model = File(context.filesDir, imagePath),
                    modifier = Modifier.size(size),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                )
                else if (chat.imagePath.isNotEmpty()) AsyncImage(
                    model = File(context.filesDir, chat.imagePath),
                    modifier = Modifier.size(size),
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                )
                else Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF202131),
                                        Color(0xFF191A21)
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(size.value, size.value)
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.name.isNotEmpty()) chat.name[0].toString().uppercase() else "",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Thin
                    )
                }
            }
        }

        if (chat.type == ChatTypes.TODO) {
            TodoBadge(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-2).dp, y = (-2).dp)
            )
        }
    }

}

@Composable
fun MessageMenu(
    initialShow: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    text: String,
    context: Context
) {
    DropdownMenu(
        expanded = initialShow,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
    ) {
        DropdownMenuItem(
            text = { Text("Копировать текст") },
            onClick = {
                onDismiss()
                context.copyToClipboard(text)
            },
            leadingIcon = {
                Icon(Icons.Default.ContentCopy, null)
            }
        )
        DropdownMenuItem(
            text = { Text("Изменить") },
            onClick = {
                onDismiss()
                onEdit()
            },
            leadingIcon = {
                Icon(Icons.Default.Edit, null)
            }
        )
        DropdownMenuItem(
            text = { Text("Удалить") },
            onClick = {
                onDismiss()
                onDelete()
            },
            leadingIcon = {
                Icon(Icons.Default.Delete, null)
            }
        )
    }
}





@Composable
fun MessageBox(
    text: String,
    time: String,
    modifier: Modifier = Modifier,
    onDelete: () -> Unit = {},
    onEdit: () -> Unit = {},
    inSearch: Boolean = false,
    onTap: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(14.dp, 14.dp, 5.dp, 14.dp), // Радиус скругления
        color = MaterialTheme.colorScheme.surfaceContainer, // Цвет фона
        modifier = modifier.padding(bottom = 8.dp).then(
            if (inSearch) Modifier.clickable(onClick = onTap)
            else Modifier.pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showMenu = true },
                    onTap = { showMenu = false }
                )
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp, 12.dp, 12.dp, 4.dp),
            horizontalAlignment = Alignment.End
        ) {
            Box {
                Text(
                    text = remember(text) {
                        buildTextWithLinks(text)
                    }
                )
            }
            Box {
                Text(text = time, fontWeight = FontWeight.Thin, fontSize = 12.sp)
            }
        }

        if (!inSearch) MessageMenu(showMenu, { showMenu = false }, onDelete, onEdit, text, context)
    }
}


@Composable
fun SystemMessageBox(text: String, modifier: Modifier = Modifier) {
    Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(percent = 50), // Радиус скругления
            color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f), // Цвет фона
            modifier = modifier.padding(bottom = 8.dp)
        ) {
            Box(Modifier.padding(12.dp, 4.dp)) {
                Text(text = text, style = MaterialTheme.typography.bodySmall)
            }
        }
    }

}


@Composable
fun ChatItem(
    chat: Chat,
    shareMode: Boolean,
    sharedText: String?,
    navController: NavController,
    folderId: Int
) {
    val context = LocalContext.current
    val allMessages = getAllChatMessages(context, chat.id)

    val lastMessage = if (allMessages.isNullOrEmpty()) {
        "Тут ничего нет!"
    } else {
        allMessages[0].content
    }
    Box(
        modifier = Modifier
            .clickable {
                if (shareMode) {
                    newMessage(
                        context = context,
                        chat = chat,
                        message = Message(
                            id = randomUID(),
                            content = sharedText!!,
                            dateTime = currentDateToString()
                        )
                    )
                    SharedContentHolder.sharedText = null
                }
                navController.navigate("chat/${chat.id}/${folderId}/0")
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChatAvatar(chat = chat, size = 52.dp)
            Column(modifier = Modifier.padding(start = 10.dp)) {
                Text(
                    chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    lastMessage,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}


@Composable
fun TodoItemBox(
    text: String,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    onTap: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val bgColor by animateColorAsState(
        targetValue = if (checked)
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surfaceContainer,
        animationSpec = tween(durationMillis = 300),
        label = "BgColorAnimation"
    )
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        shape = RoundedCornerShape(12.dp), // Радиус скругления
        color = bgColor,
        modifier = Modifier.clip(RoundedCornerShape(12.dp))
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                onClick = onTap,
                onLongClick = {
                    showMenu = true
                }
            )
            .then(modifier)
    ) {
        Column {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                val vector =
                    if (checked) Icons.Default.CheckBox
                    else Icons.Outlined.CheckBox

                Icon(
                    vector,
                    contentDescription = null,
                )
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    Text(text = text)
                }
            }

            MessageMenu(showMenu, { showMenu = false }, onDelete, onEdit, text, context)
        }

    }

}


@Composable
fun TaskStats(
    done: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        Modifier
            .padding(bottom = 16.dp)
            .clip(RoundedCornerShape(8.dp))
            .padding(16.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.66f),
        border = BorderStroke(width = 1.dp, color = MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Статистика", modifier = Modifier.padding(top = 16.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Создано",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        total.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Выполнено",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        done.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Осталось",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        (total - done).toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

}




@Composable
fun ExpandingBottomBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Surface(
        tonalElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight() // ← растягиваем по содержимому
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topBarColors(): TopAppBarColors {
    return TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.66f),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
    )
}


@Composable
fun TiledImageBackground(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Загружаем картинку через Coil в виде Painter
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(context)
            .data(R.drawable.default_bg)
            .size(Size.ORIGINAL) // Загружаем в оригинальном размере для четких плиток
            .build()
    )

    // Получаем Bitmap из состояния загруженного Painter
    val state = painter.state
    val imageBitmap = (state as? AsyncImagePainter.State.Success)?.result?.drawable?.toBitmap()

    // Создаем кисть с паттерном (повторение по X и Y)
    val tiledBrush = remember(imageBitmap) {
        imageBitmap?.let { bitmap ->
            ShaderBrush(
                ImageShader(
                    image = bitmap.asImageBitmap(),
                    tileModeX = TileMode.Repeated, // Аналог repeat-x
                    tileModeY = TileMode.Repeated  // Аналог repeat-y
                )
            )
        }
    }

    // Применяем кисть в фоновый модификатор
    Box(
        modifier = modifier
            .fillMaxSize()
            .then(
                if (tiledBrush != null) Modifier.background(tiledBrush)
                else Modifier
            )
    )
}



fun Bitmap.resizeToCover(targetWidth: Int, targetHeight: Int): Bitmap {
    // 1. Вычисляем соотношения сторон
    val sourceAspect = width.toFloat() / height.toFloat()
    val targetAspect = targetWidth.toFloat() / targetHeight.toFloat()

    // 2. Определяем масштаб (больший из двух вариантов)
    val scale = if (sourceAspect > targetAspect) {
        // Широкое изображение - масштабируем по высоте
        targetHeight.toFloat() / height.toFloat()
    } else {
        // Высокое изображение - масштабируем по ширине
        targetWidth.toFloat() / width.toFloat()
    }

    // 3. Создаем промежуточный Bitmap с масштабированием
    val scaledWidth = (width * scale).toInt()
    val scaledHeight = (height * scale).toInt()
    val scaledBitmap = this.scale(scaledWidth, scaledHeight)

    // 4. Вычисляем координаты обрезки (центрирование)
    val x = (scaledWidth - targetWidth) / 2
    val y = (scaledHeight - targetHeight) / 2

    // 5. Создаем итоговый Bitmap с обрезкой
    return Bitmap.createBitmap(
        scaledBitmap,
        x.coerceIn(0, scaledWidth - targetWidth),
        y.coerceIn(0, scaledHeight - targetHeight),
        targetWidth,
        targetHeight
    )
}

fun Context.copyToClipboard(text: String) {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Message", text)
    clipboard.setPrimaryClip(clip)
}