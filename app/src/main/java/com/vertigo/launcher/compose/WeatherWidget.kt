package com.vertigo.launcher.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vertigo.launcher.ui.HomeViewModel

@Composable
fun WeatherWidget(
    state: HomeViewModel.WeatherState,
    modifier: Modifier = Modifier,
    scaleFactor: Float = 1f,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = (16 * scaleFactor).dp, vertical = (8 * scaleFactor).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_day),
            contentDescription = "Weather",
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size((24 * scaleFactor).dp)
        )
        
        Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
        
        Text(
            text = state.temp,
            color = Color.White,
            fontSize = (20 * scaleFactor).sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width((8 * scaleFactor).dp))
        
        Text(
            text = state.condition,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = (14 * scaleFactor).sp
        )
    }
}
