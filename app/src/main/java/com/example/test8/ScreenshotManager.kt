package com.example.test8

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ScreenshotManager(private val context: Context) {
    
    companion object {
        private const val SCREENSHOT_FILENAME = "tetris_screenshot.png"
    }
    
    /**
     * Сохраняет скриншот указанного View в файл
     */
    fun saveScreenshot(view: View): Boolean {
        try {
            // Создаем битмап с размерами view
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Рисуем view на canvas
            view.draw(canvas)
            
            // Сохраняем битмап в файл
            val file = File(context.filesDir, SCREENSHOT_FILENAME)
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            return true
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
    }
    
    /**
     * Загружает сохраненный скриншот
     */
    fun loadScreenshot(): Bitmap? {
        try {
            val file = File(context.filesDir, SCREENSHOT_FILENAME)
            if (file.exists()) {
                return BitmapFactory.decodeFile(file.absolutePath)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    /**
     * Проверяет, существует ли сохраненный скриншот
     */
    fun hasScreenshot(): Boolean {
        val file = File(context.filesDir, SCREENSHOT_FILENAME)
        return file.exists()
    }
    
    /**
     * Удаляет сохраненный скриншот
     */
    fun deleteScreenshot(): Boolean {
        val file = File(context.filesDir, SCREENSHOT_FILENAME)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }
} 