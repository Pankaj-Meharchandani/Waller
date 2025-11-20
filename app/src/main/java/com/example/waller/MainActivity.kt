package com.example.waller

import android.app.Activity
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.SweepGradient
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import com.example.waller.ui.theme.WallerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import android.Manifest
import android.net.Uri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WaaaaalApp()
        }
    }
}

@Composable
fun WaaaaalApp() {
    val systemIsDark = isSystemInDarkTheme()
    var useDarkTheme by remember { mutableStateOf(systemIsDark) }
    WallerTheme(darkTheme = useDarkTheme) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            WallpaperGeneratorScreen(
                modifier = Modifier.padding(innerPadding),
                isAppDarkMode = useDarkTheme,
                onThemeChange = { useDarkTheme = !useDarkTheme }
            )
        }
    }
}

data class Wallpaper(
    val colors: List<Color>,
    val type: GradientType
)

enum class GradientType {
    Linear,
    Radial,
    Angular,
    Diamond
}

/* ------------------------------- Color/HSV helpers ------------------------------- */

fun colorToHsv(color: Color): FloatArray {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (color.red * 255).roundToInt(),
        (color.green * 255).roundToInt(),
        (color.blue * 255).roundToInt(),
        hsv
    )
    return hsv
}

fun Color.toHexString(): String {
    return String.format(
        "#%02X%02X%02X",
        (this.red * 255).roundToInt(),
        (this.green * 255).roundToInt(),
        (this.blue * 255).roundToInt()
    )
}

/* Generates a pleasing random color biased by light/dark tone */
fun generateRandomColor(isLight: Boolean): Color {
    val minLuminance = if (isLight) 0.5f else 0.0f
    val maxLuminance = if (isLight) 1.0f else 0.5f
    return Color(
        red = Random.nextFloat(),
        green = Random.nextFloat(),
        blue = Random.nextFloat(),
        alpha = 1f
    ).let { color ->
        val (h, s, _) = colorToHsv(color)
        val newV = (minLuminance + Random.nextFloat() * (maxLuminance - minLuminance)).coerceIn(0f, 1f)
        Color.hsv(h, s, newV)
    }
}

/* createShade: small variation close to base color; slightly larger deltas per your request */
fun createShade(color: Color, isLight: Boolean): Color {
    val hsv = colorToHsv(color)
    var h = hsv[0]
    var s = hsv[1]
    var v = hsv[2]

    val hueDelta = (Random.nextFloat() - 0.5f) * 40f      // +/- ~20°
    val satDelta = (Random.nextFloat() - 0.5f) * 0.6f     // +/- ~0.30
    val shadeFactor = Random.nextFloat() * 0.5f + 0.40f   // 0.40 .. 0.90

    h = (h + hueDelta).let {
        var hh = it
        while (hh < 0f) hh += 360f
        while (hh >= 360f) hh -= 360f
        hh
    }
    s = (s + satDelta).coerceIn(0f, 1f)
    v = if (isLight) (v + shadeFactor).coerceIn(0f, 1f) else (v - shadeFactor).coerceIn(0f, 1f)
    return Color.hsv(h, s, v)
}

/* ------------------------------- Color Picker & SV Picker (unchanged) ------------------------------- */

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    initialColor: Color? = null
) {
    var hue by remember { mutableFloatStateOf(initialColor?.let { colorToHsv(it)[0] } ?: 0f) }
    var saturation by remember { mutableFloatStateOf(initialColor?.let { colorToHsv(it)[1] } ?: 1f) }
    var value by remember { mutableFloatStateOf(initialColor?.let { colorToHsv(it)[2] } ?: 1f) }
    var hexCode by remember { mutableStateOf(Color.hsv(hue, saturation, value).toHexString()) }

    fun updateColor(newColor: Color) {
        val hsv = colorToHsv(newColor)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexCode = newColor.toHexString()
    }

    LaunchedEffect(initialColor) {
        initialColor?.let { hexCode = it.toHexString() }
    }

    val currentColor = Color.hsv(hue, saturation, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a color", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(currentColor)
                )
                Spacer(modifier = Modifier.height(12.dp))
                SaturationValuePicker(
                    hue = hue,
                    saturation = saturation,
                    value = value,
                    onSaturationValueChanged = { s, v ->
                        saturation = s
                        value = v
                        hexCode = Color.hsv(hue, s, v).toHexString()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Hue", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = hue,
                    onValueChange = {
                        hue = it
                        hexCode = Color.hsv(it, saturation, value).toHexString()
                    },
                    valueRange = 0f..360f
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = hexCode,
                    onValueChange = { newHex ->
                        hexCode = newHex
                        if (newHex.length == 7 && newHex.startsWith("#")) {
                            try {
                                val color = Color(newHex.toColorInt())
                                updateColor(color)
                            } catch (_: IllegalArgumentException) {
                            }
                        }
                    },
                    label = { Text("Hex Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        if (hexCode.length == 7) {
                            val color = Color(hexCode.toColorInt())
                            onColorSelected(color)
                        }
                    })
                )
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentColor) }) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SaturationValuePicker(
    hue: Float,
    saturation: Float,
    value: Float,
    onSaturationValueChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val satValSize = size
            val valueGradient = Brush.verticalGradient(
                colors = listOf(Color.White, Color.Black),
                startY = 0f,
                endY = satValSize.height
            )
            val saturationGradient = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color.hsv(hue, 1f, 1f)),
                startX = 0f,
                endX = satValSize.width
            )
            drawRect(color = Color.hsv(hue, 1f, 1f))
            drawRect(brush = valueGradient)
            drawRect(brush = saturationGradient)
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val newS = (offset.x / size.width).coerceIn(0f, 1f)
                        val newV = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(newS, newV)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newS = (change.position.x / size.width).coerceIn(0f, 1f)
                        val newV = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onSaturationValueChanged(newS, newV)
                    }
                }
        ) {
            val selectorX = saturation * size.width
            val selectorY = (1f - value) * size.height
            val selectorColor = if (value < 0.5f) Color.White else Color.Black
            drawCircle(
                color = selectorColor,
                radius = 8.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(selectorX, selectorY)
            )
        }
    }
}

/* ------------------------------- Main Screen & UI ------------------------------- */

@Composable
fun WallpaperGeneratorScreen(
    modifier: Modifier = Modifier,
    isAppDarkMode: Boolean,
    onThemeChange: () -> Unit
) {
    var isPortrait by remember { mutableStateOf(true) }
    var isLightTones by remember { mutableStateOf(true) }
    val selectedGradientTypes = remember { mutableStateListOf(GradientType.Linear) }
    val selectedColors = remember { mutableStateListOf<Color>() }
    var addNoise by remember { mutableStateOf(false) }
    var editingColorIndex by remember { mutableStateOf<Int?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    // Permission launcher for WRITE_EXTERNAL_STORAGE (only used for API < Q)
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Storage permission denied. Can't save wallpaper.", Toast.LENGTH_SHORT).show()
        }
        // If granted and we had pending wallpaper, the flow below will open the dialog because
        // showApplyDialogFor remains set. We don't need to do anything special here.
    }

    // dynamic columns/spans
    val spanCount = if (isPortrait) 2 else 1
    val columns = GridCells.Fixed(spanCount)

    // generate wallpapers function (uses same improved shading logic)
    fun generateWallpapers(): List<Wallpaper> {
        val wallpapers = mutableListOf<Wallpaper>()
        var previousType: GradientType? = null
        repeat(20) {
            val colors = when (selectedColors.size) {
                0 -> listOf(generateRandomColor(isLightTones), generateRandomColor(isLightTones))
                1 -> {
                    val base = selectedColors.first()
                    val shadedBase = createShade(base, isLightTones)
                    val secondBase = if (isLightTones) Color.White else Color.Black
                    val shadedSecond = createShade(secondBase, isLightTones)
                    listOf(shadedBase, shadedSecond)
                }
                else -> selectedColors.shuffled().take(2).map { createShade(it, isLightTones) }
            }

            val gradientType = run {
                val available = when {
                    selectedGradientTypes.isEmpty() -> GradientType.values().toList()
                    selectedGradientTypes.size == 1 -> selectedGradientTypes.toList()
                    else -> {
                        val filtered = selectedGradientTypes.filter { it != previousType }
                        if (filtered.isEmpty()) selectedGradientTypes.toList() else filtered
                    }
                }
                available.random()
            }
            previousType = gradientType
            wallpapers.add(Wallpaper(colors = colors, type = gradientType))
        }
        return wallpapers
    }

    var wallpapers by remember { mutableStateOf(generateWallpapers()) }

    // ---------------- pending click / dialog states ----------------
    var pendingClickedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) } // show small progress if needed

    // show color picker if editing index set
    if (editingColorIndex != null) {
        val idx = editingColorIndex!!
        val initial = if (idx >= 0 && idx < selectedColors.size) selectedColors[idx] else null
        ColorPickerDialog(
            initialColor = initial,
            onDismiss = { editingColorIndex = null },
            onColorSelected = { newColor ->
                if (idx >= 0 && idx < selectedColors.size) selectedColors[idx] = newColor else if (selectedColors.size < 5) selectedColors.add(newColor)
                editingColorIndex = null
            }
        )
    }

    // grid + UI
    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(span = { GridItemSpan(spanCount) }) { Header(onThemeChange = onThemeChange, isAppDarkMode = isAppDarkMode) }

        item(span = { GridItemSpan(spanCount) }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OrientationSelector(isPortrait = isPortrait, onOrientationChange = { isPortrait = it })
                }
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    WallpaperThemeSelector(isLightTones = isLightTones, onThemeChange = { isLightTones = it })
                }
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    GradientStylesSelector(selectedGradientTypes = selectedGradientTypes,
                        onStyleChange = { style ->
                            if (style in selectedGradientTypes) {
                                if (selectedGradientTypes.size > 1) selectedGradientTypes.remove(style)
                            } else selectedGradientTypes.add(style)
                        },
                        onSelectAll = {
                            selectedGradientTypes.clear(); selectedGradientTypes.addAll(GradientType.values())
                        },
                        onClear = {
                            selectedGradientTypes.clear(); selectedGradientTypes.add(GradientType.Linear)
                        })
                }
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ColorSelector(
                        selectedColors = selectedColors,
                        onAddColor = { editingColorIndex = -1 },
                        onEditColor = { idx -> editingColorIndex = idx },
                        onRemoveColor = { idx -> if (idx in selectedColors.indices) selectedColors.removeAt(idx) }
                    )
                }
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    EffectsSelector(addNoise = addNoise, onNoiseChange = { addNoise = it })
                }
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Actions(onRefreshClick = { wallpapers = generateWallpapers() })
                }
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val orientation = if (isPortrait) "portrait" else "landscape"
                val types = if (selectedGradientTypes.isEmpty()) "all" else selectedGradientTypes.joinToString(", ") { it.name.lowercase() }
                Text(text = "${wallpapers.size} wallpapers", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.weight(1f))
                Text(text = "$orientation • $types", style = MaterialTheme.typography.bodySmall)
            }
        }

        items(wallpapers) { wallpaper ->
            WallpaperItemCard(
                wallpaper = wallpaper,
                isPortrait = isPortrait,
                addNoise = addNoise,
                onClick = { clicked ->
                    // when clicked: set pending and ensure permission (only for pre-Q)
                    pendingClickedWallpaper = clicked
                    // On devices < Q we need WRITE_EXTERNAL_STORAGE to save; but applying wallpaper does not require it.
                    // We will request only if user chooses Download or if SDK < Q (MediaStore fallback).
                    // Simplest: always show dialog (no permission yet) then if user taps Download, we check permission before saving.
                    showApplyDialog = true
                }
            )
        }

        // bottom refresh button that scrolls to Actions
        item(span = { GridItemSpan(spanCount) }) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedButton(onClick = {
                    wallpapers = generateWallpapers()
                    coroutineScope.launch { gridState.animateScrollToItem(6) } // actions index (header etc kept same)
                }, modifier = Modifier.fillMaxWidth(0.6f).height(44.dp)) {
                    Text("Refresh All")
                }
            }
        }
    }

    // Apply / Download dialog
    if (showApplyDialog && pendingClickedWallpaper != null) {
        val wp = pendingClickedWallpaper!!
        Dialog(onDismissRequest = { showApplyDialog = false; pendingClickedWallpaper = null }) {
            Card(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth(0.9f)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Apply / Download", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose where to apply or save this wallpaper.", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        // Both home & lock
                        isWorking = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(context, wp, isPortrait, addNoise)
                            val flags = WallpaperManager.FLAG_SYSTEM or getLockFlag()
                            val success = tryApplyWallpaper(context, bmp, flags)
                            withContext(Dispatchers.Main) {
                                isWorking = false
                                Toast.makeText(context, if (success) "Applied to home & lock screen" else "Failed to apply", Toast.LENGTH_SHORT).show()
                                showApplyDialog = false; pendingClickedWallpaper = null
                            }
                        }
                    }) { Text("Both home & lock screen") }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        isWorking = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(context, wp, isPortrait, addNoise)
                            val success = tryApplyWallpaper(context, bmp, WallpaperManager.FLAG_SYSTEM)
                            withContext(Dispatchers.Main) {
                                isWorking = false
                                Toast.makeText(context, if (success) "Applied to home screen" else "Failed to apply", Toast.LENGTH_SHORT).show()
                                showApplyDialog = false; pendingClickedWallpaper = null
                            }
                        }
                    }) { Text("Home screen") }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(modifier = Modifier.fillMaxWidth(), onClick = {
                        isWorking = true
                        coroutineScope.launch(Dispatchers.IO) {
                            val bmp = createGradientBitmap(context, wp, isPortrait, addNoise)
                            val flagLock = getLockFlag()
                            val success = if (flagLock != 0) tryApplyWallpaper(context, bmp, flagLock) else tryApplyWallpaper(context, bmp, WallpaperManager.FLAG_SYSTEM)
                            withContext(Dispatchers.Main) {
                                isWorking = false
                                Toast.makeText(context, if (success) "Applied to lock screen" else "Failed to apply", Toast.LENGTH_SHORT).show()
                                showApplyDialog = false; pendingClickedWallpaper = null
                            }
                        }
                    }) { Text("Lock screen") }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        // Download: check permission for older devices (< Q) before saving
                        val sdkTooOld = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                        if (sdkTooOld && ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            // request then save in launcher callback flow
                            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            // We'll save only after permission granted; to do that, set a one-shot listener:
                            // Simpler approach: show a Toast asking user to press Download again after granting permission.
                            Toast.makeText(context, "Please grant storage permission and tap Download again", Toast.LENGTH_LONG).show()
                        } else {
                            // proceed to save
                            isWorking = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp = createGradientBitmap(context, wp, isPortrait, addNoise)
                                val filename = "waller_${System.currentTimeMillis()}.png"
                                val saved = saveBitmapToMediaStore(context, bmp, filename)
                                withContext(Dispatchers.Main) {
                                    isWorking = false
                                    Toast.makeText(context, if (saved) "Saved to Pictures" else "Save failed", Toast.LENGTH_SHORT).show()
                                    showApplyDialog = false; pendingClickedWallpaper = null
                                }
                            }
                        }
                    }) { Text("Download") }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = { showApplyDialog = false; pendingClickedWallpaper = null }) { Text("Cancel") }
                    if (isWorking) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

/* ------------------------------- Small UI components reused (Header, selectors) ------------------------------- */

@Composable
fun Header(onThemeChange: () -> Unit, isAppDarkMode: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = "Waller", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Generate colorful wallpapers", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.size(40.dp).background(color = if (isAppDarkMode) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).border(width = 1.dp, color = if (isAppDarkMode) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).clickable { onThemeChange() }, contentAlignment = Alignment.Center) {
            if (isAppDarkMode) Icon(Icons.Filled.DarkMode, contentDescription = "Dark", tint = Color.White) else Icon(Icons.Filled.LightMode, contentDescription = "Light", tint = Color.Black)
        }
    }
}

@Composable
fun OrientationSelector(isPortrait: Boolean, onOrientationChange: (Boolean) -> Unit) {
    Column {
        Text(text = "Orientation", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            if (isPortrait) {
                Button(onClick = {}) { Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Portrait") }
            } else {
                OutlinedButton(onClick = { onOrientationChange(true) }) { Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Portrait") }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isPortrait) {
                Button(onClick = {}) { Icon(Icons.Filled.DesktopWindows, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Landscape") }
            } else {
                OutlinedButton(onClick = { onOrientationChange(false) }) { Icon(Icons.Filled.DesktopWindows, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Landscape") }
            }
        }
    }
}

@Composable
fun WallpaperThemeSelector(isLightTones: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column {
        Text(text = "Wallpaper Theme", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Dark Tones")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(checked = isLightTones, onCheckedChange = { onThemeChange(it) })
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Light Tones")
        }
    }
}

@Composable
fun GradientStylesSelector(
    selectedGradientTypes: List<GradientType>,
    onStyleChange: (GradientType) -> Unit,
    onSelectAll: () -> Unit,
    onClear: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Gradient Styles (${selectedGradientTypes.size}/4)", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSelectAll) { Text("Select All") }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onClear) { Text("Clear") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = GradientType.Linear in selectedGradientTypes, onCheckedChange = { onStyleChange(GradientType.Linear) })
            Spacer(modifier = Modifier.width(6.dp)); Text("Linear"); Spacer(modifier = Modifier.width(16.dp))
            Checkbox(checked = GradientType.Radial in selectedGradientTypes, onCheckedChange = { onStyleChange(GradientType.Radial) })
            Spacer(modifier = Modifier.width(6.dp)); Text("Radial")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = GradientType.Angular in selectedGradientTypes, onCheckedChange = { onStyleChange(GradientType.Angular) })
            Spacer(modifier = Modifier.width(6.dp)); Text("Angular"); Spacer(modifier = Modifier.width(16.dp))
            Checkbox(checked = GradientType.Diamond in selectedGradientTypes, onCheckedChange = { onStyleChange(GradientType.Diamond) })
            Spacer(modifier = Modifier.width(6.dp)); Text("Diamond")
        }
    }
}

@Composable
fun ColorSelector(
    selectedColors: List<Color>,
    onAddColor: () -> Unit,
    onEditColor: (Int) -> Unit,
    onRemoveColor: (Int) -> Unit
) {
    Column {
        Text(text = "Colors (${selectedColors.size}/5)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onAddColor, enabled = selectedColors.size < 5) { Text("+ Add Color") }
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedColors.isNotEmpty()) OutlinedButton(onClick = { onRemoveColor(selectedColors.lastIndex) }) { Text("- Remove Last") }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (selectedColors.isEmpty()) Text("No colors selected - using random colors", style = MaterialTheme.typography.bodySmall) else Column {
            selectedColors.forEachIndexed { idx, color ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).background(color, shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).border(1.dp, Color.Black.copy(alpha = 0.08f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).clickable { onEditColor(idx) })
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = color.toHexString(), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onEditColor(idx) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Palette, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = { onRemoveColor(idx) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.85f)) }
                }
            }
        }
    }
}

@Composable
fun EffectsSelector(addNoise: Boolean, onNoiseChange: (Boolean) -> Unit) {
    Column {
        Text(text = "Effects", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addNoise, onCheckedChange = onNoiseChange); Spacer(modifier = Modifier.width(8.dp)); Text("Noise / Grain")
        }
    }
}

@Composable
fun Actions(onRefreshClick: () -> Unit) {
    Column {
        Text(text = "Actions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onRefreshClick, modifier = Modifier.fillMaxWidth()) { Text("Refresh All") }
    }
}

/* ------------------------------- Wallpaper preview items ------------------------------- */

@Composable
fun WallpaperItemCard(wallpaper: Wallpaper, isPortrait: Boolean, addNoise: Boolean, onClick: (Wallpaper) -> Unit) {
    Card(modifier = Modifier.aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f).fillMaxWidth().clickable { onClick(wallpaper) }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)) {
        WallpaperItem(wallpaper, addNoise)
    }
}

@Composable
fun WallpaperItem(wallpaper: Wallpaper, addNoise: Boolean) {
    val brush = when (wallpaper.type) {
        GradientType.Linear -> Brush.linearGradient(wallpaper.colors)
        GradientType.Radial -> Brush.radialGradient(wallpaper.colors)
        GradientType.Angular -> Brush.sweepGradient(wallpaper.colors)
        GradientType.Diamond -> Brush.linearGradient(wallpaper.colors)
    }
    Box(modifier = Modifier.background(brush).fillMaxSize()) {
        if (addNoise) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val noiseSize = 1.dp.toPx()
                val numNoisePoints = (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                repeat(numNoisePoints) {
                    val x = Random.nextFloat() * size.width
                    val y = Random.nextFloat() * size.height
                    val alpha = Random.nextFloat() * 0.15f
                    drawCircle(color = Color.White.copy(alpha = alpha), radius = noiseSize, center = androidx.compose.ui.geometry.Offset(x, y))
                }
            }
        }
        Row(modifier = Modifier.align(Alignment.BottomStart).padding(10.dp).background(Color.Black.copy(alpha = 0.28f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = wallpaper.type.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall, color = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            wallpaper.colors.take(2).forEach { c -> Box(modifier = Modifier.size(12.dp).background(c, shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp))); Spacer(modifier = Modifier.width(6.dp)) }
        }
    }
}

// Get a practical bitmap size based on the device/window (portrait or landscape)
fun getScreenSizeForBitmap(context: Context, isPortrait: Boolean): Pair<Int, Int> {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = wm.currentWindowMetrics
            val insets = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            val w = metrics.bounds.width() - insets.left - insets.right
            val h = metrics.bounds.height() - insets.top - insets.bottom
            if (isPortrait) Pair(minOf(w, h), maxOf(w, h)) else Pair(maxOf(w, h), minOf(w, h))
        } else {
            val dm = DisplayMetrics()
            (context as Activity).windowManager
            val w = dm.widthPixels
            val h = dm.heightPixels
            if (isPortrait) Pair(minOf(w, h), maxOf(w, h)) else Pair(maxOf(w, h), minOf(w, h))
        }
    } catch (e: Exception) {
        if (isPortrait) Pair(1080, 1920) else Pair(1920, 1080)
    }
}

/**
 * Create a bitmap that matches the wallpaper preview using Android shaders.
 * Uses fully-qualified Android Canvas (AndroidCanvas) to avoid Compose/Android name clash.
 */
fun createGradientBitmap(
    context: Context,
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean = false
): Bitmap {
    val (width, height) = getScreenSizeForBitmap(context, isPortrait)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    // convert Compose Color to Android ARGB ints
    val colors = wallpaper.colors.map {
        android.graphics.Color.argb(
            (it.alpha * 255).roundToInt(),
            (it.red * 255).roundToInt(),
            (it.green * 255).roundToInt(),
            (it.blue * 255).roundToInt()
        )
    }.toIntArray()

    // draw gradient same as before
    when (wallpaper.type) {
        GradientType.Linear, GradientType.Diamond -> {
            val shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
            val paint = Paint().apply { isAntiAlias = true; this.shader = shader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        GradientType.Radial -> {
            val radius = max(width, height) * 0.6f
            val shader = RadialGradient(
                width / 2f, height / 2f, radius,
                colors, null, Shader.TileMode.CLAMP
            )
            val paint = Paint().apply { isAntiAlias = true; this.shader = shader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        GradientType.Angular -> {
            val shader = SweepGradient(width / 2f, height / 2f, colors, null)
            val paint = Paint().apply { isAntiAlias = true; this.shader = shader }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    // --- draw noise/grain onto the same bitmap if requested ---
    if (addNoise) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        // choose noise size similar to Compose: ~1.dp on screen -> convert proportionally
        // we pick a small radius in pixels
        val baseDp = 1f
        val density = (context.resources.displayMetrics.density).coerceAtLeast(1f)
        val noiseSizePx = (baseDp * density).coerceAtLeast(1f)

        // number of noise points - same heuristic as Compose overlay: 2% of pixels approx
        val numNoisePoints = ((width.toLong() * height.toLong()) / (noiseSizePx.toLong() * noiseSizePx.toLong()) * 0.02f).toInt().coerceAtLeast(200)

        val rnd = Random(System.currentTimeMillis())

        repeat(numNoisePoints) {
            val x = rnd.nextFloat() * width
            val y = rnd.nextFloat() * height

            // alpha up to ~0.15 (as in Compose overlay)
            val alpha = (rnd.nextFloat() * 0.15f).coerceIn(0f, 1f)
            // paint white with computed alpha (keeps old behavior - white specks)
            val alphaInt = (alpha * 255).roundToInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.argb(alphaInt, 255, 255, 255)

            // radius: noiseSizePx (or 0.5..1.5x jitter)
            val radius = noiseSizePx * (0.6f + rnd.nextFloat() * 1.2f)

            canvas.drawCircle(x, y, radius, paint)
        }
    }

    return bmp
}


/**
 * Save a bitmap to MediaStore (Pictures/Waller). Handles API differences.
 * Safely handles nullable OutputStream and returns boolean success.
 */
fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, displayName: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Waller")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                // Ensure folder exists for legacy devices (we still insert to MediaStore)
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/Waller"
                val file = File(pictures)
                if (!file.exists()) file.mkdirs()
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri: Uri = resolver.insert(collection, contentValues) ?: return false

        // openOutputStream can return null — handle safely
        resolver.openOutputStream(uri)?.use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!compressed) {
                // remove created entry if compression failed
                resolver.delete(uri, null, null)
                return false
            }
        } ?: run {
            // couldn't open stream
            resolver.delete(uri, null, null)
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } else {
            // For older devices: notify media scanner
            val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            intent.data = uri
            context.sendBroadcast(intent)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/**
 * Apply bitmap as wallpaper. Uses setBitmap with flags when available.
 */
fun tryApplyWallpaper(context: Context, bitmap: Bitmap, flags: Int = WallpaperManager.FLAG_SYSTEM): Boolean {
    return try {
        val manager = WallpaperManager.getInstance(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager.setBitmap(bitmap, null, true, flags)
        } else {
            manager.setBitmap(bitmap)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

/** helper to return lock flag (or 0 if not supported) */
fun getLockFlag(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) WallpaperManager.FLAG_LOCK else 0
}
