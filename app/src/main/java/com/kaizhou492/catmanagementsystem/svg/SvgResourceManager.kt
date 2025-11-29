package com.kaizhou492.catmanagementsystem.svg

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.caverock.androidsvg.SVG
import java.io.InputStream

class SvgResourceManager(private val context: Context) {

    // 从 assets 加载 SVG
    private fun loadSvgFromAssets(fileName: String): SVG? {
        return try {
            val inputStream: InputStream = context.assets.open("cats/$fileName")
            SVG.getFromInputStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 将 SVG 渲染为 Bitmap，并应用颜色和状态
    fun renderCatSvg(
        skinFileName: String,
        skinColor: String?,
        eyeColor: String,
        brightness: Float = 1f,
        saturation: Float = 1f,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? {
        return try {
            // 加载皮肤 SVG
            val skinSvg = loadSvgFromAssets(skinFileName) ?: return null
            val eyeSvg = loadSvgFromAssets("eye.svg")

            // 创建 Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 设置 SVG 尺寸
            skinSvg.documentWidth = width.toFloat()
            skinSvg.documentHeight = height.toFloat()

            // 如果需要染色（default.svg 和 eye.svg）
            if (skinFileName == "default.svg" && skinColor != null) {
                // 应用皮肤颜色
                val paint = Paint()
                paint.colorFilter = createColorFilter(skinColor, brightness, saturation)
                canvas.saveLayer(null, paint)
                skinSvg.renderToCanvas(canvas)
                canvas.restore()
            } else {
                // 品种猫不需要染色，直接渲染
                val paint = Paint()
                // 只应用亮度和饱和度
                if (brightness != 1f || saturation != 1f) {
                    paint.colorFilter = createBrightnessFilter(brightness, saturation)
                }
                canvas.saveLayer(null, paint)
                skinSvg.renderToCanvas(canvas)
                canvas.restore()
            }

            // 渲染眼睛层
            if (eyeSvg != null) {
                eyeSvg.documentWidth = width.toFloat()
                eyeSvg.documentHeight = height.toFloat()

                val eyePaint = Paint()
                eyePaint.colorFilter = createColorFilter(eyeColor, brightness, saturation)
                canvas.saveLayer(null, eyePaint)
                eyeSvg.renderToCanvas(canvas)
                canvas.restore()
            }

            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 创建颜色滤镜（染色 + 亮度 + 饱和度）
    private fun createColorFilter(
        hexColor: String,
        brightness: Float,
        saturation: Float
    ): ColorFilter {
        val color = android.graphics.Color.parseColor(hexColor)
        val r = android.graphics.Color.red(color) / 255f
        val g = android.graphics.Color.green(color) / 255f
        val b = android.graphics.Color.blue(color) / 255f

        val matrix = ColorMatrix()

        // 应用颜色叠加
        val colorMatrix = ColorMatrix(floatArrayOf(
            r, 0f, 0f, 0f, 0f,
            0f, g, 0f, 0f, 0f,
            0f, 0f, b, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(colorMatrix)

        // 应用饱和度
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        matrix.postConcat(saturationMatrix)

        // 应用亮度
        val brightnessMatrix = ColorMatrix(floatArrayOf(
            brightness, 0f, 0f, 0f, 0f,
            0f, brightness, 0f, 0f, 0f,
            0f, 0f, brightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(brightnessMatrix)

        return ColorMatrixColorFilter(matrix)
    }

    // 仅应用亮度和饱和度滤镜（不染色）
    private fun createBrightnessFilter(brightness: Float, saturation: Float): ColorFilter {
        val matrix = ColorMatrix()

        // 应用饱和度
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(saturation)
        matrix.postConcat(saturationMatrix)

        // 应用亮度
        val brightnessMatrix = ColorMatrix(floatArrayOf(
            brightness, 0f, 0f, 0f, 0f,
            0f, brightness, 0f, 0f, 0f,
            0f, 0f, brightness, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))
        matrix.postConcat(brightnessMatrix)

        return ColorMatrixColorFilter(matrix)
    }
}
