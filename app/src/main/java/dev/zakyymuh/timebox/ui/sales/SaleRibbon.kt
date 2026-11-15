package dev.zakyymuh.timebox.ui.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zakyymuh.timebox.core.motor.Money
import dev.zakyymuh.timebox.ui.theme.Fresh

/**
 * Laid over a photo that is up for sale: a dusty rose "SALE" band across the top-right
 * corner, and the asking price in a white pill at the bottom (none when no price is set).
 * Fills the photo's box, so put it in the same [Box] as the photo.
 */
@Composable
fun BoxScope.SaleRibbon(price: Long?) {
    Box(Modifier.matchParentSize().clipToBounds()) {
        Text(
            "SALE",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = 18.dp)
                .rotate(45f)
                .width(130.dp)
                .background(Fresh.Green)
                .padding(vertical = 3.dp),
        )
        if (price != null) {
            Text(
                Money.rupiah(price),
                color = Fresh.GreenDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .background(Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** The rose frame a for-sale card wears, matching the card's [corner]. */
fun Modifier.saleBorder(corner: Dp): Modifier = border(3.dp, Fresh.Green, RoundedCornerShape(corner))
