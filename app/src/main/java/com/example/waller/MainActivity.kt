package com.example.waller

// ---------- IMPORTS (updated) ----------
import android.Manifest
import android.R.string.cancel
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
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.waller.ui.theme.WallerTheme
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.color.SimpleColorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.core.graphics.scale

// ---------- END IMPORTS ----------

class MainActivity : FragmentActivity(), SimpleDialog.OnDialogResultListener {

    companion object {
        private const val COLOR_DIALOG_TAG = "COLOR_DIALOG"
    }

    // callback that Compose sets when it wants a color
    private var pendingColorCallback: ((Int?) -> Unit)? = null

    /**
     * Called from Compose when user wants to add / edit a color.
     * Shows SimpleColorDialog and returns picked color (or null on cancel).
     */
    fun openColorDialog(initialColor: Int?, callback: (Int?) -> Unit) {
        pendingColorCallback = callback

        val builder = SimpleColorDialog.build()
            .allowCustom(true)
            .neg(cancel)

        if (initialColor != null) {
            builder.colorPreset(initialColor)
        }

        // title is optional – you can localize later via resources if you want
        builder.show(this, COLOR_DIALOG_TAG)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (dialogTag == COLOR_DIALOG_TAG) {
            val callback = pendingColorCallback
            pendingColorCallback = null

            if (callback != null) {
                if (which == SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE) {   // ✅
                    val colorInt = extras.getInt(SimpleColorDialog.COLOR)
                    callback(colorInt)
                } else {
                    // cancelled / negative
                    callback(null)
                }
            }
            return true
        }
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WallerApp()
        }
    }

}

@Composable
fun WallerApp() {
    val systemIsDark = isSystemInDarkTheme()
    var useDarkTheme by remember { mutableStateOf(systemIsDark) }

    WallerTheme(darkTheme = useDarkTheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onBackground
            ) { innerPadding ->
                WallpaperGeneratorScreen(
                    modifier = Modifier.padding(innerPadding),
                    isAppDarkMode = useDarkTheme,
                    onThemeChange = { useDarkTheme = !useDarkTheme }
                )
            }
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

/** Convert Compose Color to ARGB Int (for SimpleColorDialog) */
fun Color.toArgbInt(): Int =
    android.graphics.Color.argb(
        (alpha * 255).roundToInt(),
        (red * 255).roundToInt(),
        (green * 255).roundToInt(),
        (blue * 255).roundToInt()
    )

/** Convert ARGB Int from SimpleColorDialog back to Compose Color */
fun Int.toComposeColor(): Color =
    Color(
        red = android.graphics.Color.red(this) / 255f,
        green = android.graphics.Color.green(this) / 255f,
        blue = android.graphics.Color.blue(this) / 255f,
        alpha = android.graphics.Color.alpha(this) / 255f
    )

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
        val newV =
            (minLuminance + Random.nextFloat() * (maxLuminance - minLuminance)).coerceIn(0f, 1f)
        Color.hsv(h, s, newV)
    }
}

/* createShade: small variation close to base color */
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
    v =
        if (isLight) (v + shadeFactor).coerceIn(0f, 1f) else (v - shadeFactor).coerceIn(0f, 1f)
    return Color.hsv(h, s, v)
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
    var addStripes by remember { mutableStateOf(false) }
    var addOverlay by remember { mutableStateOf(false) }
    var editingColorIndex by remember { mutableStateOf<Int?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current

    // Permission launcher for WRITE_EXTERNAL_STORAGE (only used for API < Q)
    val writePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Storage permission denied. Can't save wallpaper.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // dynamic columns/spans
    val spanCount = if (isPortrait) 2 else 1
    val columns = GridCells.Fixed(spanCount)

    // generate wallpapers function
    fun generateWallpapers(): List<Wallpaper> {
        val wallpapers = mutableListOf<Wallpaper>()
        var previousType: GradientType? = null
        repeat(20) {
            val colors = when (selectedColors.size) {
                0 -> listOf(
                    generateRandomColor(isLightTones),
                    generateRandomColor(isLightTones)
                )

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

    // ----- NEW: open SimpleColorDialog when editingColorIndex changes -----
    if (editingColorIndex != null) {
        val idx = editingColorIndex!!
        LaunchedEffect(idx) {
            val activity = context as? MainActivity
            val initialColor =
                if (idx >= 0 && idx < selectedColors.size) selectedColors[idx] else null

            if (activity == null) {
                // in preview or weird context – just clear
                editingColorIndex = null
            } else {
                activity.openColorDialog(initialColor?.toArgbInt()) { pickedInt ->
                    if (pickedInt != null) {
                        val pickedColor = pickedInt.toComposeColor()
                        if (idx >= 0 && idx < selectedColors.size) {
                            selectedColors[idx] = pickedColor
                        } else if (selectedColors.size < 5) {
                            selectedColors.add(pickedColor)
                        }
                    }
                    editingColorIndex = null
                }
            }
        }
    }

    // ---------------- pending click / dialog states ----------------
    var pendingClickedWallpaper by remember { mutableStateOf<Wallpaper?>(null) }
    var showApplyDialog by remember { mutableStateOf(false) }
    var isWorking by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = columns,
        state = gridState,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(span = { GridItemSpan(spanCount) }) {
            Header(onThemeChange = onThemeChange, isAppDarkMode = isAppDarkMode)
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                OrientationSelector(
                    isPortrait = isPortrait,
                    onOrientationChange = { isPortrait = it }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                WallpaperThemeSelector(
                    isLightTones = isLightTones,
                    onThemeChange = { isLightTones = it }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                GradientStylesSelector(
                    selectedGradientTypes = selectedGradientTypes,
                    onStyleChange = { style ->
                        if (style in selectedGradientTypes) {
                            if (selectedGradientTypes.size > 1) selectedGradientTypes.remove(style)
                        } else selectedGradientTypes.add(style)
                    },
                    onSelectAll = {
                        selectedGradientTypes.clear()
                        selectedGradientTypes.addAll(GradientType.values())
                    },
                    onClear = {
                        selectedGradientTypes.clear()
                        selectedGradientTypes.add(GradientType.Linear)
                    }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                ColorSelector(
                    selectedColors = selectedColors,
                    onAddColor = { editingColorIndex = -1 },
                    onEditColor = { idx -> editingColorIndex = idx },
                    onRemoveColor = { idx ->
                        if (idx in selectedColors.indices) selectedColors.removeAt(idx)
                    }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                EffectsSelector(
                    addNoise = addNoise,
                    onNoiseChange = { addNoise = it },
                    addStripes = addStripes,
                    onStripesChange = { addStripes = it },
                    addOverlay = addOverlay,
                    onOverlayChange = { addOverlay = it }
                )
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            SectionCard {
                Actions(onRefreshClick = { wallpapers = generateWallpapers() })
            }
        }

        item(span = { GridItemSpan(spanCount) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val orientation = if (isPortrait) "portrait" else "landscape"
                val types =
                    if (selectedGradientTypes.isEmpty()) "all" else selectedGradientTypes.joinToString(
                        ", "
                    ) { it.name.lowercase() }
                Text(
                    text = "${wallpapers.size} wallpapers",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$orientation • $types",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        items(wallpapers) { wallpaper ->
            WallpaperItemCard(
                wallpaper = wallpaper,
                isPortrait = isPortrait,
                addNoise = addNoise,
                addStripes = addStripes,
                addOverlay = addOverlay,
                onClick = { clicked ->
                    pendingClickedWallpaper = clicked
                    showApplyDialog = true
                }
            )
        }

        item(span = { GridItemSpan(spanCount) }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedButton(
                    onClick = {
                        wallpapers = generateWallpapers()
                        coroutineScope.launch { gridState.animateScrollToItem(6) }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(999.dp))
                ) {
                    Text("Refresh All")
                }
            }
        }
    }

    // Apply / Download dialog (unchanged logic)
    if (showApplyDialog && pendingClickedWallpaper != null) {
        val wp = pendingClickedWallpaper!!
        Dialog(
            onDismissRequest = {
                showApplyDialog = false
                pendingClickedWallpaper = null
            }
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                ),
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        "Apply / Download",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Choose where to apply or save this wallpaper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            isWorking = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp =
                                    createGradientBitmap(context, wp, isPortrait, addNoise, addStripes, addOverlay)
                                val flags =
                                    WallpaperManager.FLAG_SYSTEM or getLockFlag()
                                val success = tryApplyWallpaper(context, bmp, flags)
                                withContext(Dispatchers.Main) {
                                    isWorking = false
                                    Toast.makeText(
                                        context,
                                        if (success) "Applied to home & lock screen" else "Failed to apply",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showApplyDialog = false
                                    pendingClickedWallpaper = null
                                }
                            }
                        }
                    ) {
                        Text("Both home & lock screen")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            isWorking = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp =
                                    createGradientBitmap(context, wp, isPortrait, addNoise, addStripes, addOverlay)
                                val success =
                                    tryApplyWallpaper(context, bmp, WallpaperManager.FLAG_SYSTEM)
                                withContext(Dispatchers.Main) {
                                    isWorking = false
                                    Toast.makeText(
                                        context,
                                        if (success) "Applied to home screen" else "Failed to apply",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showApplyDialog = false
                                    pendingClickedWallpaper = null
                                }
                            }
                        }
                    ) {
                        Text("Home screen")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        onClick = {
                            isWorking = true
                            coroutineScope.launch(Dispatchers.IO) {
                                val bmp =
                                    createGradientBitmap(context, wp, isPortrait, addNoise, addStripes, addOverlay)
                                val flagLock = getLockFlag()
                                val success =
                                    if (flagLock != 0) tryApplyWallpaper(
                                        context,
                                        bmp,
                                        flagLock
                                    ) else tryApplyWallpaper(
                                        context,
                                        bmp,
                                        WallpaperManager.FLAG_SYSTEM
                                    )
                                withContext(Dispatchers.Main) {
                                    isWorking = false
                                    Toast.makeText(
                                        context,
                                        if (success) "Applied to lock screen" else "Failed to apply",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showApplyDialog = false
                                    pendingClickedWallpaper = null
                                }
                            }
                        }
                    ) {
                        Text("Lock screen")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        onClick = {
                            val sdkTooOld =
                                Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
                            if (sdkTooOld &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                Toast.makeText(
                                    context,
                                    "Please grant storage permission and tap Download again",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                isWorking = true
                                coroutineScope.launch(Dispatchers.IO) {
                                    val bmp =
                                        createGradientBitmap(context, wp, isPortrait, addNoise, addStripes, addOverlay)
                                    val filename =
                                        "waller_${System.currentTimeMillis()}.png"
                                    val saved =
                                        saveBitmapToMediaStore(context, bmp, filename)
                                    withContext(Dispatchers.Main) {
                                        isWorking = false
                                        Toast.makeText(
                                            context,
                                            if (saved) "Saved to Pictures" else "Save failed",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        showApplyDialog = false
                                        pendingClickedWallpaper = null
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Download")
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showApplyDialog = false
                            pendingClickedWallpaper = null
                        }
                    ) {
                        Text("Cancel")
                    }
                    if (isWorking) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(999.dp))
                        )
                    }
                }
            }
        }
    }
}

/* ------------------------------- Reusable Section Card ------------------------------- */

@Composable
fun SectionCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(18.dp), clip = false)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                RoundedCornerShape(18.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            content()
        }
    }
}

/* ------------------------------- Small UI components ------------------------------- */

@Composable
fun Header(onThemeChange: () -> Unit, isAppDarkMode: Boolean) {
    val chipSize = 42.dp
    val chipShape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(chipSize)
                .clip(chipShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    chipShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Palette,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "Waller",
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = "Generate colorful, grainy gradients",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(chipSize)
                .clip(chipShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    chipShape
                )
                .clickable { onThemeChange() },
            contentAlignment = Alignment.Center
        ) {
            if (isAppDarkMode) {
                Icon(
                    Icons.Filled.DarkMode,
                    contentDescription = "Dark",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    Icons.Filled.LightMode,
                    contentDescription = "Light",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OrientationSelector(isPortrait: Boolean, onOrientationChange: (Boolean) -> Unit) {
    Column {
        Text(
            text = "Orientation",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row {
            if (isPortrait) {
                Button(
                    onClick = {},
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Portrait")
                }
            } else {
                OutlinedButton(
                    onClick = { onOrientationChange(true) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.StayCurrentPortrait, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Portrait")
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (!isPortrait) {
                Button(
                    onClick = {},
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.DesktopWindows, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Landscape")
                }
            } else {
                OutlinedButton(
                    onClick = { onOrientationChange(false) },
                    modifier = Modifier.height(40.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Icon(Icons.Filled.DesktopWindows, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Landscape")
                }
            }
        }
    }
}

@Composable
fun WallpaperThemeSelector(isLightTones: Boolean, onThemeChange: (Boolean) -> Unit) {
    Column {
        Text(
            text = "Wallpaper Theme",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Dark Tones")
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = isLightTones,
                onCheckedChange = { onThemeChange(it) }
            )
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
            Text(
                text = "Gradient Styles (${selectedGradientTypes.size}/4)",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = onSelectAll) { Text("Select All") }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(onClick = onClear) { Text("Clear") }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Mix multiple styles for a varied grid.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = GradientType.Linear in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Linear) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Linear")
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = GradientType.Radial in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Radial) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Radial")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = GradientType.Angular in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Angular) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Angular")
            Spacer(modifier = Modifier.width(16.dp))
            Checkbox(
                checked = GradientType.Diamond in selectedGradientTypes,
                onCheckedChange = { onStyleChange(GradientType.Diamond) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Diamond")
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
        Text(
            text = "Colors (${selectedColors.size}/5)",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (selectedColors.isEmpty())
                "No colors selected - random palettes will be used."
            else
                "Tap a swatch or palette icon to tweak a color.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onAddColor,
                enabled = selectedColors.size < 5,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.height(40.dp)
            ) {
                Text("+ Add Color")
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (selectedColors.isNotEmpty()) {
                OutlinedButton(
                    onClick = { onRemoveColor(selectedColors.lastIndex) },
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("- Remove Last")
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (selectedColors.isEmpty()) {
            Text(
                "Tip: Lock 1–5 base colors and let Waller generate shades around them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column {
                selectedColors.forEachIndexed { idx, color ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(color)
                                .border(
                                    1.dp,
                                    Color.Black.copy(alpha = 0.08f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onEditColor(idx) }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = color.toHexString(),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onEditColor(idx) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Palette,
                                contentDescription = "Edit",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { onRemoveColor(idx) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.Red.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EffectsSelector(
    addNoise: Boolean,
    onNoiseChange: (Boolean) -> Unit,
    addStripes: Boolean,
    onStripesChange: (Boolean) -> Unit,
    addOverlay: Boolean,
    onOverlayChange: (Boolean) -> Unit
) {
    Column {
        Text(
            text = "Effects",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Noise, stripes, and optional Nothing-style overlay.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Noise
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addNoise, onCheckedChange = onNoiseChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Snow effect")
                Text(
                    "Soft snow texture.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generated stripes
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addStripes, onCheckedChange = onStripesChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Stripes overlay")
                Text(
                    "Vertical translucent stripes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Nothing style
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(checked = addOverlay, onCheckedChange = onOverlayChange)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text("Nothing Style")
                Text(
                    "Add Nothing like glass effect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun Actions(onRefreshClick: () -> Unit) {
    Column {
        Text(
            text = "Actions",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onRefreshClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            shape = RoundedCornerShape(999.dp)
        ) {
            Text("Refresh All")
        }
    }
}

/* ------------------------------- Wallpaper preview items ------------------------------- */

@Composable
fun WallpaperItemCard(
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean,
    addStripes: Boolean,
    onClick: (Wallpaper) -> Unit,
    addOverlay: Boolean
) {
    Card(
        modifier = Modifier
            .aspectRatio(if (isPortrait) 9f / 16f else 16f / 9f)
            .fillMaxWidth()
            .clickable { onClick(wallpaper) },
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.04f)
        )
    ) {
        WallpaperItem(wallpaper, addNoise, addStripes, addOverlay )
    }
}

@Composable
fun WallpaperItem(wallpaper: Wallpaper, addNoise: Boolean, addNothingStripes: Boolean, addOverlay: Boolean) {
    val brush = when (wallpaper.type) {
        GradientType.Linear -> Brush.linearGradient(wallpaper.colors)
        GradientType.Radial -> Brush.radialGradient(wallpaper.colors)
        GradientType.Angular -> Brush.sweepGradient(wallpaper.colors)
        GradientType.Diamond -> Brush.linearGradient(wallpaper.colors)
    }
    val overlayPainter = painterResource(id = R.drawable.overlay_stripes)
    val overlayAspectRatio =
        overlayPainter.intrinsicSize.width / overlayPainter.intrinsicSize.height

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 1. Draw gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush)
        )

        // 2. Noise
        if (addNoise) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val noiseSize = 1.dp.toPx()
                val numNoisePoints =
                    (size.width * size.height / (noiseSize * noiseSize) * 0.02f).toInt()
                repeat(numNoisePoints) {
                    val x = Random.nextFloat() * size.width
                    val y = Random.nextFloat() * size.height
                    val alpha = Random.nextFloat() * 0.15f
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = noiseSize,
                        center = Offset(x, y)
                    )
                }
            }
        }

        // 3. Generated stripes
        if (addNothingStripes) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stripeCount = 18
                val stripeWidth = size.width / (stripeCount * 2f)
                for (i in 0 until stripeCount) {
                    val left = i * stripeWidth * 2f
                    drawRect(
                        color = Color.White.copy(alpha = 0.10f),
                        topLeft = Offset(left, 0f),
                        size = Size(stripeWidth, size.height)
                    )
                }
            }
        }

        // 4. PNG Overlay — ALWAYS LAST
        if (addOverlay) {
            Image(
                painter = painterResource(id = R.drawable.overlay_stripes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
        }

        // 5. Tag at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(
                    Color.Black.copy(alpha = 0.36f),
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 10.dp, vertical = 4.dp), // a bit tighter
            verticalAlignment = Alignment.CenterVertically      // 👈 important
        ) {
            Text(
                text = wallpaper.type.name
                    .lowercase()
                    .replaceFirstChar { it.uppercase() },
                color = Color.White
            )

            Spacer(Modifier.width(8.dp))

            wallpaper.colors.take(2).forEach {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(it)
                )
                Spacer(Modifier.width(6.dp))
            }
        }
    }
}

// Get a practical bitmap size based on the device/window (portrait or landscape)
fun getScreenSizeForBitmap(context: Context, isPortrait: Boolean): Pair<Int, Int> {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = wm.currentWindowMetrics
            val insets =
                metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
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
 */
fun createGradientBitmap(
    context: Context,
    wallpaper: Wallpaper,
    isPortrait: Boolean,
    addNoise: Boolean = false,
    addStripes: Boolean = false,
    addOverlay: Boolean = false
): Bitmap {
    val (width, height) = getScreenSizeForBitmap(context, isPortrait)
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)

    val colors = wallpaper.colors.map {
        android.graphics.Color.argb(
            (it.alpha * 255).roundToInt(),
            (it.red * 255).roundToInt(),
            (it.green * 255).roundToInt(),
            (it.blue * 255).roundToInt()
        )
    }.toIntArray()

    when (wallpaper.type) {
        GradientType.Linear, GradientType.Diamond -> {
            val shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                colors, null, Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        GradientType.Radial -> {
            val radius = max(width, height) * 0.6f
            val shader = RadialGradient(
                width / 2f, height / 2f, radius,
                colors, null, Shader.TileMode.CLAMP
            )
            val paint = Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }

        GradientType.Angular -> {
            val shader = SweepGradient(width / 2f, height / 2f, colors, null)
            val paint = Paint().apply {
                isAntiAlias = true
                this.shader = shader
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    if (addNoise) {
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val baseDp = 1f
        val density = (context.resources.displayMetrics.density).coerceAtLeast(1f)
        val noiseSizePx = (baseDp * density).coerceAtLeast(1f)

        val numNoisePoints =
            ((width.toLong() * height.toLong()) / (noiseSizePx.toLong() * noiseSizePx.toLong()) * 0.02f).toInt()
                .coerceAtLeast(200)

        val rnd = Random(System.currentTimeMillis())

        repeat(numNoisePoints) {
            val x = rnd.nextFloat() * width
            val y = rnd.nextFloat() * height

            val alpha = (rnd.nextFloat() * 0.15f).coerceIn(0f, 1f)
            val alphaInt = (alpha * 255).roundToInt().coerceIn(0, 255)
            paint.color = android.graphics.Color.argb(alphaInt, 255, 255, 255)

            val radius = noiseSizePx * (0.6f + rnd.nextFloat() * 1.2f)

            canvas.drawCircle(x, y, radius, paint)
        }
    }
    // stripes on the saved bitmap
    if (addStripes) {
        val paintStripe = Paint().apply { isAntiAlias = true }
        val stripeCount = 18
        val stripeWidth = width.toFloat() / (stripeCount * 2f)

        repeat(stripeCount) { i ->
            val left = i * stripeWidth * 2f
            val right = left + stripeWidth
            val alphaStripe = 0.09f
            val alphaInt = (alphaStripe * 255).roundToInt().coerceIn(0, 255)
            paintStripe.color = android.graphics.Color.argb(alphaInt, 255, 255, 255)
            canvas.drawRect(left, 0f, right, height.toFloat(), paintStripe)
        }
    }
    if (addOverlay) {
        try {
            val overlay = android.graphics.BitmapFactory.decodeResource(
                context.resources,
                R.drawable.overlay_stripes
            )

            val scaled = overlay.scale(width, height)

            canvas.drawBitmap(scaled, 0f, 0f, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    return bmp
}

/**
 * Save a bitmap to MediaStore (Pictures/Waller).
 */
fun saveBitmapToMediaStore(context: Context, bitmap: Bitmap, displayName: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Waller"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val pictures =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                        .toString() + "/Waller"
                val file = File(pictures)
                if (!file.exists()) file.mkdirs()
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val uri: Uri = resolver.insert(collection, contentValues) ?: return false

        resolver.openOutputStream(uri)?.use { out ->
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (!compressed) {
                resolver.delete(uri, null, null)
                return false
            }
        } ?: run {
            resolver.delete(uri, null, null)
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        } else {
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
 * Apply bitmap as wallpaper.
 */
fun tryApplyWallpaper(
    context: Context,
    bitmap: Bitmap,
    flags: Int = WallpaperManager.FLAG_SYSTEM
): Boolean {
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