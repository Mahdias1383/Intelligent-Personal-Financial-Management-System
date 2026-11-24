package com.example.ui.components

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors

@Composable
internal fun LoanContractCameraViewport(
    hasCameraPermission: Boolean,
    isProcessingOcr: Boolean,
    onRequestCameraPermission: () -> Unit,
    onBitmapCaptured: (Bitmap, Int) -> Unit,
    onCaptureError: (String) -> Unit,
    onPickFromGallery: () -> Unit,
    onOpenManualTextPaste: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraBindFailed by remember { mutableStateOf(false) }
    var isCapturingFrame by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }

    DisposableEffect(hasCameraPermission, lifecycleOwner) {
        if (!hasCameraPermission) {
            onDispose { }
        } else {
            val providerFuture = ProcessCameraProvider.getInstance(context)
            var activeProvider: ProcessCameraProvider? = null
            providerFuture.addListener(
                {
                    try {
                        val cameraProvider = providerFuture.get()
                        activeProvider = cameraProvider
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()
                        val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        } else {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        }
                        cameraProvider.unbindAll()
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture
                        )
                        boundCamera = camera
                        imageCaptureRef = imageCapture
                        cameraBindFailed = false
                    } catch (e: Exception) {
                        Log.w("TarazinoScanner", "CameraX bind unavailable: ${e.message}")
                        cameraBindFailed = true
                    }
                },
                ContextCompat.getMainExecutor(context)
            )
            onDispose {
                runCatching { activeProvider?.unbindAll() }
                boundCamera = null
                imageCaptureRef = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040A0D))
    ) {
        if (hasCameraPermission && !cameraBindFailed) {
            // Live CameraX Preview
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Animated Document Reticle & Laser Scan Overlay
            ScannerViewfinderOverlay(
                accentColor = Color(0xFF35D4C5),
                goldColor = Color(0xFFF5D28A),
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Friendly Fallback when Camera permission denied or camera hardware unavailable
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = Color(0xFF45E6D5),
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = if (!hasCameraPermission) {
                        "دسترسی دوربین برای اسکن زنده قرارداد وام"
                    } else {
                        "دوربین دستگاه در دسترس نیست"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (!hasCameraPermission) {
                        "برای اسکن مستقیم قراردادهای کاغذی و رسیدهای بانکی مجوز دوربین را فعال کنید، یا بدون نیاز به دسترسی حافظه تصویر را از گالری انتخاب نمایید."
                    } else {
                        "می‌توانید تصویر قرارداد یا رسید را از گالری انتخاب کنید تا متن، مبلغ قسط، تاریخ و نرخ سود آن استخراج شود."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB5CDD3),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                if (!hasCameraPermission) {
                    Button(
                        onClick = onRequestCameraPermission,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF35B8AB),
                            contentColor = Color(0xFF041C19)
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("scanner_request_camera_permission_btn")
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("فعال‌سازی دوربین", fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
                OutlinedButton(
                    onClick = onPickFromGallery,
                    border = BorderStroke(1.dp, Color(0xFF45E6D5).copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scanner_fallback_gallery_btn")
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = Color(0xFF45E6D5),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتخاب تصویر قرارداد / رسید از گالری", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenManualTextPaste,
                    border = BorderStroke(1.dp, Color(0xFFF5D28A).copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("scanner_fallback_manual_text_btn")
                ) {
                    Icon(
                        Icons.Default.EditNote,
                        contentDescription = null,
                        tint = Color(0xFFF5D28A),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("چسباندن یا تایپ متن قرارداد برای تحلیل هوشمند", color = Color(0xFFF5D28A), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Top Floating Camera HUD
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("scanner_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "بستن اسکنر",
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = Color.Black.copy(alpha = 0.58f),
                border = BorderStroke(0.8.dp, Color(0xFF45E6D5).copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = Color(0xFF45E6D5),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "اسکنر هوشمند قرارداد و رسید",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            if (hasCameraPermission && !cameraBindFailed) {
                Surface(
                    shape = CircleShape,
                    color = if (torchEnabled) Color(0xFFF5D28A).copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.55f),
                    border = BorderStroke(
                        0.8.dp,
                        if (torchEnabled) Color(0xFFF5D28A) else Color.White.copy(alpha = 0.22f)
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(
                        onClick = {
                            val next = !torchEnabled
                            torchEnabled = next
                            boundCamera?.cameraControl?.enableTorch(next)
                        },
                        modifier = Modifier.testTag("scanner_torch_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (torchEnabled) "خاموش کردن فلاش" else "روشن کردن فلاش",
                            tint = if (torchEnabled) Color(0xFFF5D28A) else Color.White
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.size(44.dp))
            }
        }

        // Bottom Floating Camera Controls Dock (when camera is active)
        if (hasCameraPermission && !cameraBindFailed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.86f))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "صفحه قرارداد وام یا رسید پرداخت را کامل داخل کادر نگه دارید",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE0F2F1),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Photo Picker (Zero-permission Gallery Import)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isProcessingOcr && !isCapturingFrame) { onPickFromGallery() }
                            .padding(8.dp)
                            .testTag("scanner_pick_gallery_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "انتخاب تصویر از گالری",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "گالری",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    // Primary Capture & OCR Shutter Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .border(3.dp, Color(0xFF45E6D5), CircleShape)
                            .padding(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isProcessingOcr || isCapturingFrame) Color(0xFF2B6F69) else Color.White
                            )
                            .clickable(enabled = !isProcessingOcr && !isCapturingFrame) {
                                val capture = imageCaptureRef
                                if (capture == null) {
                                    onCaptureError("دوربین هنوز آماده نیست؛ دوباره تلاش کنید.")
                                    return@clickable
                                }
                                isCapturingFrame = true
                                capture.takePicture(
                                    cameraExecutor,
                                    object : ImageCapture.OnImageCapturedCallback() {
                                        override fun onCaptureSuccess(image: ImageProxy) {
                                            try {
                                                val rotation = image.imageInfo.rotationDegrees
                                                val bmp = image.toBitmap()
                                                image.close()
                                                ContextCompat.getMainExecutor(context).execute {
                                                    isCapturingFrame = false
                                                    onBitmapCaptured(bmp, rotation)
                                                }
                                            } catch (e: Exception) {
                                                image.close()
                                                ContextCompat.getMainExecutor(context).execute {
                                                    isCapturingFrame = false
                                                    onCaptureError("خطا در پردازش تصویر دوربین: ${e.localizedMessage ?: ""}")
                                                }
                                            }
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            ContextCompat.getMainExecutor(context).execute {
                                                isCapturingFrame = false
                                                onCaptureError("ثبت تصویر ناموفق بود. می‌توانید از گالری استفاده کنید.")
                                            }
                                        }
                                    }
                                )
                            }
                            .testTag("scanner_capture_shutter_btn")
                    ) {
                        if (isProcessingOcr || isCapturingFrame) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "عکس‌برداری و استخراج متن قرارداد",
                                tint = Color(0xFF041C19),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    // Paste / Edit Raw Text Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(enabled = !isProcessingOcr && !isCapturingFrame) { onOpenManualTextPaste() }
                            .padding(8.dp)
                            .testTag("scanner_manual_text_btn")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.14f))
                                .border(1.dp, Color(0xFFF5D28A).copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = "ورود یا ویرایش متن قرارداد",
                                tint = Color(0xFFF5D28A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "متن دستی",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF5D28A)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannerViewfinderOverlay(
    accentColor: Color,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ScannerLaserSweep")
    val sweepProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserLineY"
    )

    Canvas(modifier = modifier) {
        val frameWidth = size.width * 0.84f
        val frameHeight = size.height * 0.54f
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f - size.height * 0.04f
        val right = left + frameWidth
        val bottom = top + frameHeight

        // Dimmed framing masks around the document viewport
        val maskColor = Color.Black.copy(alpha = 0.48f)
        drawRect(color = maskColor, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(color = maskColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(color = maskColor, topLeft = Offset(0f, top), size = Size(left, frameHeight))
        drawRect(color = maskColor, topLeft = Offset(right, top), size = Size(size.width - right, frameHeight))

        // Subtle document frame border
        drawRoundRect(
            color = Color.White.copy(alpha = 0.25f),
            topLeft = Offset(left, top),
            size = Size(frameWidth, frameHeight),
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 2f)
        )

        // Glowing corner brackets
        val cornerLen = 44f
        val strokeWidth = 7f
        val corners = listOf(
            // Top-left
            Offset(left, top + cornerLen) to Offset(left, top),
            Offset(left, top) to Offset(left + cornerLen, top),
            // Top-right
            Offset(right - cornerLen, top) to Offset(right, top),
            Offset(right, top) to Offset(right, top + cornerLen),
            // Bottom-left
            Offset(left, bottom - cornerLen) to Offset(left, bottom),
            Offset(left, bottom) to Offset(left + cornerLen, bottom),
            // Bottom-right
            Offset(right - cornerLen, bottom) to Offset(right, bottom),
            Offset(right, bottom) to Offset(right, bottom - cornerLen)
        )
        corners.forEach { (start, end) ->
            drawLine(
                color = accentColor,
                start = start,
                end = end,
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        // Animated laser scan line
        val scanY = top + 20f + (frameHeight - 40f) * sweepProgress
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, accentColor, goldColor, accentColor, Color.Transparent),
                startX = left + 16f,
                endX = right - 16f
            ),
            start = Offset(left + 16f, scanY),
            end = Offset(right - 16f, scanY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
    }
}
