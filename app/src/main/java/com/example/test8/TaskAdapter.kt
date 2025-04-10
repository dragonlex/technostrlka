package com.example.test8

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TaskAdapter(
    private var tasks: MutableList<Task>,
    private val onDeleteClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val taskName: TextView = view.findViewById(R.id.taskNameText)
        val taskDescription: TextView = view.findViewById(R.id.taskDescriptionText)
        val taskDetails: TextView = view.findViewById(R.id.taskDetailsText)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.taskName.text = task.name
        holder.taskDescription.text = task.description
        holder.taskDetails.text = "Категория: ${task.category} • Важность: ${task.importance} • Длительность: ${task.duration} мин"
        
        holder.itemView.setOnClickListener {
            showTaskInfoDialog(holder.itemView.context, task)
        }
        
        holder.deleteButton.tag = task
        
        holder.deleteButton.setOnClickListener {
            val taskToDelete = it.tag as? Task
            if (taskToDelete != null) {
                val positionToRemove = tasks.indexOf(taskToDelete)
                if (positionToRemove != -1) {
                    onDeleteClick(taskToDelete)
                    tasks.removeAt(positionToRemove)
                    notifyItemRemoved(positionToRemove)
                }
            }
        }
    }

    override fun getItemCount() = tasks.size

    fun updateTasks(newTasks: List<Task>) {
        tasks.clear()
        tasks.addAll(newTasks)
        notifyDataSetChanged()
    }

    private fun showTaskInfoDialog(context: android.content.Context, task: Task) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_task_info, null)
        
        dialogView.findViewById<TextView>(R.id.taskTitle).text = task.name
        dialogView.findViewById<TextView>(R.id.taskDescription).text = task.description
        dialogView.findViewById<TextView>(R.id.taskDate).text = 
            "Категория: ${task.category}\nВажность: ${task.importance}\nДлительность: ${task.duration} мин"

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        dialogView.findViewById<View>(R.id.closeButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
} 