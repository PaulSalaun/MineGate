package com.saunaltech.mindgate.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saunaltech.mindgate.app.data.db.MindGateDatabase
import com.saunaltech.mindgate.app.data.db.entity.ThemeEntity
import com.saunaltech.mindgate.app.data.preferences.MindGatePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { MindGatePreferences(context) }
    rememberCoroutineScope()

    var selectedLangue by remember { mutableStateOf(prefs.loadLangue()) }
    var themes by remember { mutableStateOf<List<ThemeEntity>>(emptyList()) }
    var selectedThemeIds by remember { mutableStateOf(prefs.loadActiveThemeIds().toSet()) }

    val langues = listOf("FR", "EN", "DE", "ES")

    LaunchedEffect(selectedLangue) {
        themes = withContext(Dispatchers.IO) {
            MindGateDatabase.getInstance(context).themeDao().getThemesByLangue(selectedLangue)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Retour") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {

            // --- Langue ---
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Langue des questions", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    langues.forEach { langue ->
                        FilterChip(
                            selected = selectedLangue == langue,
                            onClick = {
                                selectedLangue = langue
                                prefs.saveLangue(langue)
                                selectedThemeIds = emptySet()
                                prefs.saveActiveThemeIds(emptySet())
                            },
                            label = { Text(langue) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(24.dp))
            }

            // --- Thèmes ---
            item {
                Text("Thèmes actifs", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Laisser vide = tous les thèmes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (themes.isEmpty()) {
                item {
                    Text(
                        "Aucun thème disponible pour cette langue.\nSynchronise les questions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(themes, key = { it.id }) { theme ->
                    ThemeRow(
                        theme = theme,
                        isSelected = theme.id in selectedThemeIds,
                        onToggle = { selected ->
                            selectedThemeIds = if (selected) {
                                selectedThemeIds + theme.id
                            } else {
                                selectedThemeIds - theme.id
                            }
                            prefs.saveActiveThemeIds(selectedThemeIds)
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ThemeRow(
    theme: ThemeEntity,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(theme.nom, style = MaterialTheme.typography.bodyLarge)
            if (theme.description.isNotBlank()) {
                Text(
                    theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = isSelected, onCheckedChange = onToggle)
    }
}