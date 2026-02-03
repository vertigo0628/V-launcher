package com.example.launcher.compose

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
import com.example.launcher.ui.HomeViewModel

@Composable
fun WeatherWidget(
    state: HomeViewModel.WeatherState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Use Image with painterResource for now, assuming vector drawable or existing resource
        // If state.iconRes is a system ID that doesn't exist as painter, this mimics simple logic
        // Ideally we map condition to local resources
        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_day), // Weather icon
            contentDescription = "Weather",
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = state.temp,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = state.condition,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp
        )
    }
}
