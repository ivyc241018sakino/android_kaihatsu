package com.example.gametasks

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory

class GameTasksApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(AppIconMapper())
                add(AppIconFetcher.Factory(this@GameTasksApplication))
                add(AppIconKeyer())
            }
            .build()
    }
}
