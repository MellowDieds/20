package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CalmMintGlow
import com.example.ui.theme.SereneSky
import com.example.ui.theme.SoftAmber
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class ExerciseStep(val title: String, val icon: @Composable () -> Unit) {
    LOOK_AWAY("Uzağa Bak", { Icon(Icons.Default.RemoveRedEye, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    BLINKING("Göz Kırpma", { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    EYE_ROLL("Kas Esnetme", { Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(18.dp)) }),
    PALMING("Avuç Isıtma", { Icon(Icons.Default.SelfImprovement, contentDescription = null, modifier = Modifier.size(18.dp)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedExerciseScreen(
    onComplete: (durationSeconds: Int, routineName: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableStateOf(ExerciseStep.LOOK_AWAY) }
    var isFinished by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.testTag("animated_exercise_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "20-20-20 Mola Rehberi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Eğitici Göz Dinlendirme Animasyonu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("exercise_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri / Kapat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step Tabs
            TabRow(
                selectedTabIndex = currentStep.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                ExerciseStep.values().forEach { step ->
                    Tab(
                        selected = currentStep == step,
                        onClick = { currentStep = step },
                        text = {
                            Text(
                                text = step.title,
                                fontSize = 12.sp,
                                fontWeight = if (currentStep == step) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        icon = step.icon
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isFinished) {
                CompletionCard(
                    onFinish = {
                        onComplete(20, "20-20-20 Tam Döngü")
                        onClose()
                    }
                )
            } else {
                AnimatedContent(
                    targetState = currentStep,
                    label = "exercise_step_content"
                ) { step ->
                    when (step) {
                        ExerciseStep.LOOK_AWAY -> LookAwayExercise(
                            onNextStep = { currentStep = ExerciseStep.BLINKING },
                            onFinishRoutine = { isFinished = true }
                        )
                        ExerciseStep.BLINKING -> BlinkingExercise(
                            onNextStep = { currentStep = ExerciseStep.EYE_ROLL },
                            onPrevStep = { currentStep = ExerciseStep.LOOK_AWAY }
                        )
                        ExerciseStep.EYE_ROLL -> EyeRollExercise(
                            onNextStep = { currentStep = ExerciseStep.PALMING },
                            onPrevStep = { currentStep = ExerciseStep.BLINKING }
                        )
                        ExerciseStep.PALMING -> PalmingExercise(
                            onFinish = { isFinished = true },
                            onPrevStep = { currentStep = ExerciseStep.EYE_ROLL }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 1. Step: Look 20 feet away for 20 seconds.
 */
@Composable
fun LookAwayExercise(
    onNextStep: () -> Unit,
    onFinishRoutine: () -> Unit
) {
    var secondsLeft by remember { mutableIntStateOf(20) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_rings")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val tealGlow = CalmMintGlow
    val skyColor = SereneSky

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "1. Adım: 20 Feet (6 Metre) Uzağa Odaklan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Telefon ekranından gözlerinizi ayırın. Pencereden dışarı veya odanın en uzak köşesine bakın.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Visual Circle with Horizon/Focus
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Canvas with expanding relaxation ripples and distant target
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val baseRadius = size.width / 3f

                    // Outer pulse ring
                    drawCircle(
                        color = tealGlow.copy(alpha = 0.25f),
                        radius = baseRadius * pulseScale,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Middle calm ring
                    drawCircle(
                        color = skyColor.copy(alpha = 0.35f),
                        radius = baseRadius * (pulseScale * 0.85f),
                        center = center,
                        style = Stroke(width = 4.dp.toPx())
                    )

                    // Horizon Line & Distant Focus Dot
                    drawLine(
                        color = primaryColor.copy(alpha = 0.3f),
                        start = Offset(20f, center.y),
                        end = Offset(size.width - 20f, center.y),
                        strokeWidth = 2.dp.toPx()
                    )

                    // Distant target dot
                    drawCircle(
                        color = primaryColor,
                        radius = 12.dp.toPx(),
                        center = center
                    )

                    // Progress ring around circle
                    val sweepAngle = ((20 - secondsLeft) / 20f) * 360f
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(12.dp.toPx(), 12.dp.toPx()),
                        size = Size(size.width - 24.dp.toPx(), size.height - 24.dp.toPx()),
                        style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Center countdown text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$secondsLeft",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (secondsLeft > 0) "saniye" else "Tamamlandı! ✨",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timer controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { isRunning = !isRunning },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isRunning) "Duraklat" else "Başlat")
                }

                FilledTonalButton(
                    onClick = {
                        secondsLeft = 20
                        isRunning = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sıfırla")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Bilimsel İpucu: Ekrana yakından bakarken siliyer göz kasları kasılı kalır. 6 metre uzağa bakmak bu kasları sıfırlar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onFinishRoutine,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Molayı Bitir")
                }

                Button(
                    onClick = onNextStep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sonraki: Kırpma 👉")
                }
            }
        }
    }
}

/**
 * 2. Step: Mindful Blinking animation (rehydrates cornea).
 */
@Composable
fun BlinkingExercise(
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit
) {
    var blinkCount by remember { mutableIntStateOf(0) }
    val maxBlinks = 10

    // Eye eyelid opening animation
    val eyelidOpen = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        while (blinkCount < maxBlinks) {
            delay(1200L)
            // Close eyelid
            eyelidOpen.animateTo(0.1f, animationSpec = tween(150, easing = LinearEasing))
            delay(100L)
            // Reopen eyelid
            eyelidOpen.animateTo(1f, animationSpec = tween(200, easing = FastOutSlowInEasing))
            blinkCount++
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "2. Adım: Bilinçli Göz Kırpma",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Ekrana odaklanırken göz kırpma sayısı %66 azalır. Gözyaşı tabakasını yenilemek için nazikçe kırpın.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Eye Canvas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(containerColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(160.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val eyeWidth = size.width * 0.85f
                    val eyeHeight = size.height * 0.45f * eyelidOpen.value

                    // Eye white background
                    drawOval(
                        color = Color.White,
                        topLeft = Offset(center.x - eyeWidth / 2f, center.y - eyeHeight / 2f),
                        size = Size(eyeWidth, eyeHeight)
                    )

                    // Iris
                    val irisRadius = (size.height * 0.22f).coerceAtMost(eyeHeight * 0.95f)
                    drawCircle(
                        color = primaryColor,
                        radius = irisRadius,
                        center = center
                    )

                    // Pupil
                    drawCircle(
                        color = Color.Black,
                        radius = irisRadius * 0.5f,
                        center = center
                    )

                    // Eye shine
                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = irisRadius * 0.2f,
                        center = Offset(center.x - irisRadius * 0.25f, center.y - irisRadius * 0.25f)
                    )

                    // Eye outline
                    drawOval(
                        color = primaryColor,
                        topLeft = Offset(center.x - eyeWidth / 2f, center.y - eyeHeight / 2f),
                        size = Size(eyeWidth, eyeHeight),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Kırpma: $blinkCount / $maxBlinks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            FilledTonalButton(
                onClick = { blinkCount = 0 },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tekrar Başlat")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onPrevStep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("👈 Geri")
                }

                Button(
                    onClick = onNextStep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sonraki: Kas Esnetme 👉")
                }
            }
        }
    }
}

/**
 * 3. Step: Eye Roll / Infinity follow animation.
 */
@Composable
fun EyeRollExercise(
    onNextStep: () -> Unit,
    onPrevStep: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "infinity_track")
    val angleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val orbColor = CalmMintGlow

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "3. Adım: Göz Kaslarını Esnetme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Başınızı sabit tutun. Sadece göz bebeklerinizle hareket eden yeşil noktayı takip edin.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Infinity / Circle Canvas
            Box(
                modifier = Modifier
                    .size(240.dp, 160.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val a = size.width * 0.38f // Lemniscate width
                    val b = size.height * 0.32f

                    // Draw track points
                    for (i in 0..100) {
                        val t = (i / 100f) * (2 * Math.PI).toFloat()
                        val scale = 2 / (3 - cos(2 * t))
                        val x = center.x + a * scale * cos(t)
                        val y = center.y + b * scale * sin(2 * t) / 2
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.15f),
                            radius = 2.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    // Calculate moving orb position on figure-8
                    val t = angleProgress
                    val scale = 2 / (3 - cos(2 * t))
                    val orbX = center.x + a * scale * cos(t)
                    val orbY = center.y + b * scale * sin(2 * t) / 2

                    // Glow aura
                    drawCircle(
                        color = orbColor.copy(alpha = 0.35f),
                        radius = 18.dp.toPx(),
                        center = Offset(orbX, orbY)
                    )

                    // Sharp orb center
                    drawCircle(
                        color = orbColor,
                        radius = 9.dp.toPx(),
                        center = Offset(orbX, orbY)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onPrevStep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("👈 Geri")
                }

                Button(
                    onClick = onNextStep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sonraki: Avuç Isıtma 👉")
                }
            }
        }
    }
}

/**
 * 4. Step: Palming & Warmth relaxation.
 */
@Composable
fun PalmingExercise(
    onFinish: () -> Unit,
    onPrevStep: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breath_pulse")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )

    val isExpanding = breathScale > 1.05f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "4. Adım: Avuç İçiyle Isıtma (Palming)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Avuçlarınızı birbirine sürterek ısıtın. Gözlerinizi kapatın ve sıcak avuçlarınızı baskı uygulamadan gözlerinizin üzerine koyun.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Animated Breathing Aura
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                SoftAmber.copy(alpha = 0.35f * breathScale),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SoftAmber.copy(alpha = 0.4f),
                        radius = (size.width / 3f) * breathScale,
                        style = Stroke(width = 4.dp.toPx())
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SelfImprovement,
                        contentDescription = null,
                        tint = SoftAmber,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isExpanding) "Derin Nefes Al" else "Yavaşça Nefes Ver",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FilledTonalButton(
                    onClick = onPrevStep,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("👈 Geri")
                }

                Button(
                    onClick = onFinish,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Egzersizi Tamamla ✨")
                }
            }
        }
    }
}

/**
 * Completion celebration card.
 */
@Composable
fun CompletionCard(
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tebrikler! Mola Tamamlandı 🌿",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gözleriniz dinlendi ve göz kaslarınız gevşedi. Ekran süresi sayacı sıfırlandı ve sonraki 20 dakikalık döngü başladı.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("finish_routine_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Ana Ekrana Dön",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
