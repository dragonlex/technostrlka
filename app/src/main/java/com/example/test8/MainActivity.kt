package com.example.test8

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView
import android.widget.RadioGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

class MainActivity : AppCompatActivity() {
    private lateinit var taskManager: TaskManager
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var screenshotManager: ScreenshotManager
    private var isScreenshotMode = false
    private var originalBackground: Drawable? = null
    private var originalAlpha: Float = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        taskManager = TaskManager(this)
        screenshotManager = ScreenshotManager(this)
        setupRecyclerView()
        var md = MediaPlayer.create(this,R.raw.music)
        md.isLooping = true
        md.start()
        // Проверяем, нужно ли показать скриншот
        if (intent.getBooleanExtra("showScreenshot", false)) {
            showScreenshotBackground()
        }

        // Setup mode selection cards
        findViewById<View>(R.id.dayCard).setOnClickListener {
            startGameActivity("day")
        }

        findViewById<View>(R.id.weekCard).setOnClickListener {
            startGameActivity("week")
        }

        findViewById<View>(R.id.monthCard).setOnClickListener {
            startGameActivity("month")
        }

        // Setup FAB
        findViewById<FloatingActionButton>(R.id.addTaskButton).setOnClickListener {
            showAddTaskDialog()
        }
    }

    /**
     * Показывает скриншот в качестве фона и делает элементы невидимыми
     */
    private fun showScreenshotBackground() {
        isScreenshotMode = true
        
        // Сохраняем оригинальный фон
        originalBackground = window.decorView.background
        
        // Загружаем скриншот
        val screenshot = screenshotManager.loadScreenshot()
        if (screenshot != null) {
            // Создаем белый фон
            val whiteBackground = ColorDrawable(Color.WHITE)
            
            // Создаем фон из скриншота
            val backgroundDrawable = BitmapDrawable(resources, screenshot)
            
            // Создаем композитный фон с белым фоном и скриншотом поверх
            val layers = arrayOf<Drawable>(whiteBackground, backgroundDrawable)
            val layerDrawable = LayerDrawable(layers)
            
            // Устанавливаем фон
            window.decorView.background = layerDrawable
            
            // Делаем все элементы невидимыми, кроме кнопки настроек
            setViewsAlpha(0f)
            
            // Настраиваем кнопку настроек для возврата к нормальному виду
            val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
            settingsButton.alpha = 1.0f
            
            // Увеличиваем размер кнопки
            settingsButton.layoutParams.width = 120
            settingsButton.layoutParams.height = 120
            
            // Добавляем фон для кнопки
            settingsButton.setBackgroundColor(Color.argb(128, 255, 255, 255))
            
            // Перемещаем кнопку настроек в корневой контейнер, чтобы она была поверх всего
            val rootView = findViewById<ConstraintLayout>(R.id.main)
            val parent = settingsButton.parent as? ViewGroup
            parent?.removeView(settingsButton)
            
            // Создаем параметры для размещения кнопки в корневом контейнере
            val params = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            
            // Устанавливаем отступы
            params.setMargins(16, 16, 0, 0)
            
            // Добавляем кнопку в корневой контейнер
            rootView.addView(settingsButton, params)
            
            // Устанавливаем ограничения для позиционирования кнопки
            val constraintSet = ConstraintSet()
            constraintSet.clone(rootView)
            constraintSet.connect(settingsButton.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 16)
            constraintSet.connect(settingsButton.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
            constraintSet.applyTo(rootView)
            
            // Делаем кнопку видимой
            settingsButton.visibility = View.VISIBLE
            
            // Устанавливаем обработчик нажатия
            settingsButton.setOnClickListener {
                restoreNormalView()
            }
        }
    }
    
    /**
     * Устанавливает прозрачность для всех элементов
     */
    private fun setViewsAlpha(alpha: Float) {
        originalAlpha = alpha
        val rootView = findViewById<ViewGroup>(R.id.main)
        setViewGroupAlpha(rootView, alpha)
    }
    
    /**
     * Рекурсивно устанавливает прозрачность для ViewGroup и его дочерних элементов
     */
    private fun setViewGroupAlpha(viewGroup: ViewGroup, alpha: Float) {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            
            // Пропускаем кнопку настроек
            if (child.id == R.id.settingsButton) {
                continue
            }
            
            // Устанавливаем прозрачность
            child.alpha = alpha
            
            // Если это ViewGroup, рекурсивно обрабатываем его дочерние элементы
            if (child is ViewGroup) {
                setViewGroupAlpha(child, alpha)
            }
        }
    }
    
    /**
     * Восстанавливает нормальный вид
     */
    private fun restoreNormalView() {
        isScreenshotMode = false
        
        // Восстанавливаем оригинальный фон
        window.decorView.background = originalBackground
        
        // Восстанавливаем видимость элементов
        val rootView = findViewById<ViewGroup>(R.id.main)
        setViewGroupAlpha(rootView, 1.0f)
        
        // Возвращаем кнопку настроек на исходное место
        val settingsButton = findViewById<ImageButton>(R.id.settingsButton)
        val parent = settingsButton.parent as? ViewGroup
        parent?.removeView(settingsButton)
        
        // Находим оригинальный контейнер для кнопки настроек
        val originalContainer = findViewById<ViewGroup>(R.id.toolbar)
        originalContainer.addView(settingsButton)
        
        // Делаем кнопку снова невидимой
        settingsButton.visibility = View.INVISIBLE
        
        // Удаляем скриншот
        screenshotManager.deleteScreenshot()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.tasksRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this).apply {
            orientation = LinearLayoutManager.VERTICAL
        }
        
        taskAdapter = TaskAdapter(taskManager.loadTasks().toMutableList()) { task ->
            // Обработчик удаления задачи
            val tasks = taskManager.loadTasks().toMutableList()
            tasks.remove(task)
            taskManager.saveTasks(tasks)
        }
        
        recyclerView.adapter = taskAdapter
        updateTasksList() // Загружаем начальный список задач
    }

    private fun updateTasksList() {
        val tasks = taskManager.loadTasks()
        taskAdapter.updateTasks(tasks)
    }

    private fun startGameActivity(mode: String) {
        val intent = Intent(this, GameActivity::class.java).apply {
            putExtra("mode", mode)
        }
        startActivity(intent)
    }

    private fun showAddTaskDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        val durationSeekBar = dialogView.findViewById<SeekBar>(R.id.durationSeekBar)
        val durationText = dialogView.findViewById<TextView>(R.id.durationText)
        val taskNameInput = dialogView.findViewById<TextInputEditText>(R.id.taskNameInput)
        val taskDescriptionInput = dialogView.findViewById<TextInputEditText>(R.id.taskDescriptionInput)
        
        durationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val minutes = progress + 15
                durationText.text = "$minutes минут"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        dialogView.findViewById<View>(R.id.cancelButton).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.addButton).setOnClickListener {
            val taskName = taskNameInput.text.toString()
            val taskDescription = taskDescriptionInput.text.toString()
            
            if (taskName.isBlank()) {
                taskNameInput.error = "Введите название задачи"
                return@setOnClickListener
            }

            // Get selected category
            val categoryGroup = dialogView.findViewById<RadioGroup>(R.id.categoryGroup)
            val category = when (categoryGroup.checkedRadioButtonId) {
                R.id.workCategory -> "Работа"
                R.id.studyCategory -> "Учеба"
                R.id.personalCategory -> "Личное"
                else -> {
                    Toast.makeText(this, "Выберите категорию", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Get duration
            val duration = durationSeekBar.progress + 15

            // Get importance
            val importanceGroup = dialogView.findViewById<RadioGroup>(R.id.importanceGroup)
            val importance = when (importanceGroup.checkedRadioButtonId) {
                R.id.lowImportance -> "Низкая"
                R.id.mediumImportance -> "Средняя"
                R.id.highImportance -> "Высокая"
                else -> {
                    Toast.makeText(this, "Выберите важность", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Создаем и сохраняем новую задачу
            val task = Task(taskName, taskDescription, category, duration, importance)
            taskManager.addTask(task)

            // Обновляем список задач
            updateTasksList()

            // Показываем уведомление об успехе и закрываем диалог
            Toast.makeText(this, "Задача добавлена", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
    
    override fun onResume() {
        super.onResume()
        updateTasksList()
    }
}