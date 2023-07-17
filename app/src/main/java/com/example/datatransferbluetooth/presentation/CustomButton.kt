package com.example.datatransferbluetooth.presentation

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CustomButton(
    text: String,
    modifier: Modifier = Modifier,
    onClickAction: () -> Unit
) {
    Button(
        onClick = { onClickAction.invoke() },
        modifier = modifier.padding(horizontal = 10.dp)
            .width(320.dp)
            .height(80.dp),
        shape = MaterialTheme.shapes.large.copy(CornerSize(50)),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A06F9),
            contentColor = Color.White
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge
        )
    }
}
