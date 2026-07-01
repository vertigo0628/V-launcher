package com.vertigo.launcher.compose

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun EdgePanel(
    isVisible: Boolean,
    onClose: () -> Unit
) {
    var notesText by remember { mutableStateOf("") }
    var calcInput by remember { mutableStateOf("") }
    var calcResult by remember { mutableStateOf("") }

    // Simple evaluator
    fun evaluateCalc(expr: String) {
        if (expr.isBlank()) {
            calcResult = ""
            return
        }
        try {
            // Very simple evaluator handling +, -, *, /
            // Assumes properly formatted like "2 + 2" or simple parsing
            val sanitized = expr.replace(" ", "")
            if (sanitized.isEmpty()) {
                calcResult = ""
                return
            }
            
            // We use a regex to split by operators
            val operators = Regex("(?<=[-+*/])|(?=[-+*/])")
            val tokens = sanitized.split(operators).filter { it.isNotEmpty() }
            
            if (tokens.size >= 3) {
                var result = tokens[0].toDouble()
                var i = 1
                while (i < tokens.size - 1) {
                    val op = tokens[i]
                    val nextVal = tokens[i + 1].toDouble()
                    when (op) {
                        "+" -> result += nextVal
                        "-" -> result -= nextVal
                        "*" -> result *= nextVal
                        "/" -> result /= nextVal
                    }
                    i += 2
                }
                calcResult = if (result % 1.0 == 0.0) result.toInt().toString() else result.toString()
            } else {
                calcResult = ""
            }
        } catch (e: Exception) {
            calcResult = "Err"
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(
            initialOffsetX = { it }, // Slide in from right edge
            animationSpec = tween(300)
        ) + fadeIn(),
        exit = slideOutHorizontally(
            targetOffsetX = { it },
            animationSpec = tween(300)
        ) + fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterEnd
        ) {
            // Click outside to dismiss
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = onClose
                    )
            )

            // Panel Content
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(300.dp)
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .background(Color(0xE60F172A)) // Semi-transparent dark blue
                    .padding(24.dp)
                    .clickable(enabled = false) {} // Catch clicks on panel itself
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK TOOLS",
                            color = Color(0xFF00F0FF),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = onClose) {
                            Text("✕", color = Color.White, fontSize = 20.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "CALCULATOR",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = calcInput,
                        onValueChange = { 
                            calcInput = it
                            evaluateCalc(it)
                        },
                        placeholder = { Text("e.g. 150 * 0.2", color = Color.Gray) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFBD00FF),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (calcResult.isNotEmpty()) "= $calcResult" else "",
                        color = Color(0xFF10B981),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "QUICK NOTES",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        placeholder = { Text("Jot something down...", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F0FF),
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        maxLines = 10
                    )
                }
            }
        }
    }
}
