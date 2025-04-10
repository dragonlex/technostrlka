package com.example.test8

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import  android.media.MediaPlayer
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.Color

class GameActivity : AppCompatActivity() {
    private lateinit var tetrisView: TetrisView
    private lateinit var taskManager: TaskManager
    private lateinit var dateText: TextView
    private lateinit var scaleSpinner: Spinner
    private lateinit var pomodoroButton: Button
    private lateinit var startButton: ImageButton
    private var isGameStarted = false
    private var isPomodoroRunning = false
    private var workTimeRemaining = 25 * 60 // 25 minutes in seconds
    private var breakTimeRemaining = 5 * 60 // 5 minutes in seconds
    private var isWorkTime = true
    private var pomodoroTimer: Timer? = null
    private val tasks = mutableListOf<String>()
    private lateinit var linesCounterText: TextView
    private lateinit var screenshotManager: ScreenshotManager
    private var gameMode: String = "day" // По умолчанию режим "день"
    private lateinit var freezeButton: ImageButton

    // Добавим переменные для хранения обработчиков повторения
    private var leftHandler = Handler(Looper.getMainLooper())
    private var rightHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        // Получаем режим из Intent
        var md = MediaPlayer.create(this,R.raw.music)
        md.isLooping = true
        md.start()
        gameMode = intent.getStringExtra("mode") ?: "day"
        
        // Инициализируем TaskManager и ScreenshotManager
        taskManager = TaskManager(this)
        screenshotManager = ScreenshotManager(this)

        // Initialize views
        tetrisView = findViewById(R.id.tetrisView)
        dateText = findViewById(R.id.dateText)
        scaleSpinner = findViewById(R.id.scaleSpinner)
        pomodoroButton = findViewById(R.id.pomodoroButton)
        startButton = findViewById(R.id.startButton)
        
        // Устанавливаем начальную иконку play
        startButton.setImageResource(android.R.drawable.ic_media_play)

        // Настраиваем размер поля тетриса в зависимости от режима
        setupTetrisViewForMode()

        // Setup date display
        updateDateDisplay()

        // Setup scale spinner
        scaleSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // TODO: Implement scale change
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        linesCounterText = findViewById(R.id.linesCounterText)

        // Установим слушатель для обновления счетчика рядов
        tetrisView.setOnLinesCompletedListener { lines ->
            linesCounterText.text = "Собрано рядов: $lines"
        }

        // Устанавливаем слушатель завершения игры
        tetrisView.setOnGameCompletedListener {
            // Создаем скриншот игровой зоны
            val screenshot = tetrisView.createGameScreenshot()
            if (screenshot != null) {
                // Сохраняем скриншот
                screenshotManager.saveScreenshot(tetrisView)
                
                // Показываем сообщение о завершении игры
                Toast.makeText(this, "Игра завершена!", Toast.LENGTH_LONG).show()
            }
        }

        // Функция для создания анимации кнопки
        fun animateButton(button: ImageButton, action: () -> Unit) {
            button.animate()
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(100)
                .withEndAction {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction {
                            action.invoke()
                        }
                        .start()
                }
                .start()
        }

        // Setup game controls с анимацией
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { button ->
            animateButton(button as ImageButton) {
                finish()
            }
        }

        // Добавим обработчики для быстрого перемещения
        val leftButton = findViewById<ImageButton>(R.id.leftButton)
        val rightButton = findViewById<ImageButton>(R.id.rightButton)

        // Обработчик для кнопки влево
        leftButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Анимация нажатия
                    view.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .start()
                    // Начинаем быстрое перемещение влево
                    view.isPressed = true
                    moveLeftRepeatedly()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Анимация отпускания
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    // Останавливаем перемещение
                    view.isPressed = false
                    true
                }
                else -> false
            }
        }

        // Обработчик для кнопки вправо
        rightButton.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Анимация нажатия
                    view.animate()
                        .scaleX(0.9f)
                        .scaleY(0.9f)
                        .setDuration(100)
                        .start()
                    // Начинаем быстрое перемещение вправо
                    view.isPressed = true
                    moveRightRepeatedly()
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Анимация отпускания
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .start()
                    // Останавливаем перемещение
                    view.isPressed = false
                    true
                }
                else -> false
            }
        }

        // Кнопка паузы с анимацией
        val pauseButton = findViewById<ImageButton>(R.id.pauseButton)
        pauseButton.setOnClickListener {
            animateButton(pauseButton) {
                if (tetrisView.isGamePaused()) {
                    tetrisView.resume()
                    pauseButton.setImageResource(android.R.drawable.ic_media_pause)
                } else {
                    tetrisView.pause()
                    pauseButton.setImageResource(android.R.drawable.ic_media_play)
                }
            }
        }

        // Кнопка старт с анимацией
        startButton.setOnClickListener {
            animateButton(startButton) {
                if (!isGameStarted) {
                    createSampleTasks()
                    tetrisView.startGame()
                    isGameStarted = true
                    startButton.setImageResource(android.R.drawable.ic_menu_revert)
                } else {
                    tetrisView.reset()
                    isGameStarted = false
                }
            }
        }

        // Setup Pomodoro button
        pomodoroButton.setOnClickListener {
            if (isPomodoroRunning) {
                stopPomodoro()
            } else {
                startPomodoro()
            }
        }

        // Исправляем обработчик кнопки поворота
        findViewById<ImageButton>(R.id.rotateButton).setOnClickListener { button ->
            if (!tetrisView.isGamePaused()) {
                animateButton(button as ImageButton) {
                    tetrisView.rotate()
                }
            }
        }

        // Исправляем обработчик кнопки сброса (быстрого падения)
        findViewById<ImageButton>(R.id.dropButton).setOnClickListener { button ->
            if (!tetrisView.isGamePaused()) {
                animateButton(button as ImageButton) {
                    tetrisView.dropToBottom()
                }
            }
        }

        // Инициализируем кнопку заморозки фигуры
        freezeButton = findViewById(R.id.freezeButton)
        freezeButton.setOnClickListener {
            tetrisView.freezeCurrentPiece()
        }
    }

    private fun createSampleTasks() {
        tasks.clear()
        
        // Загружаем сохраненные задачи
        val savedTasks = taskManager.loadTasks()
        if (savedTasks.isNotEmpty()) {
            // Если есть сохраненные задачи, используем их
            val taskNames = savedTasks.map { "${it.name} (${it.importance})" }
            val taskDescriptions = savedTasks.map { it.description }
            tetrisView.setTasks(taskNames, taskDescriptions, savedTasks)
        } else {
            // Если сохраненных задач нет, используем тестовые
            val defaultTasks = listOf(
                Task("Проверить почту", "Проверить рабочую и личную почту", "Работа", 15, "Средняя"),
                Task("Написать отчет", "Подготовить отчет за неделю", "Работа", 60, "Высокая"),
                Task("Созвониться с клиентом", "Обсудить текущий проект", "Работа", 30, "Высокая"),
                Task("Подготовить презентацию", "Создать презентацию для встречи", "Работа", 45, "Средняя"),
                Task("Сделать кофе", "Сделать перерыв на кофе", "Личное", 10, "Низкая"),
                Task("Провести митинг", "Провести ежедневный stand-up", "Работа", 30, "Средняя"),
                Task("Обновить документацию", "Актуализировать проектную документацию", "Работа", 60, "Высокая")
            )
            
            val names = defaultTasks.map { "${it.name} (${it.importance})" }
            val descriptions = defaultTasks.map { it.description }
            tetrisView.setTasks(names, descriptions, defaultTasks)
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale("ru"))
        dateText.text = dateFormat.format(Date())
    }

    private fun startPomodoro() {
        isPomodoroRunning = true
        pomodoroButton.text = "Остановить"
        pomodoroTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    runOnUiThread {
                        if (isWorkTime) {
                            workTimeRemaining--
                            if (workTimeRemaining <= 0) {
                                isWorkTime = false
                                workTimeRemaining = 25 * 60
                                Toast.makeText(this@GameActivity, "Время перерыва!", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            breakTimeRemaining--
                            if (breakTimeRemaining <= 0) {
                                isWorkTime = true
                                breakTimeRemaining = 5 * 60
                                Toast.makeText(this@GameActivity, "Время работы!", Toast.LENGTH_LONG).show()
                            }
                        }
                        updatePomodoroDisplay()
                    }
                }
            }, 0, 1000)
        }
    }

    private fun stopPomodoro() {
        isPomodoroRunning = false
        pomodoroButton.text = "Pomodoro"
        pomodoroTimer?.cancel()
        workTimeRemaining = 25 * 60
        breakTimeRemaining = 5 * 60
        isWorkTime = true
        updatePomodoroDisplay()
    }

    private fun updatePomodoroDisplay() {
        val minutes = if (isWorkTime) workTimeRemaining / 60 else breakTimeRemaining / 60
        val seconds = if (isWorkTime) workTimeRemaining % 60 else breakTimeRemaining % 60
        val mode = if (isWorkTime) "Работа" else "Перерыв"
        pomodoroButton.text = "$mode: ${minutes}:${seconds.toString().padStart(2, '0')}"
    }

    override fun onPause() {
        super.onPause()
        tetrisView.pause()
        if (isPomodoroRunning) {
            stopPomodoro()
        }
    }

    override fun onResume() {
        super.onResume()
        tetrisView.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        pomodoroTimer?.cancel()
        leftHandler.removeCallbacksAndMessages(null)
        rightHandler.removeCallbacksAndMessages(null)
    }

    // Функция для повторяющегося движения влево
    private fun moveLeftRepeatedly() {
        val leftRunnable = object : Runnable {
            override fun run() {
                if (findViewById<ImageButton>(R.id.leftButton).isPressed && !tetrisView.isGamePaused()) {
                    tetrisView.moveLeft()
                    leftHandler.postDelayed(this, 50) // Повторяем каждые 50 миллисекунд
                }
            }
        }
        leftHandler.post(leftRunnable)
    }

    // Функция для повторяющегося движения вправо
    private fun moveRightRepeatedly() {
        val rightRunnable = object : Runnable {
            override fun run() {
                if (findViewById<ImageButton>(R.id.rightButton).isPressed && !tetrisView.isGamePaused()) {
                    tetrisView.moveRight()
                    rightHandler.postDelayed(this, 50) // Повторяем каждые 50 миллисекунд
                }
            }
        }
        rightHandler.post(rightRunnable)
    }

    /**
     * Настраивает отображение режима в зависимости от выбранного режима
     */
    private fun setupTetrisViewForMode() {
        try {
            // Обновляем текст в зависимости от режима
            when (gameMode) {
                "day" -> {
                    dateText.text = "Режим: День"
                    tetrisView.setGameMode("day")
                }
                "week" -> {
                    dateText.text = "Режим: Неделя"
                    tetrisView.setGameMode("week")
                }
                "month" -> {
                    dateText.text = "Режим: Месяц"
                    tetrisView.setGameMode("month")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
} 