package com.example.datatransferbluetooth.presentation


import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

typealias NavigationAction=()->Unit

@Composable
fun CustomAppBar(title: String? = null,
                 navigationIcon: ImageVector? = null,
                 navigationAction: NavigationAction? = null) {
    val titleText = title ?: "BlueShare"
    if (navigationIcon != null && navigationAction != null) {
        TopAppBar(
            title = { Text(titleText, color = Color.White) },
            navigationIcon = {
                IconButton(
                    onClick = {
                        navigationAction()
                    }
                )
                {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Volver",
                        tint = Color.White
                    )
                }
            },
            backgroundColor = Color(0xFF1B0A82)
        )
    } else {
        TopAppBar(
            title = { Text(titleText, color = Color.White) },
            backgroundColor = Color(0xFF1B0A82)
        )
    }
}

@Preview(
    showBackground = true
)
@Composable
fun CustomAppBarPreview() {
    CustomAppBar("Aplicación Bluetooth")
}