package com.example.datatransferbluetooth.presentation


import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

typealias NavigationAction=()->Unit

@Composable
fun CustomAppBar(title:String?=null,
                 navigationIcon: ImageVector?=null,
                 navigationAction:NavigationAction?=null) {
    val titleText=title ?: "Aplicación Bluetooth"
    if (navigationIcon!=null&&navigationAction!=null){
        TopAppBar(
            title = { Text(titleText) },
            navigationIcon = {
                IconButton(onClick = {
                    navigationAction()
                }) {
                    Icon(navigationIcon, contentDescription = "")
                }
            },
            backgroundColor = MaterialTheme.colors.primary
        )
    }else{
        TopAppBar(
            title = { Text(titleText) },
            backgroundColor = MaterialTheme.colors.primary
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