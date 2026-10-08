package com.example.gametasks

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.Fetcher
import coil.key.Keyer
import coil.map.Mapper
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * アプリのアイコンを安全に取得するためのカスタムFetcher。
 * Adaptive Iconによる描画クラッシュを避けるため、ソフトウェアBitmapへ一度変換します。
 */
class AppIconFetcher(
    private val context: Context,
    private val packageName: String
) : Fetcher {
    override suspend fun fetch(): DrawableResult {
        return withContext(Dispatchers.IO) {
            val pm = context.packageManager
            val drawable: Drawable = try {
                pm.getApplicationIcon(packageName.trim())
            } catch (e: Exception) {
                Log.e("AppIconFetcher", "Icon not found for $packageName")
                pm.defaultActivityIcon
            }

            // どんな形式のDrawableも安全な192pxのBitmapに変換
            val bitmap = try {
                val size = 192 // 高解像度かつ安定したサイズ
                val b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(b)
                drawable.setBounds(0, 0, size, size)
                drawable.draw(canvas)
                b
            } catch (e: Exception) {
                // 万が一の失敗時は透明な1x1のBitmapを返してクラッシュを防ぐ
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }

            DrawableResult(
                drawable = BitmapDrawable(context.resources, bitmap),
                isSampled = false,
                dataSource = DataSource.DISK
            )
        }
    }

    class Factory(private val context: Context) : Fetcher.Factory<String> {
        override fun create(data: String, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (!data.startsWith("package:")) return null
            val pkg = data.substringAfter("package:").trim()
            if (pkg.isBlank()) return null
            return AppIconFetcher(context, pkg)
        }
    }

    companion object {
        fun getPackageName(data: String): String? {
            val trimmed = data.trim()
            if (trimmed.isEmpty() || trimmed.contains("/") || trimmed.contains(" ")) return null
            if (trimmed.startsWith("package:")) return trimmed.substringAfter("package:").trim()
            return if (trimmed.contains(".") && !trimmed.contains(":")) trimmed else null
        }
    }
}

class AppIconMapper : Mapper<String, String> {
    override fun map(data: String, options: Options): String? {
        if (data.startsWith("package:")) return null
        val pkg = AppIconFetcher.getPackageName(data)
        return if (pkg != null) "package:$pkg" else null
    }
}

class AppIconKeyer : Keyer<String> {
    override fun key(data: String, options: Options): String? {
        if (data.startsWith("package:")) return data
        val pkg = AppIconFetcher.getPackageName(data)
        return if (pkg != null) "package:$pkg" else null
    }
}
