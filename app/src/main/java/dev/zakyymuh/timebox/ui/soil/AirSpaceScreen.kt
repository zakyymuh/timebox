package dev.zakyymuh.timebox.ui.soil

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import dev.zakyymuh.timebox.ui.common.RoundButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dev.zakyymuh.timebox.core.soil.AirResult
import dev.zakyymuh.timebox.core.soil.AirTest
import dev.zakyymuh.timebox.core.soil.AirTestForm
import dev.zakyymuh.timebox.core.soil.SampleCheck
import dev.zakyymuh.timebox.core.soil.SampleStatus
import dev.zakyymuh.timebox.data.SoilRepository
import dev.zakyymuh.timebox.ui.common.SectionTitle
import dev.zakyymuh.timebox.ui.common.SoftCard
import dev.zakyymuh.timebox.ui.common.TopBar
import dev.zakyymuh.timebox.ui.common.softFieldColors
import dev.zakyymuh.timebox.ui.theme.Fresh
import dev.zakyymuh.timebox.ui.theme.Label
import dev.zakyymuh.timebox.ui.theme.Small
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

/** Just the reminder state, for the card on the plants page. */
class SoilStatusViewModel(repo: SoilRepository) : ViewModel() {
    val status: StateFlow<SampleStatus?> = repo.statusFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

class AirSpaceViewModel(private val repo: SoilRepository) : ViewModel() {
    private val _form = MutableStateFlow(AirTestForm())
    val form: StateFlow<AirTestForm> = _form.asStateFlow()

    val tests: StateFlow<List<AirTest>> = repo.testsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val status: StateFlow<SampleStatus?> = repo.statusFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun update(form: AirTestForm) {
        _form.value = form
    }

    /** Saves the test and clears the drained/poured readings; the mix and pot stay for the next check. */
    fun save() {
        val form = _form.value
        if (!form.isValid) return
        _form.value = form.copy(drainedMl = null, pouredMl = null)
        viewModelScope.launch { repo.save(form) }
    }

}

/** Every check, newest first, to read back. Nothing here can be removed. */
class SoilHistoryViewModel(repo: SoilRepository) : ViewModel() {
    val tests: StateFlow<List<AirTest>?> = repo.testsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/** The air-filled porosity calculator, the re-check reminder and how to test. 📋 opens past checks. */
@Composable
fun AirSpaceScreen(viewModel: AirSpaceViewModel, onBack: () -> Unit, onHistory: () -> Unit) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()

    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(
            Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            TopBar(title = "Soil air space", onBack = onBack) {
                RoundButton("📋", onClick = onHistory, description = "Check history")
            }
            status?.let { SoilWaterCard(it, onClick = null) }

            SectionTitle("Calculate")
            Field(form.mix, "Mix name, e.g. Mix A (optional)", number = false) {
                viewModel.update(form.copy(mix = it.take(AirTestForm.MAX_MIX)))
            }
            Spacer(Modifier.height(10.dp))
            Field(form.potMl?.toString() ?: "", "Pot volume (ml)") { viewModel.update(form.copy(potMl = it.toMl())) }
            Spacer(Modifier.height(10.dp))
            Field(form.drainedMl?.toString() ?: "", "Water that drained out (ml)") { viewModel.update(form.copy(drainedMl = it.toMl())) }
            Spacer(Modifier.height(10.dp))
            Field(form.pouredMl?.toString() ?: "", "Water poured in to soak (ml, optional)") { viewModel.update(form.copy(pouredMl = it.toMl())) }
            Problem(form)

            form.result?.let { ResultCard(it) }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::save,
                enabled = form.isValid,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Fresh.Ink,
                    contentColor = Color.White,
                    disabledContainerColor = Fresh.Card,
                    disabledContentColor = Fresh.InkSoft,
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) { Text("Save test", style = MaterialTheme.typography.labelLarge) }

            SectionTitle("How to test")
            SoftCard(corner = 20.dp) {
                listOf(
                    "Plug the pot's drainage hole and fill the empty pot with water to the line you'll fill the mix to. That water, in ml, is the pot volume.",
                    "Empty and dry the pot, then fill it to the same line with dry mix, tapped down lightly as when potting.",
                    "Keep the hole plugged and slowly pour in measured water until it just reaches the surface. That's 'poured' (optional).",
                    "Wait 15–30 minutes, then unplug the hole over a measuring cup and let it drain fully. That's 'drained'.",
                    "Keep this pot as your sample and re-test it every ${SampleCheck.EVERY} waterings: if the air space drops, the mix is compacting or breaking down.",
                ).forEachIndexed { i, step ->
                    Row(Modifier.padding(bottom = 8.dp)) {
                        Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, color = Fresh.Green, modifier = Modifier.width(22.dp))
                        Text(step, style = MaterialTheme.typography.bodyMedium, color = Fresh.Ink)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ResultCard(result: AirResult) {
    SoftCard(color = Fresh.Pastel, corner = 24.dp, modifier = Modifier.padding(top = 14.dp)) {
        Text("Air space", style = Label, color = Fresh.GreenDeep.copy(alpha = 0.75f))
        Text("${result.airPct}%", style = MaterialTheme.typography.displaySmall, color = Fresh.GreenDeep)
        Text(result.verdict, style = MaterialTheme.typography.bodyMedium, color = Fresh.GreenDeep)
        if (result.totalPct != null && result.waterPct != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Total pore space ${result.totalPct}% · holds water in ${result.waterPct}%",
                style = Small,
                color = Fresh.GreenDeep,
            )
        }
    }
}

@Composable
private fun Problem(form: AirTestForm) {
    val pot = form.potMl
    val drained = form.drainedMl
    val poured = form.pouredMl
    val message = when {
        pot != null && pot <= 0 -> "The pot volume must be more than 0"
        pot != null && drained != null && drained > pot -> "More water drained than the pot holds: check the numbers"
        drained != null && poured != null && poured < drained -> "Poured must be at least what drained out"
        pot != null && poured != null && poured > pot -> "Poured can't be more than the pot volume"
        else -> null
    }
    message?.let { Text(it, style = Small, color = Fresh.Error, modifier = Modifier.padding(start = 4.dp, top = 6.dp)) }
}

@Composable
private fun TestRow(test: AirTest) {
    SoftCard(corner = 20.dp, modifier = Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "${test.result.airPct}% air" + (test.result.totalPct?.let { " · $it% total" } ?: ""),
                    style = MaterialTheme.typography.titleMedium,
                    color = Fresh.Ink,
                )
                Text(
                    listOf(test.date.format(dateFormat), test.mix.ifBlank { null }, "${test.drainedMl}/${test.potMl} ml")
                        .filterNotNull().joinToString(" · "),
                    style = Label,
                    color = Fresh.InkSoft,
                )
            }
        }
    }
}

/** Past checks, newest first, read only. */
@Composable
fun SoilHistoryScreen(viewModel: SoilHistoryViewModel, onBack: () -> Unit) {
    val tests by viewModel.tests.collectAsStateWithLifecycle()
    Scaffold(containerColor = Fresh.Page) { insets ->
        Column(Modifier.padding(insets).padding(horizontal = 20.dp).fillMaxSize()) {
            TopBar(title = "Check history", onBack = onBack)
            val list = tests ?: return@Column
            if (list.isEmpty()) {
                Text("No checks yet", style = MaterialTheme.typography.headlineSmall, color = Fresh.InkSoft.copy(alpha = 0.75f))
                Spacer(Modifier.height(8.dp))
                Text("Save a test and it shows here.", style = MaterialTheme.typography.bodyMedium, color = Fresh.InkSoft.copy(alpha = 0.6f))
                return@Column
            }
            LazyColumn {
                items(list, key = { it.id }) { TestRow(it) }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

private fun String.toMl(): Int? = filter(Char::isDigit).take(6).toIntOrNull()

@Composable
private fun Field(value: String, hint: String, number: Boolean = true, onChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(hint, color = Fresh.InkSoft) },
        singleLine = true,
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
        shape = RoundedCornerShape(20.dp),
        colors = softFieldColors(),
        modifier = Modifier.fillMaxWidth(),
    )
}
