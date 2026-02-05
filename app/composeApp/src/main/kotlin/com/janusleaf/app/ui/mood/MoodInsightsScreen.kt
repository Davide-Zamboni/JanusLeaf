package com.janusleaf.app.ui.mood

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.janusleaf.app.domain.model.JournalPreview
import com.janusleaf.app.ui.components.MainBottomBar
import com.janusleaf.app.ui.components.MainTab
import com.janusleaf.app.ui.preview.PreviewSamples
import com.janusleaf.app.ui.theme.JanusLeafTheme
import com.janusleaf.app.ui.util.formatAxisDate
import com.janusleaf.app.ui.util.formatShortDate
import com.janusleaf.app.ui.util.moodColor
import com.janusleaf.app.presentation.viewmodel.MoodInsightsViewModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

private enum class TimePeriod(val label: String, val days: Int) {
    WEEK("W", 7),
    MONTH("M", 30),
    SIX_MONTHS("6M", 180),
    YEAR("Y", 365)
}

private data class MoodDataPoint(
    val date: LocalDate,
    val score: Int,
    val entryTitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodInsightsScreen(
    viewModel: MoodInsightsViewModel,
    onProfileClick: () -> Unit,
    onNavigateToJournal: () -> Unit,
    onNavigateToInsights: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isPreview = LocalInspectionMode.current
    LaunchedEffect(Unit) {
        if (!isPreview && uiState.entries.isEmpty()) {
            viewModel.loadEntries()
        }
    }

    Scaffold(
        bottomBar = {
            MainBottomBar(
                selectedTab = MainTab.Insights,
                onSelectJournal = onNavigateToJournal,
                onSelectInsights = onNavigateToInsights
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            MoodInsightsContent(entries = uiState.entries, onProfileClick = onProfileClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodInsightsContent(
    entries: List<JournalPreview>,
    onProfileClick: () -> Unit
) {
    var selectedPeriod by remember { mutableStateOf(TimePeriod.SIX_MONTHS) }
    var selectedPoint by remember { mutableStateOf<MoodDataPoint?>(null) }
    val colorScheme = MaterialTheme.colorScheme

    val moodData = remember(entries, selectedPeriod) {
        buildMoodData(entries, selectedPeriod)
    }

    val averageMood = remember(moodData) {
        if (moodData.isEmpty()) 0.0 else moodData.map { it.score }.average()
    }

    val trend = remember(moodData, colorScheme) { computeTrend(moodData, colorScheme) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "State of Mind", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        text = dateRangeText(selectedPeriod),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onProfileClick) {
                    Icon(Icons.Filled.AccountCircle, contentDescription = null)
                }
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                TimePeriod.values().forEach { period ->
                    SegmentedButton(
                        selected = selectedPeriod == period,
                        onClick = { selectedPeriod = period },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = period.ordinal,
                            count = TimePeriod.values().size
                        )
                    ) {
                        Text(period.label)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "TOTAL",
                    value = moodData.size.toString(),
                    subtitle = "entries",
                    icon = Icons.Filled.Assessment,
                    accent = colorScheme.primary
                )
                StatCard(
                    title = "AVERAGE",
                    value = String.format("%.1f", averageMood),
                    subtitle = "mood",
                    icon = Icons.Filled.Favorite,
                    accent = moodColor(averageMood.toInt(), colorScheme)
                )
                StatCard(
                    title = "TREND",
                    value = trend.emoji,
                    subtitle = trend.label,
                    icon = trend.icon,
                    accent = trend.color
                )
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Mood Over Time", style = MaterialTheme.typography.titleLarge)
                    AnimatedVisibility(visible = selectedPoint != null) {
                        selectedPoint?.let { point ->
                            Text(
                                text = "${point.score} - ${formatShortDate(point.date.toJava())}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (moodData.isEmpty()) {
                        EmptyChartPlaceholder()
                    } else {
                        MoodChart(
                            data = moodData,
                            onSelect = { selectedPoint = it },
                            colorScheme = colorScheme
                        )
                    }
                    AxisLabels(period = selectedPeriod)
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Mood Breakdown", style = MaterialTheme.typography.titleLarge)
                    MoodCategoryRow(
                        title = "Daily Moods",
                        count = moodData.size,
                        icon = Icons.Filled.Assessment,
                        color = colorScheme.secondary
                    )
                    MoodCategoryRow(
                        title = "High Energy",
                        count = moodData.count { it.score >= 7 },
                        icon = Icons.Filled.FlashOn,
                        color = colorScheme.primary
                    )
                    MoodCategoryRow(
                        title = "Low Energy",
                        count = moodData.count { it.score <= 4 },
                        icon = Icons.Filled.NightsStay,
                        color = colorScheme.tertiary
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(120.dp)) }
    }
}

private fun buildMoodData(entries: List<JournalPreview>, period: TimePeriod): List<MoodDataPoint> {
    val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val startDate = endDate.minus(period.days, DateTimeUnit.DAY)
    return entries.mapNotNull { entry ->
        val score = entry.moodScore ?: return@mapNotNull null
        if (entry.entryDate < startDate || entry.entryDate > endDate) return@mapNotNull null
        MoodDataPoint(
            date = entry.entryDate,
            score = score,
            entryTitle = entry.title
        )
    }.sortedBy { it.date }
}

private data class TrendResult(
    val label: String,
    val emoji: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

private fun computeTrend(data: List<MoodDataPoint>, colorScheme: ColorScheme): TrendResult {
    if (data.size < 2) {
        return TrendResult("stable", "Stable", Icons.AutoMirrored.Filled.TrendingFlat, colorScheme.primary)
    }
    val mid = data.size / 2
    val first = data.take(mid).map { it.score }.average()
    val second = data.drop(mid).map { it.score }.average()
    val diff = second - first
    return when {
        diff > 0.5 -> TrendResult("improving", "Up", Icons.AutoMirrored.Filled.TrendingUp, colorScheme.primary)
        diff < -0.5 -> TrendResult("declining", "Down", Icons.AutoMirrored.Filled.TrendingDown, colorScheme.error)
        else -> TrendResult("stable", "Stable", Icons.AutoMirrored.Filled.TrendingFlat, colorScheme.primary)
    }
}

@Composable
private fun RowScope.StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = accent)
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoodChart(
    data: List<MoodDataPoint>,
    onSelect: (MoodDataPoint?) -> Unit,
    colorScheme: ColorScheme
) {
    var selectedPoint by remember { mutableStateOf<MoodDataPoint?>(null) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .pointerInput(data) {
                detectDragGestures(
                    onDragEnd = {
                        onSelect(null)
                        selectedPoint = null
                    },
                    onDragCancel = {
                        onSelect(null)
                        selectedPoint = null
                    }
                ) { change, _ ->
                    val width = size.width
                    val height = size.height
                    val points = data.mapIndexed { index, point ->
                        val x = if (data.size == 1) width / 2f else width * index / (data.size - 1f)
                        val y = height * (1f - ((point.score - 1) / 9f))
                        point to Offset(x, y)
                    }
                    val nearest = points.minByOrNull { abs(it.second.x - change.position.x) }?.first
                    selectedPoint = nearest
                    onSelect(nearest)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val points = data.mapIndexed { index, point ->
                val x = if (data.size == 1) width / 2f else width * index / (data.size - 1)
                val y = height * (1f - ((point.score - 1) / 9f))
                Offset(x, y)
            }

            for (i in 0..4) {
                val y = height * i / 4f
                drawLine(
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1f
                )
            }

            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = colorScheme.primary,
                    style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                val fillPath = Path().apply {
                    moveTo(points.first().x, height)
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                    lineTo(points.last().x, height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(colorScheme.primary.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
            }

            points.forEachIndexed { index, point ->
                val score = data[index].score
                val isSelected = selectedPoint?.date == data[index].date
                val radius = if (isSelected) 8f else 5f
                drawCircle(
                    color = moodColor(score, colorScheme),
                    radius = radius,
                    center = point
                )
                drawCircle(
                    color = colorScheme.surface,
                    radius = radius + 2f,
                    center = point,
                    style = Stroke(width = 1.5f)
                )
            }
        }
    }
}

@Composable
private fun AxisLabels(period: TimePeriod) {
    val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val startDate = endDate.minus(period.days, DateTimeUnit.DAY)
    val labels = (0 until 5).map { idx ->
        val fraction = idx / 4f
        val dayOffset = (period.days * fraction).toInt()
        startDate.plus(dayOffset, DateTimeUnit.DAY)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { date ->
            Text(
                text = formatAxisDate(date.toJava(), period == TimePeriod.WEEK),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MoodCategoryRow(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        )
        Text(
            text = if (count > 0) "$count entries" else "--",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "No mood data yet", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = "Start journaling to see your mood patterns",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun dateRangeText(period: TimePeriod): String {
    val endDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val startDate = endDate.minus(period.days, DateTimeUnit.DAY)
    return "${formatShortDate(startDate.toJava())} - ${formatShortDate(endDate.toJava())}"
}

private fun LocalDate.toJava(): java.time.LocalDate {
    return java.time.LocalDate.of(year, monthNumber, dayOfMonth)
}

@Preview
@Composable
private fun MoodInsightsPreview() {
    JanusLeafTheme {
        MoodInsightsContent(
            entries = PreviewSamples.journalPreviewList(),
            onProfileClick = {}
        )
    }
}
