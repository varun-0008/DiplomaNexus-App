package com.example.diplomanexus

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.diplomanexus.theme.DiplomaNexusTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    android.webkit.WebView.enableSlowWholeDocumentDraw()
    super.onCreate(savedInstanceState)
    Log.e("MainActivity", "onCreate called!")

    enableEdgeToEdge()
    setContent {
      DiplomaNexusTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
