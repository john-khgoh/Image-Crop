package com.example.imagecrop

import android.annotation.SuppressLint
import android.content.res.Configuration

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun crop(imageScale: Float, bitmap: Bitmap, uiViewModel: UiViewModel) {

    uiViewModel.uiState.value.bitmap.value = bitmap
    uiViewModel.uiState.value.currentOrientation.value = LocalConfiguration.current.orientation

    if(!uiViewModel.uiState.value.orientationSavedFlag.value) {
        uiViewModel.uiState.value.previousOrientation.value = uiViewModel.uiState.value.currentOrientation.value
        uiViewModel.uiState.value.orientationSavedFlag.value = true
    }

    //If screen orientation event occured
    if(uiViewModel.uiState.value.currentOrientation.value!=uiViewModel.uiState.value.previousOrientation.value) {
        uiViewModel.uiState.value.orientationSavedFlag.value = false
    }

    var TAG = "Error"

    var finalWidth by remember {mutableStateOf(0)}
    var finalHeight by remember {mutableStateOf(0)}
    var offsetXImage by remember {mutableStateOf(0)}
    var offsetYImage by remember {mutableStateOf(0)}
    var cropSquareColor = Color.DarkGray
    var widthRatio by remember { mutableStateOf(0f)}
    var heightRatio by remember { mutableStateOf(0f)}

    val width: Int = bitmap.width
    val height: Int = bitmap.height
    val ratioBitmap = width.toFloat() / height.toFloat() //Original bitmap aspect ratio

    var offset by remember { mutableStateOf(Offset.Zero)}
    var panOffset by remember {mutableStateOf(Offset.Zero)}
    var absoluteZoom by remember {mutableStateOf(1.0f)}
    var zoom by remember {mutableStateOf(1f)}
    var rect by remember {mutableStateOf(Unit)}
    var flag by remember {mutableStateOf(false)}

    var dragPos by remember {mutableStateOf(Offset(0f,0f))}
    var previousDragPos by remember {mutableStateOf(Offset(0f,0f))}
    var dragMagnitude by remember {mutableStateOf(Offset(0f,0f))}
    var polygonSize by remember {mutableStateOf(30f)}
    var landscapeOffset by remember {mutableStateOf(0)}

    var toastShown by remember {mutableStateOf(false)}

    val screenHeight by remember {mutableStateOf(uiViewModel.uiState.value.context.resources.displayMetrics.heightPixels)}
    val reference by remember {mutableStateOf(100) }
    val scalingFactor by remember { mutableStateOf((reference/100.0).toFloat())}

    if(uiViewModel.uiState.value.currentOrientation.value== Configuration.ORIENTATION_PORTRAIT) {
        landscapeOffset = 0
    } else {
        landscapeOffset = ((scalingFactor * screenHeight)/2 - (2 * finalWidth/3)).toInt()
    }

    if(!uiViewModel.uiState.value.cropRotationHorizontalFlag.value) {
        widthRatio = (width.toFloat() / finalWidth.toFloat())
        heightRatio = (height.toFloat() / finalHeight.toFloat())
    } else {
        widthRatio = (width.toFloat() / finalHeight.toFloat())
        heightRatio = (height.toFloat() / finalWidth.toFloat())
    }

    //Create cropped image
    if (uiViewModel.uiState.value.allowCropBool.value) {
        val panOffsetValue = panOffset * -1F
        try {
            uiViewModel.uiState.value.cropResult.value = Bitmap.createBitmap(
                uiViewModel.uiState.value.bitmap.value!!,
                (widthRatio * (panOffsetValue.x + uiViewModel.uiState.value.offsetX.value)).toInt(), //x = width
                (heightRatio * (panOffsetValue.y + uiViewModel.uiState.value.offsetY.value)).toInt(), //y = height
                (widthRatio * uiViewModel.uiState.value.cropSquareX.value).toInt(),
                (heightRatio * uiViewModel.uiState.value.cropSquareY.value).toInt()
            )
            uiViewModel.uiState.value.cropResultReady.value = true
        } catch(e: Throwable) {
            Log.e(TAG,"Out of bounds error: " + e.message.toString())
            uiViewModel.uiState.value.errorDialog.value = true
            uiViewModel.uiState.value.allowCropBool.value = false
            //Resets the crop box to the center
            uiViewModel.uiState.value.cropSquareX.value = 400
            uiViewModel.uiState.value.cropSquareY.value = 400
            uiViewModel.uiState.value.cropSquareResetFlag.value = false
            uiViewModel.uiState.value.cropSquareOffFlag.value = false
            offset = Offset(
                (finalWidth.toFloat() - uiViewModel.uiState.value.cropSquareX.value) / 2,
                (finalHeight.toFloat() - uiViewModel.uiState.value.cropSquareY.value) / 2
            )
        }
    }

    Scaffold(topBar = {cropTopBar(uiViewModel)}) {innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.size(10.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    //.scale(imageScale)
                    .pointerInput(Unit) {
                        detectTransformGestures { centroid, pan, gestureZoom, gestureRotate ->
                            if(!toastShown) {
                                Toast.makeText(uiViewModel.uiState.value.context,"Tip: Long-press the cropbox corners to resize", Toast.LENGTH_LONG).show()
                                toastShown = true
                            }

                            if(uiViewModel.uiState.value.cropSquareOffFlag.value) {
                                absoluteZoom *= gestureZoom
                                finalWidth = (finalWidth * gestureZoom).toInt()
                                finalHeight = (finalHeight * gestureZoom).toInt()
                                panOffset += pan
                            } else {
                                offset += pan
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress { change, dragAmount ->
                            dragPos = change.position //Location of movement
                            dragMagnitude = dragAmount // Direction of movement
                        }
                    }

            ) {
                if(!uiViewModel.uiState.value.cropInitializationFlag.value) {
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

                    panOffset = Offset.Zero
                    absoluteZoom = 1.0f

                    uiViewModel.uiState.value.cropRotationHorizontalFlag.value = false
                    uiViewModel.uiState.value.cropSquareResetFlag.value = false
                    uiViewModel.uiState.value.cropSquareOffFlag.value = false
                    uiViewModel.uiState.value.cropInitializationFlag.value = true
                }

                /*
                //Centering the image
                offsetXImage = (size.width - finalWidth) / 2
                offsetYImage = (size.height - finalHeight) / 2 */

                //Set the position to the center of image at the beginning
                if (!uiViewModel.uiState.value.cropSquareResetFlag.value) {
                    offset = Offset(
                        (finalWidth.toFloat() - uiViewModel.uiState.value.cropSquareX.value) / 2,
                        (finalHeight.toFloat() - uiViewModel.uiState.value.cropSquareY.value) / 2
                    )
                    uiViewModel.uiState.value.cropSquareResetFlag.value = true
                }

                //Set the threshold according to be 1/6th of cropSquare size (double of 1/3rd, but convenient for multiplication)
                uiViewModel.uiState.value.thresholdX.value = uiViewModel.uiState.value.cropSquareX.value/6
                uiViewModel.uiState.value.thresholdY.value = uiViewModel.uiState.value.cropSquareY.value/6

                //If there's a change to dragPos
                if(previousDragPos != dragPos) {
                    //If none of the flags are currently raised, run raiseDirectionalFlag
                    // to check if the offset is within bounds of the crop square
                    if(!uiViewModel.uiState.value.westFlag.value &&
                        !uiViewModel.uiState.value.northFlag.value &&
                        !uiViewModel.uiState.value.eastFlag.value &&
                        !uiViewModel.uiState.value.southFlag.value
                    ) {
                        raiseDirectionalFlag(dragPos, landscapeOffset, uiViewModel)
                    }

                    //If westFlag is raised
                    if(uiViewModel.uiState.value.westFlag.value) {
                        //Outside changes
                        if(dragMagnitude.x < 0) {
                            uiViewModel.uiState.value.offsetX.value += dragMagnitude.x
                            offset += Offset(dragMagnitude.x,0f)
                            uiViewModel.uiState.value.cropSquareX.value -= dragMagnitude.x.toInt()
                        }
                        //Inside changes
                        else if(dragMagnitude.x > 0) {
                            uiViewModel.uiState.value.offsetX.value += dragMagnitude.x
                            offset += Offset(dragMagnitude.x,0f)
                            uiViewModel.uiState.value.cropSquareX.value -= dragMagnitude.x.toInt()
                        }
                    }
                    //If northFlag is raised
                    if(uiViewModel.uiState.value.northFlag.value) {
                        //Outside changes
                        if(dragMagnitude.y < 0) {
                            uiViewModel.uiState.value.offsetY.value += dragMagnitude.y
                            offset += Offset(0f,dragMagnitude.y)
                            uiViewModel.uiState.value.cropSquareY.value -= dragMagnitude.y.toInt()
                        }
                        //Inside changes
                        else if(dragMagnitude.y > 0) {
                            uiViewModel.uiState.value.offsetY.value += dragMagnitude.y
                            offset += Offset(0f,dragMagnitude.y)
                            uiViewModel.uiState.value.cropSquareY.value -= dragMagnitude.y.toInt()
                        }
                    }
                    //If eastFlag is raised
                    if(uiViewModel.uiState.value.eastFlag.value) {
                        //Outside changes
                        if(dragMagnitude.x > 0) {
                            uiViewModel.uiState.value.cropSquareX.value += dragMagnitude.x.toInt()
                        }
                        //Inside changes
                        else if(dragMagnitude.x < 0) {
                            uiViewModel.uiState.value.cropSquareX.value += dragMagnitude.x.toInt()
                        }
                    }
                    //If southFlag is raised
                    if(uiViewModel.uiState.value.southFlag.value) {
                        //Outside changes
                        if(dragMagnitude.y > 0) {
                            uiViewModel.uiState.value.cropSquareY.value += dragMagnitude.y.toInt()
                        }
                        //Inside changes
                        else if(dragMagnitude.y < 0) {
                            uiViewModel.uiState.value.cropSquareY.value += dragMagnitude.y.toInt()
                        }
                    }
                }
                //If there's no change to dragPos, set all directional flags to false
                else {
                    uiViewModel.uiState.value.westFlag.value = false
                    uiViewModel.uiState.value.northFlag.value = false
                    uiViewModel.uiState.value.eastFlag.value = false
                    uiViewModel.uiState.value.southFlag.value = false

                    uiViewModel.uiState.value.offsetX.value = offset.x
                    uiViewModel.uiState.value.offsetY.value = offset.y
                }

                if(!uiViewModel.uiState.value.cropRotationHorizontalFlag.value) {
                    drawImage(
                        image = uiViewModel.uiState.value.bitmap.value!!.asImageBitmap(),
                        dstSize = IntSize(finalWidth, finalHeight),
                        dstOffset = IntOffset(landscapeOffset + panOffset.x.toInt(),panOffset.y.toInt())
                    )
                } else {
                    drawImage(
                        image = uiViewModel.uiState.value.bitmap.value!!.asImageBitmap(),
                        dstSize = IntSize(finalHeight, finalWidth),
                        dstOffset = IntOffset(landscapeOffset + panOffset.x.toInt(),panOffset.y.toInt())
                    )
                }

                /*
                //Bounding the crop box to the dimensions of the image
                if (uiViewModel.uiState.value.offsetX.value < 0f) {
                    uiViewModel.uiState.value.offsetX.value = 0f
                } else if ((uiViewModel.uiState.value.offsetX.value + (uiViewModel.uiState.value.cropSquareX.value * zoomX)) > finalWidth) {
                    uiViewModel.uiState.value.offsetX.value = finalWidth - (uiViewModel.uiState.value.cropSquareX.value * zoom)
                }
                if (uiViewModel.uiState.value.offsetY.value < 0f) {
                    uiViewModel.uiState.value.offsetY.value = 0f
                } else if ((uiViewModel.uiState.value.offsetY.value + (uiViewModel.uiState.value.cropSquareY.value * zoomY)) > finalHeight) {
                    uiViewModel.uiState.value.offsetY.value = finalHeight - (uiViewModel.uiState.value.cropSquareY.value * zoom)
                } */

                if(!uiViewModel.uiState.value.allowCropBool.value && !uiViewModel.uiState.value.cropSquareOffFlag.value) {
                    //Draw the cropbox when allowCropBool is true (not previewing the crop image)
                    rect = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value + landscapeOffset, uiViewModel.uiState.value.offsetY.value),
                        size = Size(uiViewModel.uiState.value.cropSquareX.value.toFloat(), uiViewModel.uiState.value.cropSquareY.value.toFloat()),
                        style = Stroke(3.dp.toPx())
                    )
                    rect

                    //Dynamic polygonSize
                    if(uiViewModel.uiState.value.cropSquareX.value<=50 || uiViewModel.uiState.value.cropSquareY.value<=50) {
                        polygonSize = 15f
                    }
                    else if((uiViewModel.uiState.value.cropSquareX.value in 51..99) ||
                        (uiViewModel.uiState.value.cropSquareY.value in 51..99)) {
                        polygonSize = 20f
                    }
                    else {
                        polygonSize = 30f
                    }

                    //Draw the resize polygons
                    var westPolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value-(polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value + (0.5 * uiViewModel.uiState.value.cropSquareY.value).toFloat()-(polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    westPolygon

                    var northPolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value + (0.5*uiViewModel.uiState.value.cropSquareX.value).toFloat()-(polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value-(polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    northPolygon

                    var eastPolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value + uiViewModel.uiState.value.cropSquareX.value - (polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value + (0.5*uiViewModel.uiState.value.cropSquareY.value).toFloat()-(polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    eastPolygon

                    var southPolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value + (0.5*uiViewModel.uiState.value.cropSquareX.value).toFloat()-(polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value + uiViewModel.uiState.value.cropSquareY.value - (polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    southPolygon

                    var nwPolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value-(polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value-(polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    nwPolygon

                    var nePolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value+uiViewModel.uiState.value.cropSquareX.value-(polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value-(polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    nePolygon

                    var sePolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value + uiViewModel.uiState.value.cropSquareX.value - (polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value + uiViewModel.uiState.value.cropSquareY.value - (polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    sePolygon

                    var swPolygon = drawRect(
                        color = cropSquareColor,
                        topLeft = Offset(uiViewModel.uiState.value.offsetX.value - (polygonSize/2) + landscapeOffset,
                            uiViewModel.uiState.value.offsetY.value + uiViewModel.uiState.value.cropSquareY.value - (polygonSize/2)),
                        size = Size(polygonSize,polygonSize)
                    )
                    swPolygon
                }

                //widthRatio = (width.toFloat() / finalWidth.toFloat())
                //heightRatio = (height.toFloat() / finalHeight.toFloat())

                //At the end, set previousDragPos to dragPos
                previousDragPos = dragPos
            }
            when {
                uiViewModel.uiState.value.cropResultReady.value -> {
                    resultDialog(uiViewModel = uiViewModel)
                }
            }
            when {
                uiViewModel.uiState.value.errorDialog.value -> {
                    errorDialog(uiViewModel = uiViewModel, message = "The crop box is out of bounds.")
                }
            }
        }
    }

}

@SuppressLint("StateFlowValueCalledInComposition")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun cropTopBar(uiViewModel: UiViewModel) {

    var rotateDropMenuBool by remember {mutableStateOf(false)}

    var zoomCropLabel = when(uiViewModel.uiState.value.cropSquareOffFlag.value) {
        false -> "Zoom"
        true -> "Crop box"
    }

    var zoomCropIcon = when(uiViewModel.uiState.value.cropSquareOffFlag.value) {
        false -> R.drawable.zoom_in_24px
        true -> R.drawable.crop_free_24px
    }

    TopAppBar(
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(),
        navigationIcon = {
            IconButton(
                onClick = {
                    //navigate up
                }
            ) {
                Icon(painter = painterResource(R.drawable.arrow_back_24px), contentDescription = null)
            }
        },
        actions = {
            if(!uiViewModel.uiState.value.allowCropBool.value) {
                //Zoom/Crop mode
                IconButton(
                    onClick = {
                        uiViewModel.uiState.value.cropSquareOffFlag.value =
                            !uiViewModel.uiState.value.cropSquareOffFlag.value
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .border(width = 0.dp, color = Color.Unspecified, shape = RectangleShape),
                    //enabled = buttonsEnabled
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(zoomCropIcon),
                            contentDescription = null,
                        )
                        Text(
                            zoomCropLabel,
                            style = MaterialTheme.typography.labelSmall,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                //Reset (image)
                IconButton(
                    onClick = {
                        uiViewModel.uiState.value.cropInitializationFlag.value = false
                    },
                    modifier = Modifier
                        .size(60.dp)
                        .border(width = 0.dp, color = Color.Unspecified, shape = RectangleShape),
                    //enabled = buttonsEnabled
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.reset_image_24px),
                            contentDescription = null,
                        )
                        Text("Reset", style = MaterialTheme.typography.labelSmall, overflow = TextOverflow.Ellipsis)
                    }
                }

                //Rotate
                Box(
                    modifier = Modifier
                        //.fillMaxSize()
                        .wrapContentSize(Alignment.TopEnd)
                ) {
                    IconButton(
                        onClick = { rotateDropMenuBool = !rotateDropMenuBool },
                        modifier = Modifier
                            .size(60.dp)
                            .border(width = 0.dp, color = Color.Unspecified, shape = RectangleShape)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.rotate_right_24px),
                                contentDescription = null,
                            )
                            Text(
                                "Rotate",
                                style = MaterialTheme.typography.labelSmall,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    DropdownMenu(expanded = rotateDropMenuBool, onDismissRequest = { rotateDropMenuBool = false }) {
                        //Rotate 90° Right
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rotate_right_24px),
                                    contentDescription = null,
                                )
                            },
                            text = {
                                Text(
                                    "90° Right",
                                    style = MaterialTheme.typography.labelSmall,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                uiViewModel.uiState.value.bitmap.value = rotateBitmap(uiViewModel, uiViewModel.uiState.value.bitmap.value!!, 90F)
                                rotateDropMenuBool = false
                            })

                        //Rotate 90° Left
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.rotate_left_24px),
                                    contentDescription = null,
                                )
                            },
                            text = {
                                Text(
                                    "90° Left",
                                    style = MaterialTheme.typography.labelSmall,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                uiViewModel.uiState.value.bitmap.value = rotateBitmap(uiViewModel, uiViewModel.uiState.value.bitmap.value!!, -90F)
                                rotateDropMenuBool = false
                            })
                    }
                }

                Spacer(modifier = Modifier.width(30.dp))

                    IconButton(
                        onClick = {uiViewModel.uiState.value.allowCropBool.value = !uiViewModel.uiState.value.allowCropBool.value},
                        modifier = Modifier
                            .size(60.dp)
                            .border(width = 0.dp, color = Color.Unspecified, shape = RectangleShape),
                    ) {
                        Column() {
                            Icon(painter = painterResource(R.drawable.crop_24px), contentDescription = null)
                            Text("Crop",style= MaterialTheme.typography.labelSmall)
                        }
                    }
            }
            else {
                //Cancel
                IconButton(
                    onClick = {uiViewModel.uiState.value.allowCropBool.value = !uiViewModel.uiState.value.allowCropBool.value},
                    modifier = Modifier
                        .size(60.dp)
                        .border(width = 0.dp, color = Color.Unspecified, shape = RectangleShape),
                ) {
                    Column() {
                        Icon(painter = painterResource(R.drawable.close_24px), contentDescription = null)
                        Text("Cancel",style= MaterialTheme.typography.labelSmall)
                    }
                }

                //Accept
                IconButton(
                    onClick = {},
                    modifier = Modifier
                        .size(60.dp)
                        .border(width = 0.dp, color = Color.Unspecified, shape = RectangleShape),
                ) {
                    Column() {
                        Icon(painter = painterResource(R.drawable.done_24px), contentDescription = null)
                        Text("Accept",style= MaterialTheme.typography.labelSmall)
                    }
                }
            }



        }
    )
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun resultDialog(
    uiViewModel: UiViewModel
) {
    Dialog(onDismissRequest = {
        uiViewModel.uiState.value.cropResultReady.value = false
        uiViewModel.uiState.value.allowCropBool.value = false},
    ) {
        Card(
            modifier = Modifier
                .background(color = Color.Unspecified)
                .width((0.4*uiViewModel.uiState.value.cropSquareX.value-12).dp)
                .height((0.4*uiViewModel.uiState.value.cropSquareY.value-10).dp)
        ) {

            Canvas(modifier = Modifier) {
                try {
                    drawImage(
                        uiViewModel.uiState.value.cropResult.value!!.asImageBitmap(),
                        dstSize = IntSize((uiViewModel.uiState.value.cropSquareX.value), (uiViewModel.uiState.value.cropSquareY.value)),
                    )
                } catch(e: Throwable) {
                    Log.d("Error",e.stackTraceToString())
                }

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

//Function to raise a directional flag for the size change of cropping square
fun raiseDirectionalFlag(dragPos: Offset, landscapeOffset: Int, uiViewModel: UiViewModel) {
    var offsetX = uiViewModel.uiState.value.offsetX.value
    var offsetY = uiViewModel.uiState.value.offsetY.value
    var cropSquareX = uiViewModel.uiState.value.cropSquareX.value
    var cropSquareY = uiViewModel.uiState.value.cropSquareY.value
    var thresholdX = uiViewModel.uiState.value.thresholdX.value
    var thresholdY = uiViewModel.uiState.value.thresholdY.value

    //The 8 directional points of the crop square
    var westPoint = Offset(offsetX + landscapeOffset,offsetY + (0.5*cropSquareY).toFloat())
    var northPoint = Offset(offsetX + (0.5*cropSquareX).toFloat() + landscapeOffset,offsetY)
    var eastPoint = Offset(offsetX + cropSquareX + landscapeOffset,offsetY + (0.5 * cropSquareY).toFloat())
    var southPoint = Offset(offsetX + (0.5*cropSquareX).toFloat() + landscapeOffset,offsetY+cropSquareY)
    var nwPoint = Offset(offsetX + landscapeOffset,offsetY)
    var nePoint = Offset(offsetX+cropSquareX + landscapeOffset,offsetY)
    var sePoint = Offset(offsetX + cropSquareX + landscapeOffset,offsetY +cropSquareY)
    var swPoint = Offset(offsetX + landscapeOffset,offsetY+cropSquareY)

    //Defining westPoint boundaries
    var westPoint_west = westPoint.x - thresholdX
    var westPoint_north = westPoint.y - thresholdY
    var westPoint_east = westPoint.x + thresholdX
    var westPoint_south = westPoint.y + thresholdY

    //Defining northPoint boundaries
    var northPoint_west = northPoint.x - thresholdX
    var northPoint_north = northPoint.y - thresholdY
    var northPoint_east = northPoint.x + thresholdX
    var northPoint_south = northPoint.y + thresholdY

    //Defining eastPoint boundaries
    var eastPoint_west = eastPoint.x - thresholdX
    var eastPoint_north = eastPoint.y - thresholdY
    var eastPoint_east = eastPoint.x + thresholdX
    var eastPoint_south = eastPoint.y + thresholdY

    //Defining southPoint boundaries
    var southPoint_west = southPoint.x - thresholdX
    var southPoint_north = southPoint.y - thresholdY
    var southPoint_east = southPoint.x + thresholdX
    var southPoint_south = southPoint.y + thresholdY

    //Defining the nwPoint boundaries
    var nwPoint_west = nwPoint.x - thresholdX
    var nwPoint_north = nwPoint.y - thresholdY
    var nwPoint_east = nwPoint.x + thresholdX
    var nwPoint_south = nwPoint.y + thresholdY

    //Defining the nePoint boundaries
    var nePoint_west = nePoint.x - thresholdX
    var nePoint_north = nePoint.y - thresholdY
    var nePoint_east = nePoint.x + thresholdX
    var nePoint_south = nePoint.y + thresholdY

    //Defining the sePoint boundaries
    var sePoint_west = sePoint.x - thresholdX
    var sePoint_north = sePoint.y - thresholdY
    var sePoint_east = sePoint.x + thresholdX
    var sePoint_south = sePoint.y + thresholdY

    //Defining the swPoint boundaries
    var swPoint_west = swPoint.x - thresholdX
    var swPoint_north = swPoint.y - thresholdY
    var swPoint_east = swPoint.x + thresholdX
    var swPoint_south = swPoint.y + thresholdY

    if(dragPos.x >= westPoint_west && dragPos.x <= westPoint_east && dragPos.y >= westPoint_north && dragPos.y <= westPoint_south) {
        uiViewModel.uiState.value.westFlag.value = true
    }
    else if(dragPos.x >= northPoint_west && dragPos.x <= northPoint_east && dragPos.y >= northPoint_north && dragPos.y <= northPoint_south) {
        uiViewModel.uiState.value.northFlag.value = true
    }
    else if(dragPos.x >= eastPoint_west && dragPos.x <= eastPoint_east && dragPos.y >= eastPoint_north && dragPos.y <= eastPoint_south) {
        uiViewModel.uiState.value.eastFlag.value = true
    }
    else if(dragPos.x >= southPoint_west && dragPos.x <= southPoint_east && dragPos.y >= southPoint_north && dragPos.y <= southPoint_south) {
        uiViewModel.uiState.value.southFlag.value = true
    }
    else if(dragPos.x >= nwPoint_west && dragPos.x <= nwPoint_east && dragPos.y >= nwPoint_north && dragPos.y <= nwPoint_south) {
        uiViewModel.uiState.value.northFlag.value = true
        uiViewModel.uiState.value.westFlag.value = true
    }
    else if(dragPos.x >= nePoint_west && dragPos.x <= nePoint_east && dragPos.y >= nePoint_north && dragPos.y <= nePoint_south) {
        uiViewModel.uiState.value.northFlag.value = true
        uiViewModel.uiState.value.eastFlag.value = true
    }
    else if(dragPos.x >= sePoint_west && dragPos.x <= sePoint_east && dragPos.y >= sePoint_north && dragPos.y <= sePoint_south) {
        uiViewModel.uiState.value.southFlag.value = true
        uiViewModel.uiState.value.eastFlag.value = true
    }
    else if(dragPos.x >= swPoint_west && dragPos.x <= swPoint_east && dragPos.y >= swPoint_north && dragPos.y <= swPoint_south) {
        uiViewModel.uiState.value.southFlag.value = true
        uiViewModel.uiState.value.westFlag.value = true
    }
}

fun rotateBitmap(uiViewModel: UiViewModel, bitmap: Bitmap, rotationDegrees: Float): Bitmap {
    uiViewModel.uiState.value.cropRotationHorizontalFlag.value = !uiViewModel.uiState.value.cropRotationHorizontalFlag.value
    val matrix = Matrix()
    matrix.postRotate(rotationDegrees)
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap,bitmap.width,bitmap.height,true)
    return Bitmap.createBitmap(scaledBitmap,0,0,scaledBitmap.width,scaledBitmap.height,matrix,true)
}


