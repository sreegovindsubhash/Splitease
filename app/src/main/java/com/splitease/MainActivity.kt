package com.splitease

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.splitease.presentation.navigation.SplitEaseNavGraph
import com.splitease.presentation.theme.SplitEaseTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SplitEaseTheme {
                SplitEaseNavGraph()
            }
        }
    }
}
