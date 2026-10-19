package com.weatherfocus.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weatherfocus.app.data.model.CustomAlertRules
import com.weatherfocus.app.data.model.DoNotDisturbSettings
import com.weatherfocus.app.data.model.ThemeMode
import com.weatherfocus.app.data.prefs.CountryCatalog
import com.weatherfocus.app.notification.NotificationHelper
import com.weatherfocus.app.ui.WeatherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: WeatherViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var cityQuery by remember(state.settings.cityName) { mutableStateOf(state.settings.cityName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            SectionTitle("Appearance")
            ThemeModePicker(current = state.settings.themeMode, onSelect = { viewModel.onThemeModeSelected(it) })

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Location")

            ExposedDropdownMenuBox(expanded = countryMenuExpanded, onExpandedChange = { countryMenuExpanded = it }) {
                OutlinedTextField(
                    value = state.settings.countryLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Country") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = countryMenuExpanded,
                    onDismissRequest = { countryMenuExpanded = false }
                ) {
                    CountryCatalog.ALL.forEach { entry ->
                        DropdownMenuItem(
                            text = { Text(entry.label) },
                            onClick = {
                                viewModel.onCountrySelected(entry.code, entry.label)
                                countryMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer4()

            Column {
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = {
                        cityQuery = it
                        viewModel.onCityQueryChanged(it)
                    },
                    label = { Text("City") },
                    placeholder = { Text("Start typing a city name...") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.citySuggestions.isNotEmpty()) {
                    Card(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Column {
                            state.citySuggestions.take(6).forEach { place ->
                                DropdownMenuItem(
                                    text = { Text(place.displayLabel) },
                                    onClick = {
                                        cityQuery = place.name
                                        viewModel.onCitySelected(place)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Units")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Use Fahrenheit", modifier = Modifier.weight(1f))
                Switch(checked = state.settings.useFahrenheit, onCheckedChange = { viewModel.onFahrenheitToggled(it) }, colors = visibleSwitchColors())
            }

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Do Not Disturb")
            Text(
                "Silence weather-alert notifications during a daily quiet-hours window.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer4()
            DoNotDisturbEditor(dnd = state.settings.doNotDisturb, onChange = { viewModel.updateDoNotDisturb(it) })

            Spacer4()
            HorizontalDivider()
            Spacer4()

            SectionTitle("Notifications")
            Text(
                "If custom alerts are enabled below but you never see a notification, first make sure this phone actually delivers notifications for this app (Android may block them silently even with everything configured correctly). Tap the button to send a test notification right now, bypassing Do Not Disturb.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer4()
            val context = LocalContext.current
            Button(
                onClick = { NotificationHelper.showTestNotification(context) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                )
            ) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("Send test notification")
            }
            Text(
                "If nothing arrives: check your phone's system Settings > Apps > Weather > Notifications is turned on, and that battery optimization isn't restricting this app in the background.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer4()
            HorizontalDivider()
            Spacer4()

            CollapsibleAlertRulesSection(
                savedRules = state.settings.customAlertRules,
                onSave = { viewModel.updateAlertRules(it) }
            )
        }
    }
}

/**
 * "Custom Weather Alerts" as a collapsible card: starts collapsed so opening it doesn't push the user
 * all the way down the Settings screen. While expanded, edits are kept in local draft state only (not
 * written on every slider tick) - tapping Save commits them all at once and collapses the card back down.
 */
@Composable
private fun CollapsibleAlertRulesSection(savedRules: CustomAlertRules, onSave: (CustomAlertRules) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var draft by remember(expanded) { mutableStateOf(savedRules) }
    val dirty = expanded && draft != savedRules

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Custom Weather Alerts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    if (!expanded) {
                        Text(
                            if (savedRules.enabled) "Enabled \u00B7 tap to edit" else "Disabled \u00B7 tap to edit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer4()
                    AlertRulesEditor(rules = draft, onChange = { draft = it })

                    Spacer4()
                    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        // Explicit colors here on purpose: the default filled/outlined button colors come from
                        // colorScheme.primary, which this app's dark theme sets to a near-black graphite (by
                        // design, for the rest of the UI) - that made "Save" blend invisibly into the dark
                        // background and made "Cancel"'s text (which defaults to the primary color too) almost
                        // unreadable. secondary/onSecondary and onSurface stay high-contrast in both themes.
                        Button(
                            onClick = {
                                onSave(draft)
                                expanded = false
                            },
                            enabled = dirty,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary,
                                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                        androidx.compose.material3.OutlinedButton(
                            onClick = { expanded = false },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeModePicker(current: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
        ThemeChip("System", current == ThemeMode.SYSTEM) { onSelect(ThemeMode.SYSTEM) }
        ThemeChip("Light", current == ThemeMode.LIGHT) { onSelect(ThemeMode.LIGHT) }
        ThemeChip("Dark", current == ThemeMode.DARK) { onSelect(ThemeMode.DARK) }
    }
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null
    )
}

@Composable
private fun DoNotDisturbEditor(dnd: DoNotDisturbSettings, onChange: (DoNotDisturbSettings) -> Unit) {
    RowSwitch("Enable Do Not Disturb", dnd.enabled) { onChange(dnd.copy(enabled = it)) }
    if (!dnd.enabled) return

    Spacer4()
    ExpandableParamSlider(
        label = { "From ${formatHour(it)}" },
        value = dnd.startHour,
        range = 0..23
    ) { onChange(dnd.copy(startHour = it)) }

    ExpandableParamSlider(
        label = { "To ${formatHour(it)}" },
        value = dnd.endHour,
        range = 0..23
    ) { onChange(dnd.copy(endHour = it)) }
}

private fun formatHour(hour: Int): String = String.format(java.util.Locale.US, "%02d:00", hour)

/**
 * One parameter, one collapsible row: tap the row to reveal its slider, drag it, and it auto-collapses
 * back to a compact summary the moment you let go - so a settings section with several sliders doesn't
 * turn into a wall of always-open controls, and each parameter manages its own expand/collapse state
 * independently of the others.
 *
 * The label is driven off [dragValue] while expanded (updated on every [Slider.onValueChange] frame, not
 * just when the drag finishes), so the number next to the row updates live as you move the thumb instead
 * of only jumping once you stop - this is what was previously missing since the label read the *committed*
 * value only.
 */
@Composable
private fun ExpandableParamSlider(
    label: (Int) -> String,
    value: Int,
    range: IntRange,
    onCommit: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var dragValue by remember(value, expanded) { mutableStateOf(value.toFloat()) }
    val displayValue = if (expanded) dragValue.toInt() else value

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 6.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(label(displayValue), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        AnimatedVisibility(visible = expanded) {
            // Explicit colors: the default Slider colors come from colorScheme.primary, which this app's
            // dark theme sets to a near-black graphite - against the dark background that made the track
            // and thumb nearly invisible. secondary (a warm amber/yellow in both themes) stays visible.
            Slider(
                value = dragValue,
                onValueChange = { dragValue = it },
                onValueChangeFinished = {
                    onCommit(dragValue.toInt())
                    expanded = false
                },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                steps = (range.last - range.first - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    activeTickColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.5f),
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    inactiveTickColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
            )
        }
    }
}

@Composable
private fun AlertRulesEditor(rules: CustomAlertRules, onChange: (CustomAlertRules) -> Unit) {
    RowSwitch("Enable custom alerts", rules.enabled) { onChange(rules.copy(enabled = it)) }
    if (!rules.enabled) return

    Spacer4()
    ExpandableParamSlider(
        label = { "Check the next ${it}h" },
        value = rules.horizonHours,
        range = 1..48
    ) { onChange(rules.copy(horizonHours = it)) }

    ExpandableParamSlider(
        label = { "Warn ${it}h before" },
        value = rules.leadTimeHours,
        range = 1..24
    ) { onChange(rules.copy(leadTimeHours = it)) }

    Spacer4()
    RowSwitch("Temperature above ${rules.tempAboveValue.toInt()}\u00B0C", rules.tempAboveEnabled) { onChange(rules.copy(tempAboveEnabled = it)) }
    if (rules.tempAboveEnabled) {
        ExpandableParamSlider(label = { "$it\u00B0C" }, value = rules.tempAboveValue.toInt(), range = -10..45) {
            onChange(rules.copy(tempAboveValue = it.toDouble()))
        }
    }

    RowSwitch("Temperature below ${rules.tempBelowValue.toInt()}\u00B0C", rules.tempBelowEnabled) { onChange(rules.copy(tempBelowEnabled = it)) }
    if (rules.tempBelowEnabled) {
        ExpandableParamSlider(label = { "$it\u00B0C" }, value = rules.tempBelowValue.toInt(), range = -30..20) {
            onChange(rules.copy(tempBelowValue = it.toDouble()))
        }
    }

    RowSwitch("UV index above ${rules.uvIndexValue.toInt()}", rules.uvIndexEnabled) { onChange(rules.copy(uvIndexEnabled = it)) }
    if (rules.uvIndexEnabled) {
        ExpandableParamSlider(label = { "UV $it" }, value = rules.uvIndexValue.toInt(), range = 1..12) {
            onChange(rules.copy(uvIndexValue = it.toDouble()))
        }
    }

    RowSwitch("Wind speed above ${rules.windSpeedValue.toInt()} km/h", rules.windSpeedEnabled) { onChange(rules.copy(windSpeedEnabled = it)) }
    if (rules.windSpeedEnabled) {
        ExpandableParamSlider(label = { "$it km/h" }, value = rules.windSpeedValue.toInt(), range = 5..100) {
            onChange(rules.copy(windSpeedValue = it.toDouble()))
        }
    }

    RowSwitch("Rain probability above ${rules.rainProbValue}%", rules.rainProbEnabled) { onChange(rules.copy(rainProbEnabled = it)) }
    if (rules.rainProbEnabled) {
        ExpandableParamSlider(label = { "$it%" }, value = rules.rainProbValue, range = 10..100) {
            onChange(rules.copy(rainProbValue = it))
        }
    }

    RowSwitch("Thunderstorm expected", rules.thunderstormEnabled) { onChange(rules.copy(thunderstormEnabled = it)) }
    RowSwitch("Snow expected", rules.snowEnabled) { onChange(rules.copy(snowEnabled = it)) }

    // ---------------- Winter / Nordic rules: each has its own value you set ----------------
    Spacer4()
    Text("\u2744\uFE0F Winter & Nordic alerts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    Text(
        "Tap a value to set the number that triggers the notification.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer4()

    RowSwitch("\uD83E\uDDCA Black ice risk (wet, at or below ${rules.blackIceMaxTemp.toInt()}\u00B0C)", rules.blackIceEnabled) { onChange(rules.copy(blackIceEnabled = it)) }
    if (rules.blackIceEnabled) {
        ExpandableParamSlider(label = { "Wet weather at or below $it\u00B0C" }, value = rules.blackIceMaxTemp.toInt(), range = -5..5) {
            onChange(rules.copy(blackIceMaxTemp = it.toDouble()))
        }
    }

    RowSwitch("\uD83E\uDDCA Freezing rain (rain at or below ${rules.freezingRainMaxTemp.toInt()}\u00B0C)", rules.freezingRainEnabled) { onChange(rules.copy(freezingRainEnabled = it)) }
    if (rules.freezingRainEnabled) {
        ExpandableParamSlider(label = { "Rain at or below $it\u00B0C" }, value = rules.freezingRainMaxTemp.toInt(), range = -5..3) {
            onChange(rules.copy(freezingRainMaxTemp = it.toDouble()))
        }
    }

    RowSwitch("\u2744\uFE0F Frost arriving (drops to ${rules.frostValue.toInt()}\u00B0C or below)", rules.frostArrivesEnabled) { onChange(rules.copy(frostArrivesEnabled = it)) }
    if (rules.frostArrivesEnabled) {
        ExpandableParamSlider(label = { "Drops to $it\u00B0C or below" }, value = rules.frostValue.toInt(), range = -10..5) {
            onChange(rules.copy(frostValue = it.toDouble()))
        }
    }

    RowSwitch("\uD83C\uDF28\uFE0F Heavy snowfall (\u2265 ${rules.heavySnowValue.toInt()} cm/h)", rules.heavySnowEnabled) { onChange(rules.copy(heavySnowEnabled = it)) }
    if (rules.heavySnowEnabled) {
        ExpandableParamSlider(label = { "$it cm of snow per hour" }, value = rules.heavySnowValue.toInt(), range = 1..10) {
            onChange(rules.copy(heavySnowValue = it.toDouble()))
        }
    }

    RowSwitch("\uD83C\uDF2C\uFE0F Snowstorm (snow + wind \u2265 ${rules.snowstormWindValue.toInt()} km/h)", rules.snowstormEnabled) { onChange(rules.copy(snowstormEnabled = it)) }
    if (rules.snowstormEnabled) {
        ExpandableParamSlider(label = { "Snow with wind \u2265 $it km/h" }, value = rules.snowstormWindValue.toInt(), range = 10..100) {
            onChange(rules.copy(snowstormWindValue = it.toDouble()))
        }
    }

    RowSwitch("\uD83D\uDCA8 Wind gusts above ${rules.windGustValue.toInt()} km/h", rules.windGustEnabled) { onChange(rules.copy(windGustEnabled = it)) }
    if (rules.windGustEnabled) {
        ExpandableParamSlider(label = { "Gusts \u2265 $it km/h" }, value = rules.windGustValue.toInt(), range = 20..150) {
            onChange(rules.copy(windGustValue = it.toDouble()))
        }
    }

    RowSwitch("\uD83E\uDD76 Wind chill below ${rules.windChillValue.toInt()}\u00B0C", rules.windChillEnabled) { onChange(rules.copy(windChillEnabled = it)) }
    if (rules.windChillEnabled) {
        ExpandableParamSlider(label = { "Feels like \u2264 $it\u00B0C" }, value = rules.windChillValue.toInt(), range = -40..5) {
            onChange(rules.copy(windChillValue = it.toDouble()))
        }
    }

    RowSwitch("\uD83C\uDF2B\uFE0F Low visibility below ${rules.lowVisibilityValue.toInt()} m", rules.lowVisibilityEnabled) { onChange(rules.copy(lowVisibilityEnabled = it)) }
    if (rules.lowVisibilityEnabled) {
        // Slider works in 100 m steps so it doesn't need thousands of tick marks.
        ExpandableParamSlider(label = { "Visibility \u2264 ${it * 100} m" }, value = (rules.lowVisibilityValue / 100).toInt(), range = 1..50) {
            onChange(rules.copy(lowVisibilityValue = it * 100.0))
        }
    }

    RowSwitch("\uD83D\uDCC9 Rapid temperature drop (\u2265 ${rules.rapidDropValue.toInt()}\u00B0C in 6h)", rules.rapidDropEnabled) { onChange(rules.copy(rapidDropEnabled = it)) }
    if (rules.rapidDropEnabled) {
        ExpandableParamSlider(label = { "Drop of $it\u00B0C within 6 hours" }, value = rules.rapidDropValue.toInt(), range = 2..25) {
            onChange(rules.copy(rapidDropValue = it.toDouble()))
        }
    }

    Spacer4()
    androidx.compose.material3.OutlinedButton(
        onClick = {
            onChange(
                rules.copy(
                    enabled = true, horizonHours = 48, leadTimeHours = 3,
                    tempBelowEnabled = true, tempBelowValue = -15.0,
                    windSpeedEnabled = true, windSpeedValue = 50.0,
                    snowEnabled = true,
                    blackIceEnabled = true, blackIceMaxTemp = 1.0,
                    freezingRainEnabled = true, freezingRainMaxTemp = 0.0,
                    frostArrivesEnabled = true, frostValue = 0.0,
                    heavySnowEnabled = true, heavySnowValue = 2.0,
                    snowstormEnabled = true, snowstormWindValue = 40.0,
                    windGustEnabled = true, windGustValue = 70.0,
                    windChillEnabled = true, windChillValue = -15.0,
                    lowVisibilityEnabled = true, lowVisibilityValue = 1000.0,
                    rapidDropEnabled = true, rapidDropValue = 8.0
                )
            )
        },
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) { Text("Apply Norway winter preset (then tap Save)") }
}

@Composable
private fun RowSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = visibleSwitchColors())
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
private fun Spacer4() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
}


/**
 * Switch colours that stay clearly readable in dark mode: ON = bright green track with a white thumb,
 * OFF = dim grey track with a visible outline. (The theme's primary colour is near-black in dark mode,
 * so the default switch colours made ON and OFF look almost identical.)
 */
@Composable
private fun visibleSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = androidx.compose.ui.graphics.Color.White,
    checkedTrackColor = androidx.compose.ui.graphics.Color(0xFF2EBD59),
    checkedBorderColor = androidx.compose.ui.graphics.Color(0xFF2EBD59),
    uncheckedThumbColor = androidx.compose.ui.graphics.Color(0xFFB0B3B8),
    uncheckedTrackColor = androidx.compose.ui.graphics.Color(0xFF3A3C40),
    uncheckedBorderColor = androidx.compose.ui.graphics.Color(0xFF8A8D93)
)
