package com.miempresa.todolist

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miempresa.todolist.ui.theme.ToDoListTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val Context.dataStore by preferencesDataStore(name = "tasks_prefs")

data class Task(val title: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ToDoListTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    ToDoListApp()
                }
            }
        }
    }
}

@Composable
fun ToDoListApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var tasks by remember { mutableStateOf(listOf<Task>()) }
    var text by remember { mutableStateOf("") }

    // Estados para diálogo
    var showDialog by remember { mutableStateOf(false) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }

    val TASKS_KEY = stringSetPreferencesKey("tasks_set")

    // Cargar tareas al iniciar
    LaunchedEffect(Unit) {
        val prefs = context.dataStore.data.first()
        val savedTasks = prefs[TASKS_KEY] ?: emptySet()
        tasks = savedTasks.map { Task(it) }
    }

    // Función para guardar en DataStore
    fun saveTasks(updatedTasks: List<Task>) {
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[TASKS_KEY] = updatedTasks.map { it.title }.toSet()
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text("Escribe una tarea") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (text.isNotBlank()) {
                    val newTasks = tasks + Task(text)
                    tasks = newTasks
                    saveTasks(newTasks)
                    text = ""
                }
            })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (text.isNotBlank()) {
                    val newTasks = tasks + Task(text)
                    tasks = newTasks
                    saveTasks(newTasks)
                    text = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Agregar tarea")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(tasks) { task ->
                TaskItem(
                    task = task,
                    onLongPress = {
                        taskToDelete = task
                        showDialog = true
                    }
                )
            }
        }
    }

    // Diálogo de confirmación
    if (showDialog && taskToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Eliminar tarea") },
            text = { Text("¿Seguro que deseas eliminar esta tarea?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedTasks = tasks - taskToDelete!!
                        tasks = updatedTasks
                        saveTasks(updatedTasks)
                        taskToDelete = null
                        showDialog = false
                    }
                ) {
                    Text("Eliminar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TaskItem(task: Task, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        Text(
            text = task.title,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
