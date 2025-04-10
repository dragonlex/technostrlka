package com.example.test8

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TaskManager(context: Context) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveTasks(tasks: List<Task>) {
        val json = gson.toJson(tasks)
        sharedPreferences.edit().putString("tasks", json).apply()
    }

    fun loadTasks(): List<Task> {
        val json = sharedPreferences.getString("tasks", null) ?: return emptyList()
        val type = object : TypeToken<List<Task>>() {}.type
        return gson.fromJson(json, type)
    }

    fun addTask(task: Task) {
        val tasks = loadTasks().toMutableList()
        tasks.add(task)
        saveTasks(tasks)
    }

    fun clearTasks() {
        saveTasks(emptyList())
    }
} 