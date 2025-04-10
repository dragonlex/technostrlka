package com.example.test8

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import android.app.Activity
import java.util.*
import android.graphics.Bitmap
import android.content.Intent
import android.view.LayoutInflater
import android.app.AlertDialog
import android.widget.TextView

class TetrisView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.LTGRAY
        strokeWidth = 1f
    }

    private val bottomBorderPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.DKGRAY
        strokeWidth = 3f
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 24f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private var cellSize = 0f
    private var gridWidth = 0
    private var gridHeight = 0
    private var grid = Array(20) { Array(8) { 0 } }
    private var currentPiece: TetrisPiece? = null
    private var nextPiece: TetrisPiece? = null
    private var gameSpeed = 500L // milliseconds
    private var isPaused = false
    private var gameTimer: Timer? = null
    private var controlPanelHeight = 0f
    private var tasks = listOf<String>()
    private var currentTaskIndex = 0
    private var gridText = Array(20) { Array<String>(8) { "" } }
    private var completedLines = 0
    private var onLinesCompletedListener: ((Int) -> Unit)? = null
    private var gridDescriptions = Array(20) { Array<String>(8) { "" } }
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var taskDescriptions = listOf<String>()
    private var hasTasksRemaining = true
    private var onGameCompletedListener: (() -> Unit)? = null
    private var gridTasks = Array(20) { Array<Task?>(8) { null } }
    private var taskList = listOf<Task>()
    private var gameMode: String = "day" // По умолчанию режим "день"

    fun isGamePaused(): Boolean = isPaused

    init {
        startGame()
        setOnLongClickListener { view ->
            val rawX = lastTouchX - view.left
            val rawY = lastTouchY - view.top
            showDescription(rawX, rawY)
            true
        }
    }

    fun startGame() {
        gameTimer?.cancel()
        grid = Array(20) { Array(8) { 0 } }
        gridText = Array(20) { Array(8) { "" } }
        gridDescriptions = Array(20) { Array(8) { "" } }
        currentPiece = null
        nextPiece = null
        isPaused = false
        hasTasksRemaining = true
        currentTaskIndex = 0

        gameTimer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    if (!isPaused && (hasTasksRemaining || currentPiece != null)) {
                        moveDown()
                        postInvalidate()
                    }
                }
            }, 0, gameSpeed)
        }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        
        // Используем всю доступную высоту
        val availableHeight = height
        
        // Отступ для времени сбоку (увеличен на одну клетку)
        val timeMargin = 80f
        
        // Если gridWidth еще не установлен, устанавливаем его по умолчанию
        if (gridWidth == 0) {
            gridWidth = 8 // Уменьшаем ширину поля на 2 клетки (было 10, стало 8)
        }
        
        // Рассчитываем размер ячейки на основе ширины с учетом отступа для времени
        cellSize = ((width - timeMargin) / gridWidth).toFloat()
        
        // Если размеры поля еще не были установлены через setGridSize,
        // то устанавливаем их по умолчанию
        if (gridHeight == 0) {
            gridHeight = (availableHeight / cellSize).toInt()
            
            // Обновляем размер сетки
        grid = Array(gridHeight) { Array(gridWidth) { 0 } }
            
            // Обновляем размер массива для текста
            gridText = Array(gridHeight) { Array(gridWidth) { "" } }
            
            // Обновляем размер массива для описаний
            gridDescriptions = Array(gridHeight) { Array(gridWidth) { "" } }
            
            // Обновляем размер массива для задач
            gridTasks = Array(gridHeight) { Array(gridWidth) { null } }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawGrid(canvas)
        drawCurrentPiece(canvas)
        drawModeText(canvas)
    }

    private fun drawGrid(canvas: Canvas) {
        // Отступ для времени сбоку (увеличен на одну клетку)
        val timeMargin = 80f
        
        // Рисуем вертикальные линии
        for (i in 0..gridWidth) {
            canvas.drawLine(
                i * cellSize,
                0f,
                i * cellSize,
                gridHeight * cellSize,
                gridPaint
            )
        }
        
        // Рисуем горизонтальные линии
        for (i in 0..gridHeight) {
            canvas.drawLine(
                0f,
                i * cellSize,
                gridWidth * cellSize, // Ограничиваем ширину линий шириной игрового поля
                i * cellSize,
                gridPaint
            )
        }

        // Рисуем заполненные ячейки с их текстом
        for (y in 0 until gridHeight) {
            for (x in 0 until gridWidth) {
                if (grid[y][x] != 0) {
                    drawCell(canvas, x, y, grid[y][x], gridText[y][x])
                }
            }
        }
    }

    private fun drawCurrentPiece(canvas: Canvas) {
        currentPiece?.let { piece ->
            piece.shape.forEach { (x, y) ->
                drawCell(canvas, piece.x + x, piece.y + y, piece.color, piece.taskName)
            }
        }
    }

    private fun drawCell(canvas: Canvas, x: Int, y: Int, color: Int, taskName: String = "") {
        paint.color = color
        val rect = RectF(
            x * cellSize,
            y * cellSize,
            (x + 1) * cellSize,
            (y + 1) * cellSize
        )
        canvas.drawRect(rect, paint)
        
        if (taskName.isNotEmpty()) {
            // Настраиваем параметры текста
            textPaint.color = Color.BLACK
            textPaint.textSize = cellSize * 0.5f
            
            // Получаем границы текста для центрирования
            val textBounds = Rect()
            textPaint.getTextBounds(taskName, 0, taskName.length, textBounds)
            
            // Если текст слишком длинный, обрезаем его
            var displayText = taskName
            while (textPaint.measureText(displayText) > cellSize * 0.9f && displayText.length > 3) {
                displayText = displayText.substring(0, displayText.length - 4) + "..."
            }
            
            // Рисуем текст в центре ячейки
            canvas.drawText(
                displayText,
                rect.centerX(),
                rect.centerY() + textBounds.height() / 2,
                textPaint
            )
        }
    }

    fun moveLeft() {
        if (!isPaused) {
        currentPiece?.let { piece ->
            if (canMove(piece, -1, 0)) {
                piece.x--
                invalidate()
                }
            }
        }
    }

    fun moveRight() {
        if (!isPaused) {
        currentPiece?.let { piece ->
            if (canMove(piece, 1, 0)) {
                piece.x++
                invalidate()
                }
            }
        }
    }

    fun rotate() {
        if (!isPaused) {
        currentPiece?.let { piece ->
            val rotated = piece.rotate()
            if (canMove(rotated, 0, 0)) {
                currentPiece = rotated
                invalidate()
                }
            }
        }
    }

    fun moveDown() {
        if (!isPaused) {
        currentPiece?.let { piece ->
            if (canMove(piece, 0, 1)) {
                piece.y++
                invalidate()
            } else {
                lockPiece()
                clearLines()
                spawnNewPiece()
            }
        } ?: spawnNewPiece()
        }
    }

    fun dropToBottom() {
        if (!isPaused) {
            currentPiece?.let { piece ->
                // Двигаем фигуру вниз, пока это возможно
                while (canMove(piece, 0, 1)) {
                    piece.y++
                }
                // Фиксируем фигуру и создаем новую
                lockPiece()
                spawnNewPiece()
                invalidate()
            }
        }
    }

    private fun canMove(piece: TetrisPiece, dx: Int, dy: Int): Boolean {
        return piece.shape.all { (x, y) ->
            val newX = piece.x + x + dx
            val newY = piece.y + y + dy
            newX in 0 until gridWidth && newY in 0 until gridHeight && grid[newY][newX] == 0
        }
    }

    private fun lockPiece() {
        currentPiece?.let { piece ->
            piece.shape.forEach { (x, y) ->
                val newY = piece.y + y
                val newX = piece.x + x
                if (newY in 0 until gridHeight && newX in 0 until gridWidth) {
                    grid[newY][newX] = piece.color
                    gridText[newY][newX] = piece.taskName
                    gridDescriptions[newY][newX] = piece.description
                    gridTasks[newY][newX] = piece.task
                }
            }
            checkLines()
            
            // Показываем уведомление о завершении задач только после того, как текущая фигура зафиксирована
            // и это была последняя фигура
            if (!hasTasksRemaining && nextPiece == null) {
                (context as? Activity)?.runOnUiThread {
                    gameTimer?.cancel()
                    Toast.makeText(context, "Все задачи выполнены!", Toast.LENGTH_LONG).show()
                    // Вызываем слушатель завершения игры
                    onGameCompletedListener?.invoke()
                }
            }
        }
    }

    private fun checkLines() {
        for (y in grid.indices.reversed()) {
            if (grid[y].all { it != 0 }) {
                completedLines++
                onLinesCompletedListener?.invoke(completedLines)
            }
        }
    }

    private fun clearLines() {
        var linesCleared = 0
        for (y in grid.indices.reversed()) {
            if (grid[y].all { it != 0 }) {
                for (y2 in y downTo 1) {
                    grid[y2] = grid[y2 - 1].copyOf()
                    gridText[y2] = gridText[y2 - 1].copyOf()
                }
                grid[0] = Array(gridWidth) { 0 }
                gridText[0] = Array(gridWidth) { "" }
                linesCleared++
            }
        }
    }

    private fun spawnNewPiece() {
        // Если нет задач, но текущая фигура уже существует, не создаем новую
        if (!hasTasksRemaining && currentPiece == null) {
            return
        }

        currentPiece = nextPiece ?: createRandomPiece()
        nextPiece = createRandomPiece()
        
        // Если не удалось создать следующую фигуру (нет задач)
        if (nextPiece == null) {
            // Устанавливаем флаг, что это последняя фигура
            hasTasksRemaining = false
        }

        // Проверяем, можно ли разместить текущую фигуру
        if (currentPiece != null && !canMove(currentPiece!!, 0, 0)) {
            // Game Over
            gameTimer?.cancel()
        }
    }

    private fun createRandomPiece(): TetrisPiece? {
        // Проверяем, есть ли еще задачи
        if (tasks.isEmpty() || currentTaskIndex >= tasks.size) {
            hasTasksRemaining = false
              // Останавливаем таймер
            // Уведомление о завершении задач будет показано только после того, как текущая фигура будет опущена
            return null
        }
        
        // Получаем следующую задачу из списка
        val taskName = tasks[currentTaskIndex]
        val taskDescription = taskDescriptions.getOrNull(currentTaskIndex) ?: ""
        val task = if (currentTaskIndex < taskList.size) taskList[currentTaskIndex] else null
        currentTaskIndex++
        
        // Выбираем фигуру в зависимости от длительности задачи
        val shape = selectShapeByDuration(task?.duration ?: 15)
        val color = selectColorByImportance(task?.importance ?: "Средняя")
        
        // Размещаем фигуру в центре уменьшенного поля
        return TetrisPiece(shape, color, gridWidth / 2 - 1, 0, taskName, taskDescription, task)
    }
    
    private fun selectShapeByDuration(duration: Int): List<Pair<Int, Int>> {
        // Фигуры разного размера в зависимости от длительности
        return when {
            // Короткие задачи (15-30 минут) - маленькие фигуры
            duration <= 30 -> listOf(
                listOf(Pair(0, 0)), // 1x1
                listOf(Pair(0, 0), Pair(1, 0)), // 1x2
                listOf(Pair(0, 0), Pair(0, 1)) // 2x1
            ).random()
            
            // Средние задачи (31-60 минут) - средние фигуры
            duration <= 60 -> listOf(
                listOf(Pair(0, 0), Pair(1, 0), Pair(0, 1), Pair(1, 1)), // 2x2 квадрат
                listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2)), // 1x3
                listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0)), // 3x1
                listOf(Pair(0, 0), Pair(0, 1), Pair(1, 1)), // L маленький
                listOf(Pair(0, 0), Pair(1, 0), Pair(1, 1)) // L маленький отраженный
            ).random()
            
            // Длинные задачи (более 60 минут) - большие фигуры
            else -> listOf(
                listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1), Pair(2, 1)), // 3x2
                listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(0, 2)), // L большой
                listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(2, 1), Pair(2, 2)), // L большой отраженный
                listOf(Pair(0, 0), Pair(1, 0), Pair(2, 0), Pair(0, 1), Pair(1, 1)), // T большой
                listOf(Pair(0, 0), Pair(0, 1), Pair(0, 2), Pair(1, 0), Pair(1, 1), Pair(1, 2)) // 2x3
            ).random()
        }
    }
    
    private fun selectColorByImportance(importance: String): Int {
        return when (importance) {
            "Высокая" -> Color.RED
            "Средняя" -> Color.BLUE
            "Низкая" -> Color.GREEN
            else -> Color.GRAY
        }
    }

    fun pause() {
        isPaused = true
    }

    fun resume() {
        isPaused = false
    }

    fun freezeCurrentPiece() {
        if (!isPaused && currentPiece != null) {
            lockPiece()
            spawnNewPiece()
            invalidate()
        }
    }

    fun reset() {
        grid = Array(gridHeight) { Array(gridWidth) { 0 } }
        gridText = Array(gridHeight) { Array(gridWidth) { "" } }
        gridDescriptions = Array(gridHeight) { Array(gridWidth) { "" } }
        gridTasks = Array(gridHeight) { Array(gridWidth) { null } }
        currentPiece = null
        nextPiece = null
        hasTasksRemaining = true
        currentTaskIndex = 0
        invalidate()
    }

    private var currentTask: Task? = null

    fun setTasks(taskNames: List<String>, descriptions: List<String>, tasks: List<Task>) {
        this.tasks = taskNames
        this.taskDescriptions = descriptions
        this.taskList = tasks
        this.currentTaskIndex = 0
        this.hasTasksRemaining = true
        this.currentTask = if (tasks.isNotEmpty()) tasks[0] else null
    }

    fun setOnLinesCompletedListener(listener: (Int) -> Unit) {
        onLinesCompletedListener = listener
    }

    fun showDescription(x: Float, y: Float) {
        val gridX = (x / cellSize).toInt()
        val gridY = (y / cellSize).toInt()
        
        if (gridX in 0 until gridWidth && gridY in 0 until gridHeight) {
            val task = gridTasks[gridY][gridX]
            if (task != null) {
                showTaskInfoDialog(task)
            }
        }
    }

    private fun showTaskInfoDialog(task: Task) {
        // Ставим игру на паузу при открытии диалога
        pause()
        
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_task_info, null)
        
        // Заполняем информацию о задаче
        dialogView.findViewById<TextView>(R.id.taskTitle).text = task.name
        dialogView.findViewById<TextView>(R.id.taskDescription).text = task.description
        dialogView.findViewById<TextView>(R.id.taskDate).text = 
            "Категория: ${task.category}\nВажность: ${task.importance}\nДлительность: ${task.duration} мин"
        dialogView.findViewById<TextView>(R.id.taskDate).visibility = View.VISIBLE

        // Создаем и показываем диалог
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // Добавляем обработчик для кнопки закрытия
        dialogView.findViewById<View>(R.id.closeButton).setOnClickListener {
            // Возобновляем игру при закрытии диалога
            resume()
            dialog.dismiss()
        }
        
        // Добавляем обработчик для кнопки удаления фигуры
        dialogView.findViewById<View>(R.id.deleteButton).setOnClickListener {
            // Находим все ячейки с этой задачей и удаляем их
            var found = false
            for (y in 0 until gridHeight) {
                for (x in 0 until gridWidth) {
                    if (gridTasks[y][x] == task) {
                        grid[y][x] = 0
                        gridText[y][x] = ""
                        gridDescriptions[y][x] = ""
                        gridTasks[y][x] = null
                        found = true
                    }
                }
            }
            
            // Если нашли и удалили хотя бы одну ячейку
            if (found) {
                // Проверяем, есть ли пустые строки, которые нужно удалить
                clearLines()
                // Перерисовываем поле
                invalidate()
                // Показываем уведомление
                Toast.makeText(context, "Фигура удалена", Toast.LENGTH_SHORT).show()
            }
            
            // Возобновляем игру при закрытии диалога
            resume()
            // Закрываем диалог
            dialog.dismiss()
        }
        
        // Добавляем слушатель для закрытия диалога (например, при нажатии вне диалога)
        dialog.setOnDismissListener {
            // Возобновляем игру при закрытии диалога любым способом
            resume()
        }

        dialog.show()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        lastTouchX = event.x
        lastTouchY = event.y
        
        if (event.action == MotionEvent.ACTION_UP) {
            showDescription(event.x, event.y)
        }
        
        return super.onTouchEvent(event)
    }

    /**
     * Создает скриншот игровой зоны
     */
    fun createGameScreenshot(): Bitmap? {
        try {
            // Создаем битмап с размерами view
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Рисуем view на canvas
            draw(canvas)
            
            return bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Устанавливает слушатель завершения игры
     */
    fun setOnGameCompletedListener(listener: () -> Unit) {
        onGameCompletedListener = listener
    }

    /**
     * Устанавливает размер поля тетриса
     * @param width Ширина поля (количество ячеек по горизонтали)
     * @param height Высота поля (количество ячеек по вертикали)
     */
    fun setGridSize(width: Int, height: Int) {
        gridWidth = width
        gridHeight = height
        
        // Обновляем размер сетки
        grid = Array(gridHeight) { Array(gridWidth) { 0 } }
        
        // Обновляем размер массива для текста
        gridText = Array(gridHeight) { Array(gridWidth) { "" } }
        
        // Обновляем размер массива для описаний
        gridDescriptions = Array(gridHeight) { Array(gridWidth) { "" } }
        
        // Обновляем размер массива для задач
        gridTasks = Array(gridHeight) { Array(gridWidth) { null } }
        
        // Отступ для времени сбоку (увеличен на одну клетку)
        val timeMargin = 80f
        
        // Рассчитываем размер ячейки на основе ширины view с учетом отступа для времени
        if (this.width > 0) {
            cellSize = ((this.width - timeMargin) / gridWidth).toFloat()
        }
        
        // Перерисовываем view
        invalidate()
    }

    /**
     * Устанавливает режим игры
     * @param mode Режим игры ("day", "week", "month")
     */
    fun setGameMode(mode: String) {
        gameMode = mode
        invalidate()
    }

    private fun drawModeText(canvas: Canvas) {
        try {
            // Настраиваем параметры текста для подписей
            val textPaint = Paint().apply {
                color = Color.BLACK
                textSize = cellSize * 0.4f
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            
            // Отступ от игрового поля
            val timeMargin = 80f
            
            when (gameMode) {
                "day" -> {
                    // Рисуем часы сбоку поля (от 7 до 24)
                    val startHour = 7
                    val endHour = 24
                    val totalHours = endHour - startHour + 1
                    
                    // Вычисляем, сколько ячеек приходится на один час
                    val cellsPerHour = gridHeight.toFloat() / totalHours
                    
                    // Рисуем часы рядом с каждой соответствующей ячейкой
                    for (hour in startHour..endHour) {
                        // Вычисляем позицию Y для текущего часа (перевернуто сверху вниз)
                        val hourIndex = endHour - hour
                        val yPosition = (hourIndex * cellsPerHour + cellsPerHour / 2) * cellSize
                        
                        // Рисуем текст часа в отведенном для времени месте (правее)
                        canvas.drawText(
                            "$hour:00",
                            width - 50f, // Значительно увеличиваем отступ для времени с 30f до 50f
                            yPosition,
                            textPaint
                        )
                    }
                }
                "week" -> {
                    // Рисуем дни недели сбоку поля
                    val daysOfWeek = listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС")
                    val cellsPerDay = gridHeight.toFloat() / daysOfWeek.size
                    
                    // Рисуем дни недели рядом с каждой соответствующей ячейкой
                    for (i in daysOfWeek.indices) {
                        // Переворачиваем индексы сверху вниз
                        val reversedIndex = daysOfWeek.size - 1 - i
                        val yPosition = (reversedIndex * cellsPerDay + cellsPerDay / 2) * cellSize
                        
                        // Рисуем текст дня недели в отведенном для времени месте
                        canvas.drawText(
                            daysOfWeek[i],
                            width - 10f,
                            yPosition,
                            textPaint
                        )
                    }
                }
                "month" -> {
                    // Рисуем числа месяца сбоку поля (от 1 до 31)
                    val startDay = 1
                    val endDay = 31
                    val totalDays = endDay - startDay + 1
                    
                    // Вычисляем, сколько ячеек приходится на один день
                    val cellsPerDay = gridHeight.toFloat() / totalDays
                    
                    // Рисуем числа месяца рядом с каждой соответствующей ячейкой
                    for (day in startDay..endDay) {
                        // Вычисляем позицию Y для текущего дня (перевернуто сверху вниз)
                        val dayIndex = endDay - day
                        val yPosition = (dayIndex * cellsPerDay + cellsPerDay / 2) * cellSize
                        
                        // Рисуем текст дня в отведенном для времени месте
                        canvas.drawText(
                            day.toString(),
                            width - 10f,
                            yPosition,
                            textPaint
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class TetrisPiece(
    val shape: List<Pair<Int, Int>>,
    val color: Int,
    var x: Int,
    var y: Int,
    val taskName: String = "",
    val description: String = "",
    val task: Task? = null
) {
    fun rotate(): TetrisPiece {
        val rotatedShape = shape.map { (x, y) -> Pair(-y, x) }
        return copy(shape = rotatedShape)
    }
} 