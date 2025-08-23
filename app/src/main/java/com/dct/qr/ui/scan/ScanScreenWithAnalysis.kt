package com.dct.qr.ui.scan // Nahraďte vaším skutočným balíčkom

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.dct.qr.R // Nahraďte vaším R súborom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.min // Potrebné pre minOf

const val TAG_SCAN_WITH_ANALYSIS = "ScanWithAnalysis"

@Composable
fun ScanScreenWithAnalysis(
    onQrCodeScanned: (value: String, type: Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            Log.d(TAG_SCAN_WITH_ANALYSIS, "Callback z permissionLauncher. Povolenie udelené: $granted")
            if (granted) {
                hasCameraPermission = true
            } else {
                Log.w(TAG_SCAN_WITH_ANALYSIS, "Povolenie pre kameru bolo ZAMIETNUTÉ.")
                // Tu by ste mohli zobraziť správu používateľovi alebo ho presmerovať do nastavení
            }
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            Log.i(TAG_SCAN_WITH_ANALYSIS, "Nemáme povolenie, žiadam o povolenie kamery...")
            permissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Log.i(TAG_SCAN_WITH_ANALYSIS, "Povolenie pre kameru už máme udelené.")
        }
    }

    val cameraExecutor: ExecutorService = remember {
        Log.d(TAG_SCAN_WITH_ANALYSIS, "Vytváram nový cameraExecutor (single thread).")
        Executors.newSingleThreadExecutor()
    }

    val qrCodeAnalyzer = remember(onQrCodeScanned) {
        Log.d(TAG_SCAN_WITH_ANALYSIS, "Vytváram novú inštanciu QrCodeAnalyzer.")
        QrCodeAnalyzer(
            onQrCodeScanned = { value, type ->
                Log.i(TAG_SCAN_WITH_ANALYSIS, "ScanScreen: QR kód prijatý: $value, typ: $type")
                onQrCodeScanned(value, type)
            },
            onAnalysisError = { exception ->
                Log.e(TAG_SCAN_WITH_ANALYSIS, "ScanScreen: Chyba v QrCodeAnalyzer", exception)
            },
            onProcessingComplete = {
                // Tento callback momentálne nevyužívame priamo tu
            }
        )
    }

    LaunchedEffect(key1 = lifecycleOwner, key2 = hasCameraPermission) {
        if (hasCameraPermission) {
            Log.d(TAG_SCAN_WITH_ANALYSIS, "ScanScreen sa (znovu)objavil a má povolenie, obnovujem skenovanie.")
            qrCodeAnalyzer.resumeScanning()
        } else {
            Log.d(TAG_SCAN_WITH_ANALYSIS, "ScanScreen sa (znovu)objavil, ale nemá povolenie, pozastavujem skenovanie.")
            qrCodeAnalyzer.pauseScanning()
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            Log.d(TAG_SCAN_WITH_ANALYSIS, "DisposableEffect (onDispose): Uvoľňujem cameraExecutor.")
            if (!cameraExecutor.isShutdown) {
                cameraExecutor.shutdown()
                Log.i(TAG_SCAN_WITH_ANALYSIS, "cameraExecutor.shutdown() bol úspešne zavolaný.")
            } else {
                Log.w(TAG_SCAN_WITH_ANALYSIS, "cameraExecutor už bol predtým vypnutý alebo sa vypína.")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            Log.i(TAG_SCAN_WITH_ANALYSIS, "Máme povolenie. Pokúšam sa zobraziť AndroidView pre kameru s QR analýzou.")
            AndroidView(
                factory = { androidViewContext ->
                    Log.d(TAG_SCAN_WITH_ANALYSIS, "AndroidView Factory: Vytváram PreviewView...")
                    val previewView = PreviewView(androidViewContext).apply {
                        this.id = R.id.preview_view_id // Uistite sa, že máte tento ID v ids.xml ak ho používate inde
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        Log.d(TAG_SCAN_WITH_ANALYSIS, "PreviewView vytvorená. ScaleType: ${this.scaleType}, ImplMode: ${this.implementationMode}")
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(androidViewContext)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val previewUseCase = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalysisUseCase = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    Log.d(TAG_SCAN_WITH_ANALYSIS, "ImageAnalysisUseCase vytvorený. Nastavujem QrCodeAnalyzer.")
                                    it.setAnalyzer(cameraExecutor, qrCodeAnalyzer)
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                previewUseCase,
                                imageAnalysisUseCase
                            )
                            Log.i(TAG_SCAN_WITH_ANALYSIS, "CameraX ÚSPEŠNE VIAZANÝ (Preview + QrCodeAnalyzer).")
                        } catch (e: Exception) {
                            Log.e(TAG_SCAN_WITH_ANALYSIS, "CHYBA pri konfigurácii alebo viazaní CameraX: ${e.message}", e)
                        }
                    }, ContextCompat.getMainExecutor(androidViewContext))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Prekrytie s rámčekom
            ScannerOverlay(modifier = Modifier.fillMaxSize())

            // Inštrukčný text
            InstructionText(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 72.dp) // Odsadenie od spodku, aby nezasahoval do navigačných prvkov OS
                    .padding(horizontal = 16.dp) // Aby text nebol príliš široký na menších obrazovkách
            )

        } else {
            Log.d(TAG_SCAN_WITH_ANALYSIS, "Nemáme povolenie pre kameru. Zobrazujem UI pre žiadosť.")
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.camera_permission_needed_simple), // Predpokladám, že máte tento string
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = {
                    Log.i(TAG_SCAN_WITH_ANALYSIS, "Užívateľ klikol na 'Udeliť povolenie'. Spúšťam permissionLauncher.")
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(stringResource(R.string.grant_permission_simple)) // Predpokladám, že máte tento string
                }
                Text(
                    stringResource(R.string.camera_permission_rationale_simple), // Predpokladám, že máte tento string
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}


@Composable
fun ScannerOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val frameFraction = 0.7f
        val strokeWidthPx = 0.dp.toPx() // Hrúbka rámčeka v pixeloch
        val cornerRadiusPx = 0.dp.toPx()

        val canvasWidth = size.width
        val canvasHeight = size.height

        val frameSize = min(canvasWidth, canvasHeight) * frameFraction
        val frameTopLeftX = (canvasWidth - frameSize) / 2
        val frameTopLeftY = (canvasHeight - frameSize) / 2

        // 1. Nakresliť stmavené pozadie pre celú plochu
        drawRect(
            color = Color.Black.copy(alpha = 0.5f),
            size = size
        )

        // 2. Nakresliť biely PLNY zaoblený obdĺžnik (bude to základ pre rámček)
        // Tento obdĺžnik bude mať vonkajšie rozmery nášho finálneho rámčeka.
        drawRoundRect(
            color = Color.White, // Farba rámčeka
            topLeft = Offset(frameTopLeftX, frameTopLeftY),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(cornerRadiusPx)
            // Štýl je predvolene Fill, čo je to, čo chceme
        )

        // 3. "Vyrezať" priehľadný otvor VNÚTRI bieleho obdĺžnika
        // Tento otvor bude o hrúbku rámčeka menší z každej strany.
        val innerFrameTopLeftX = frameTopLeftX + strokeWidthPx
        val innerFrameTopLeftY = frameTopLeftY + strokeWidthPx
        val innerFrameSize = frameSize - (2 * strokeWidthPx)

        // Uistiť sa, že vnútorný rámček nie je negatívnej veľkosti, ak by bol strokeWidth príliš veľký
        if (innerFrameSize > 0) {
            drawRoundRect(
                color = Color.Transparent, // Priehľadná farba na "vyrezanie"
                topLeft = Offset(innerFrameTopLeftX, innerFrameTopLeftY),
                size = Size(innerFrameSize, innerFrameSize),
                // Vnútorné rohy by mali mať polomer o hrúbku rámčeka menší,
                // aby vizuálne ladili s vonkajšími rohmi.
                // Ak je cornerRadiusPx <= strokeWidthPx, vnútorný roh bude ostrý (0f).
                cornerRadius = CornerRadius(maxOf(0f, cornerRadiusPx - strokeWidthPx)),
                blendMode = androidx.compose.ui.graphics.BlendMode.Clear // Dôležité pre "vyrezanie"
            )
        }



            }
}


@Composable
fun InstructionText(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.scan_qr_instruction), // Uistite sa, že máte tento string definovaný
        color = Color.White,
        fontSize = 16.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
    )
}

// Potrebné stringy v strings.xml (príklady, upravte podľa potreby):
/*
<resources>
    <string name="scan_qr_instruction">Umiestnite QR kód alebo čiarový kód do rámčeka</string>
    <string name="camera_permission_needed_simple">Pre skenovanie je potrebné povolenie pre fotoaparát.</string>
    <string name="grant_permission_simple">Udeliť povolenie</string>
    <string name="camera_permission_rationale_simple">Bez povolenia pre fotoaparát nemôže aplikácia skenovať kódy.</string>
</resources>
*/

// Ak používate R.id.preview_view_id, mali by ste mať v res/values/ids.xml niečo ako:
/*
<resources>
    <item type="id" name="preview_view_id" />
</resources>
*/
// Ak ho však nikde inde explicitne nepoužívate, nie je striktne nutné.
// PreviewView ho nepotrebuje pre svoju funkčnosť, pokiaľ naň neodkazujete z XML layoutu (čo tu nerobíme).
