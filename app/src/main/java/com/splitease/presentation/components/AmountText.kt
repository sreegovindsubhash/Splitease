package com.splitease.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.splitease.util.MoneyFormatter

/**
 * Renders a monetary amount as formatted text.
 * [amountMinorUnits] must be Long (never Double/Float).
 * Uses [MoneyFormatter] for locale-aware, currency-aware display.
 */
@Composable
fun AmountText(
    amountMinorUnits: Long,
    currencyCode: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = Color.Unspecified,
) {
    val formatted = MoneyFormatter.format(amountMinorUnits, currencyCode)
    Text(
        text = formatted,
        style = style,
        color = color,
        modifier = modifier.semantics {
            contentDescription = "$formatted total"
        },
    )
}
