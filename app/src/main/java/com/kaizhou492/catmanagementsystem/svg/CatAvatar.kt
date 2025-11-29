package com.kaizhou492.catmanagementsystem.svg

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kaizhou492.catmanagementsystem.models.Cat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CatAvatar(
    cat: Cat,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(cat.id, cat.brightness, cat.saturation) {
        isLoading = true
        bitmap = withContext(Dispatchers.IO) {
            val svgManager = SvgResourceManager(context)
            val fileName = when (cat.breed) {
                "橘猫" -> "1.svg"
                "布偶猫" -> "2.svg"
                "暹罗猫" -> "3.svg"
                "蓝猫" -> "4.svg"
                "三花猫" -> "5.svg"
                "无毛猫" -> "6.svg"
                "奶牛猫" -> "7.svg"
                "狸花猫" -> "8.svg"
                "缅因猫" -> "9.svg"
                else -> "default.svg"
            }

            // 只有 default.svg 需要染色
            val skinColor = if (fileName == "default.svg") cat.skinColor else null

            svgManager.renderCatSvg(
                skinFileName = fileName,
                skinColor = skinColor,
                eyeColor = cat.eyeColor,
                brightness = cat.brightness,
                saturation = cat.saturation,
                width = (size.value * 2).toInt(), // 2x for better quality
                height = (size.value * 2).toInt()
            )
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size / 2)
            )
        } else {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = cat.name,
                    modifier = Modifier.size(size)
                )
            }
        }
    }
}
