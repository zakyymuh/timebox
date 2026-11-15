package dev.zakyymuh.timebox.ui.sales

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.core.motor.Money
import dev.zakyymuh.timebox.ui.theme.Fresh

/** The rose frame a for-sale card or photo wears, matching its [corner]. */
fun Modifier.saleBorder(corner: Dp): Modifier = border(3.dp, Fresh.Green, RoundedCornerShape(corner))

/** The asking price in a white pill, for the bottom-left of a for-sale photo. */
@Composable
fun PricePill(price: Long, modifier: Modifier = Modifier) {
    Text(
        Money.rupiah(price),
        color = Fresh.GreenDeep.takeUnless { Fresh.Dark } ?: Fresh.Green,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .background(Color.White, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/**
 * A 44dp white circle with a rose share mark and a light shadow, laid inside a for-sale
 * photo's top-right corner. Shares the plant as a story; its tap never reaches the card under it.
 */
@Composable
fun StoryShareButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val rose = Fresh.Green
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(44.dp)
            .shadow(3.dp, CircleShape)
            .clip(CircleShape)
            .background(Color.White)
            .clickable(onClickLabel = "Share as a story", role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Share as a story" },
    ) {
        Canvas(Modifier.size(20.dp)) {
            val line = size.minDimension * 0.1f
            val dot = size.minDimension * 0.14f
            val top = Offset(size.width * 0.76f, size.height * 0.2f)
            val mid = Offset(size.width * 0.24f, size.height * 0.5f)
            val bottom = Offset(size.width * 0.76f, size.height * 0.8f)
            drawLine(rose, mid, top, strokeWidth = line, cap = StrokeCap.Round)
            drawLine(rose, mid, bottom, strokeWidth = line, cap = StrokeCap.Round)
            listOf(top, mid, bottom).forEach {
                drawCircle(Color.White, radius = dot, center = it)
                drawCircle(rose, radius = dot, center = it, style = Stroke(width = line))
            }
        }
    }
}
