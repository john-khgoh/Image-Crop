package com.example.imagecrop

import android.annotation.SuppressLint

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog


@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun crop(imageScale: Float, bitmap: Bitmap, uiViewModel: UiViewModel) {
    var TAG = "Error"

    var finalWidth = 0
    var finalHeight = 0
    var offsetXImage = 0f
    var offsetYImage = 0f
    var cropSquareX = 400
    var cropSquareY = 400
    //var cropSquareColor = Color(0xFFFEFEFE)
    var cropSquareColor = Color.DarkGray
    var widthRatio: Float = 0f
    var heightRatio: Float = 0f

    val width: Int = bitmap.width
    val height: Int = bitmap.height
    val ratioBitmap = width.toFloat() / height.toFloat() //Original bitmap aspect ratio

    var offset by remember { mutableStateOf(Offset.Zero)}
    var zoom by remember {mutableStateOf(1f)}
    var rect by remember {mutableStateOf(Unit)}
    var flag by remember {mutableStateOf(false)}

    Column() {
        cropTopBar(uiViewModel)
        Spacer(modifier = Modifier.size(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(imageScale)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, gestureZoom, gestureRotate ->
                        val newScale = zoom * gestureZoom
                        offset += pan
                        zoom = newScale
                    }
                }

        ) {
            //The maximum aspect ratio by Canvas
            val ratioMax = size.width / size.height

            finalWidth = size.width.toInt()
            finalHeight = size.height.toInt()

            //Ensuring the image fits within the aspect ratio bounds
            if (ratioMax > ratioBitmap) {
                finalWidth = (size.height * ratioBitmap).toInt()
            } else {
                finalHeight = (size.width / ratioBitmap).toInt()
            }

            //Centering the image
            offsetXImage = (size.width - finalWidth) / 2
            offsetYImage = (size.height - finalHeight) / 2

            drawImage(
                image = bitmap.asImageBitmap(),
                //dstOffset = IntOffset(offsetXImage.toInt(), offsetYImage.toInt()),
                dstSize = IntSize(finalWidth, finalHeight),
            )

            //Set the position to the center of image at the beginning
            if (!flag) {
                offset = Offset(
                    (finalWidth.toFloat() - cropSquareX) / 2,
                    (finalHeight.toFloat() - cropSquareY) / 2
                )
                flag = true
            }

            var offset_x = offset.x
            var offset_y = offset.y

            //Bounding the crop box to the dimensions of the image
            if (offset_x < 0f) {
                offset_x = 0f
            } else if ((offset_x + (cropSquareX * zoom)) > finalWidth) {
                offset_x = finalWidth - (cropSquareX * zoom)
            }
            if (offset_y < 0f) {
                offset_y = 0f
            } else if ((offset_y + (cropSquareY * zoom)) > finalHeight) {
                offset_y = finalHeight - (cropSquareY * zoom)
            }

            //Draw the cropbox
            rect = drawRect(
                color = cropSquareColor,
                topLeft = Offset(offset_x, offset_y),
                size = Size(cropSquareX * zoom, cropSquareY * zoom),
                style = Stroke(2.dp.toPx())
            )
            rect

            widthRatio = (width.toFloat() / finalWidth.toFloat())
            heightRatio = (height.toFloat() / finalHeight.toFloat())

            //Create cropped image
            if (uiViewModel.uiState.value.allowCropBool.value) {
                try {
                    uiViewModel.uiState.value.cropResult.value = Bitmap.createBitmap(
                        bitmap,
                        (widthRatio * offset_x).toInt(), //x = width
                        (heightRatio * offset_y).toInt(), //y = height
                        (widthRatio * cropSquareX * zoom).toInt(),
                        (heightRatio * cropSquareY * zoom).toInt()
                    )
                    uiViewModel.uiState.value.cropResultReady.value = true
                }
                catch(e: Throwable) {
                    Log.e(TAG,"Out of bounds error: " + e.message.toString())
                    uiViewModel.uiState.value.errorDialog.value = true
                    uiViewModel.uiState.value.allowCropBool.value = false
                    //Resets the crop box to the center
                    offset = Offset(
                        (finalWidth.toFloat() - cropSquareX) / 2,
                        (finalHeight.toFloat() - cropSquareY) / 2
                    )
                    zoom = 1f
                }
            }
        }
        when {
            uiViewModel.uiState.value.cropResultReady.value -> {
                resultDialog(uiViewModel = uiViewModel, cropSquareX, cropSquareY, zoom)
            }
        }
        when {
            uiViewModel.uiState.value.errorDialog.value -> {
                errorDialog(uiViewModel = uiViewModel, message = "The crop box is out of bounds.")
            }
        }
    }
}

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cropTopBar(uiViewModel: UiViewModel) {
    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.smallTopAppBarColors(),
        actions = {
            IconButton(
                onClick = {uiViewModel.uiState.value.allowCropBool.value = !uiViewModel.uiState.value.allowCropBool.value}
            ) {
                Column() {
                    Icon(painter = painterResource(R.drawable.crop_24px), contentDescription = null)
                    Text("Crop",style= MaterialTheme.typography.labelSmall)
                }
            }
        }
    )

}

@Composable
fun resultDialog(
    uiViewModel: UiViewModel,
    cropSquareX: Int,
    cropSquareY: Int,
    zoom: Float
) {
    val cardHeight = 157
    val cardWidth = 157

    Dialog(onDismissRequest = {
        uiViewModel.uiState.value.cropResultReady.value = false
        uiViewModel.uiState.value.allowCropBool.value = false}) {
        Card(
            modifier = Modifier
                .background(color = Color.Unspecified, RoundedCornerShape(0.dp))
                .height((cardHeight * zoom).toInt().dp)
                .width((cardWidth * zoom).toInt().dp)

        ) {

            Canvas(modifier = Modifier) {
                drawImage(
                    uiViewModel.uiState.value.cropResult.value!!.asImageBitmap(),
                    //dstOffset = IntOffset(offsetXImage.toInt(), offsetYImage.toInt()),
                    dstSize = IntSize((cropSquareX*zoom).toInt(), (cropSquareY*zoom).toInt()),
                )
            }
        }
    }
}

@Composable
fun errorDialog(
    uiViewModel: UiViewModel,
    message:String
) {
    Dialog(onDismissRequest = {uiViewModel.uiState.value.errorDialog.value = false}) {
        Card(modifier = Modifier
            .background(color = Color.White,RoundedCornerShape(24.dp))
            //.height(100.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .width(180.dp)
                        .padding(10.dp, 0.dp)
                ) {
                    Text("Error",style = MaterialTheme.typography.titleLarge)
                    Image(
                        painter = painterResource(R.drawable.close_24px),
                        contentDescription = null,
                        modifier = Modifier
                            //.padding(0.dp,5.dp,0.dp,0.dp)
                            .size(32.dp)
                            .clickable {
                                uiViewModel.uiState.value.errorDialog.value = false
                            }
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier
                        .width(180.dp)
                        .padding(10.dp, 0.dp)
                ) {
                    Text(message,style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}


