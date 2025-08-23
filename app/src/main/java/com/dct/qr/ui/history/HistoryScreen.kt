@file:OptIn(ExperimentalMaterial3Api::class)

package com.dct.qr.ui.history

import kotlinx.coroutines.flow.Flow // Uistite sa, že tento import je prítomný
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModel
import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dct.qr.R // Váš R súbor
import com.dct.qr.data.db.ScannedCode
import com.dct.qr.data.repository.HistoryRepository
import com.dct.qr.ui.theme.QRTheme // Vaša téma
import java.util.Date
import kotlinx.coroutines.flow.flowOf // Tento je tiež potrebný pre preview

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel // Bude injektovaný alebo poskytnutý
) {
    val uiState by historyViewModel.historyUiState.collectAsState()
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var codeToDelete by remember { mutableStateOf<ScannedCode?>(null) } // Stav pre kód na vymazanie
    var showDeleteItemDialog by remember { mutableStateOf(false) } // Stav pre zobrazenie dialógu vymazania položky

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_title_history)) },
                actions = {
                    val codesAvailable = when (val state = uiState) {
                        is HistoryUiState.Success -> state.codes.isNotEmpty()
                        // Ak je Empty, ale predtým bol Success a zoznam nebol prázdny (napr. po vymazaní všetkých)
                        // V tomto prípade by sme nemali ukazovať tlačidlo, preto sa spoliehame na Success.codes.isNotEmpty()
                        else -> false
                    }
                    if (codesAvailable) {
                        IconButton(onClick = { showDeleteAllDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.action_clear_history)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is HistoryUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is HistoryUiState.Success -> {
                    if (state.codes.isEmpty()) {
                        EmptyHistoryView(Modifier.align(Alignment.Center))
                    } else {
                        HistoryList(
                            codes = state.codes,
                            onAttemptDeleteCode = { code ->
                                codeToDelete = code // Nastavíme kód, ktorý chceme vymazať
                                showDeleteItemDialog = true // Zobrazíme dialóg
                            },
                            onCodeClick = { code ->
                                // TODO: Čo sa má stať po kliknutí na položku?
                                // Napr. zobraziť detail, skopírovať, otvoriť URL...
                                println("Clicked on: ${code.content}")
                            }
                        )
                    }
                }
                is HistoryUiState.Empty -> {
                    EmptyHistoryView(Modifier.align(Alignment.Center))
                }
                is HistoryUiState.Error -> {
                    Text(
                        text = state.message,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    // Dialóg pre vymazanie všetkých položiek
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text(stringResource(R.string.dialog_title_clear_history)) },
            text = { Text(stringResource(R.string.dialog_message_clear_history)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        historyViewModel.clearAllHistory()
                        showDeleteAllDialog = false
                    }
                ) { Text(stringResource(R.string.action_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Dialóg pre vymazanie jednotlivej položky
    if (showDeleteItemDialog && codeToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteItemDialog = false
                codeToDelete = null // Vyčistíme stav
            },
            title = { Text(stringResource(R.string.dialog_title_delete_item)) },
            text = { Text(stringResource(R.string.dialog_message_delete_item_confirm, codeToDelete!!.content.take(50) + if (codeToDelete!!.content.length > 50) "..." else "")) }, // Zobrazí prvých 50 znakov
            confirmButton = {
                TextButton(
                    onClick = {
                        codeToDelete?.let { code ->
                            historyViewModel.deleteCode(code)
                        }
                        showDeleteItemDialog = false
                        codeToDelete = null
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteItemDialog = false
                    codeToDelete = null
                }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun HistoryList(
    codes: List<ScannedCode>,
    onAttemptDeleteCode: (ScannedCode) -> Unit, // Zmenené z onDeleteCode
    onCodeClick: (ScannedCode) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier.padding(8.dp)) {
        items(items = codes, key = { it.id }) { code ->
            HistoryItem(
                scannedCode = code,
                onDeleteClick = { onAttemptDeleteCode(code) }, // Volá nový lambda
                onItemClick = { onCodeClick(code) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
fun HistoryItem(
    scannedCode: ScannedCode,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val formattedDate = remember(scannedCode.timestamp) {
        val dateFormat = DateFormat.getDateFormat(context)
        val timeFormat = DateFormat.getTimeFormat(context)
        "${dateFormat.format(Date(scannedCode.timestamp))} ${timeFormat.format(Date(scannedCode.timestamp))}"
    }

    ListItem(
        headlineContent = {
            Text(
                text = scannedCode.content,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = "Typ: ${scannedCode.type} - $formattedDate",
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.action_delete_item)
                )
            }
        },
        modifier = modifier.clickable(onClick = onItemClick)
    )
}

@Composable
fun EmptyHistoryView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(R.string.history_empty_placeholder),
            style = MaterialTheme.typography.titleMedium
        )
    }
}


@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview_Success() {
    QRTheme {
        val fakeCodes = listOf(
            ScannedCode(1, "https://example.com", 8, System.currentTimeMillis() - 100000),
            ScannedCode(2, "Produkt XYZ123", 256, System.currentTimeMillis() - 200000),
            ScannedCode(3, "Veľmi dlhý text, ktorý by sa mal zalomiť alebo skrátiť pomocou elipsy, aby sa zmestil na dva riadky.", 1, System.currentTimeMillis())
        )
        val fakeViewModel = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val mockDao = object : com.dct.qr.data.db.ScannedCodeDao {
                    override suspend fun insertCode(scannedCode: ScannedCode) {}
                    override fun getAllCodes(): Flow<List<ScannedCode>> = flowOf(fakeCodes)
                    override suspend fun getCodeById(id: Int): ScannedCode? = null
                    override suspend fun deleteCode(scannedCode: ScannedCode) {}
                    override suspend fun clearAllCodes() {}
                }
                // Ak HistoryRepository nie je 'open', toto je správny spôsob
                val mockRepo = HistoryRepository(mockDao)
                return HistoryViewModel(mockRepo) as T
            }
        }.create(HistoryViewModel::class.java)

        HistoryScreen(historyViewModel = fakeViewModel)
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview_Empty() {
    QRTheme {
        val fakeViewModel = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val mockDao = object : com.dct.qr.data.db.ScannedCodeDao {
                    override suspend fun insertCode(scannedCode: ScannedCode) {}
                    override fun getAllCodes(): Flow<List<ScannedCode>> = flowOf(emptyList()) // Prázdny zoznam
                    override suspend fun getCodeById(id: Int): ScannedCode? = null
                    override suspend fun deleteCode(scannedCode: ScannedCode) {}
                    override suspend fun clearAllCodes() {}
                }
                val mockRepo = HistoryRepository(mockDao)
                return HistoryViewModel(mockRepo) as T
            }
        }.create(HistoryViewModel::class.java)
        HistoryScreen(historyViewModel = fakeViewModel)
    }
}
