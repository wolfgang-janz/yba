package de.bandur.yba.presentation.screen.age

import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MetricCard(
    title: String,
    value: String,
    ageImpact: Double,
    sliderValue: Float,
    sliderRange: ClosedFloatingPointRange<Float>,
    sliderLabelMin: String,
    sliderLabelMax: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = 4.dp,
        backgroundColor = Color(0xFF2E2E2E)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, style = MaterialTheme.typography.h6, color = Color.White)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, style = MaterialTheme.typography.h5, color = Color.White)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format("%+.1f", ageImpact),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ageImpact < 0) Color.Green else Color.Red
                    )
                    Text(text = "years", style = MaterialTheme.typography.caption, color = Color.Gray)
                }
            }
            Slider(
                value = sliderValue,
                onValueChange = {},
                valueRange = sliderRange,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = sliderLabelMin, style = MaterialTheme.typography.caption, color = Color.Gray)
                Text(text = sliderLabelMax, style = MaterialTheme.typography.caption, color = Color.Gray)
            }
        }
    }
}
