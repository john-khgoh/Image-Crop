package com.example.imagecrop

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import com.example.imagecrop.ui.theme.ImageCropTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ImageCropTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    imageCrop()
                }
            }
        }
    }
}

@Composable
fun imageCrop(modifier: Modifier = Modifier) {
    var context = LocalContext.current
    var uiViewModel = UiViewModel(true, context = context)
    var imageBitmap = ImageBitmap.imageResource(R.drawable.cat)
    var bitmap =  imageBitmap.asAndroidBitmap()
    crop(1.0f,bitmap,uiViewModel)

}

data class UiViewModel(var openDialog: Boolean, var context: Context): ViewModel() {
    private val _uiState = MutableStateFlow(UiState(context = context))
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
}

data class UiState constructor(
    var allowCropBool: MutableState<Boolean> = mutableStateOf(false),
    var cropResultReady: MutableState<Boolean> = mutableStateOf(false),
    var errorDialog: MutableState<Boolean> = mutableStateOf(false),
    var cropResult: MutableState<Bitmap?> = mutableStateOf(null),
    var context: Context,
    var westFlag: MutableState<Boolean> = mutableStateOf(false),
    var northFlag: MutableState<Boolean> = mutableStateOf(false),
    var eastFlag: MutableState<Boolean> = mutableStateOf(false),
    var southFlag: MutableState<Boolean> = mutableStateOf(false),
    var offsetX: MutableState<Float> = mutableStateOf(0f),
    var offsetY: MutableState<Float> = mutableStateOf(0f),
    var cropSquareX: MutableState<Int> = mutableStateOf(400),
    var cropSquareY: MutableState<Int> = mutableStateOf(400),
    var thresholdX: MutableState<Int> = mutableStateOf(65),
    var thresholdY: MutableState<Int> = mutableStateOf(65),
    //var offsetXincrement: MutableState<Float> = mutableStateOf(0f),
    //var offsetYincrement: MutableState<Float> = mutableStateOf(0f),
    //var cropSquareXincrement: MutableState<Int> = mutableStateOf(10),
    //var cropSquareYincrement: MutableState<Int> = mutableStateOf(10),
)

