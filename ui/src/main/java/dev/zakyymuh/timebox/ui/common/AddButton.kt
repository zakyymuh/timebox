package dev.zakyymuh.timebox.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.zakyymuh.timebox.ui.theme.Fresh

/** Room a scrolling list leaves at its end, so the floating [AddButton] never covers the last row. */
val AddButtonClearance = 104.dp

/**
 * The floating "+ Add …" pill at the bottom centre of a list screen. Use it as a Scaffold's
 * floatingActionButton with FabPosition.Center.
 */
@Composable
fun AddButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = Fresh.Ink, contentColor = Fresh.Page),
        contentPadding = PaddingValues(horizontal = 24.dp),
        modifier = Modifier.height(56.dp),
    ) {
        Text("+  $label", style = MaterialTheme.typography.labelLarge)
    }
}
