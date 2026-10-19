package com.weatherfocus.app.ui.month

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.weatherfocus.app.data.model.DayForecast
import com.weatherfocus.app.ui.WeatherFormat
import com.weatherfocus.app.ui.WeatherViewModel
import com.weatherfocus.app.ui.components.PremiumWeatherIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthScreen(viewModel: WeatherViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    val days = state.bundle?.monthDays.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Next Month") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (days.any { it.isOutlookEstimate }) {
                Text(
                    "Days beyond ~16 are a historical outlook (average of the last 3 years for that date), not a real forecast.",
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                items(days) { day -> MonthDayRow(day, state.settings.useFahrenheit) }
            }
        }
    }
}

@Composable
private fun MonthDayRow(day: DayForecast, useFahrenheit: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (day.isOutlookEstimate) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.4f)) {
                Text("${day.dayLabel}, ${day.dateLabel}", fontWeight = FontWeight.SemiBold)
                Text(
                    day.description + if (day.isOutlookEstimate) " \u00B7 outlook" else "",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            PremiumWeatherIcon(day.conditionGroup, size = 34.dp, modifier = Modifier.weight(0.6f))
            Text(
                "${WeatherFormat.tempNoUnit(day.maxTemp, useFahrenheit)} / ${WeatherFormat.tempNoUnit(day.minTemp, useFahrenheit)}",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Row(modifier = Modifier.weight(0.6f), verticalAlignment = Alignment.CenterVertically) {
                day.rainChancePercent?.let {
                    Icon(
                        Icons.Outlined.WaterDrop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        " $it%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
