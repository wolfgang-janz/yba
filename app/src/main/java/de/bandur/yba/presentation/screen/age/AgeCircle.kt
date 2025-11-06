package de.bandur.yba.presentation.screen.age

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AgeCircle(
    totalBiologicalAge: Double,
    ageDifference: Double
) {
    val ringColor = Brush.sweepGradient(
        listOf(
            Color(0xFF00FF00),
            Color(0xFF88FF88),
            Color(0xFF00FF00),
        )
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(200.dp)
            .drawBehind {
                drawCircle(
                    brush = ringColor,
                    style = Stroke(width = 15f)
                )
            }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", totalBiologicalAge),
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "YBA AGE",
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format("%.1f", kotlin.math.abs(ageDifference)),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (ageDifference < 0) Color.Green else Color.Red
            )
            Text(
                text = if (ageDifference < 0) "YEARS YOUNGER" else "YEARS OLDER",
                style = MaterialTheme.typography.caption,
                color = Color.Gray
            )
        }
    }
}
