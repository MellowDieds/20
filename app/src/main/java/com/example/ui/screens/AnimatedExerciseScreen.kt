package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.VibrationHelper
import kotlinx.coroutines.delay

enum class ExerciseType(
    val title: String,
    val durationLabel: String,
    val icon: ImageVector,
    val medicalBenefit: String
) {
    LOOK_AWAY(
        title = "Uzağa Bakış",
        durationLabel = "20 Sn",
        icon = Icons.Default.RemoveRedEye,
        medicalBenefit = "Sürekli ekrana bakmaktan kramp giren siliyer odak kaslarını gevşetir, göz yorgunluğu baş ağrısını anında engeller."
    ),
    BLINKING(
        title = "Kırpma Masajı",
        durationLabel = "30 Sn",
        icon = Icons.Default.WaterDrop,
        medicalBenefit = "Meibomian yağ bezlerini uyararak gözün kornea yüzeyini nemlendirir, yanma, batma ve kuruluk hissini yok eder."
    ),
    STRETCH(
        title = "4 Yönlü Esnetme",
        durationLabel = "30 Sn",
        icon = Icons.Default.ZoomOutMap,
        medicalBenefit = "Sabit noktaya bakmaktan tutulan 6 ekstraoküler göz kasını esnetir, göz arkasındaki baskı ve zonklamayı giderir."
    ),
    NEAR_FAR(
        title = "Yakın-Uzak Odak",
        durationLabel = "30 Sn",
        icon = Icons.Default.Spa,
        medicalBenefit = "Göz merceğinin odaklanma refleksini tazeler, ekrandan kalktıktan sonra oluşan geçici bulanık görmeyi (yalancı miyopi) çözer."
    ),
    PALMING(
        title = "Sıcak Palming",
        durationLabel = "30 Sn",
        icon = Icons.Default.SelfImprovement,
        medicalBenefit = "Sıcaklık ve zifiri karanlık ile retinadaki fotoreseptörleri sıfırlar, göz sinirini derinlemesine dinlendirir."
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedExerciseScreen(
    onComplete: (durationSeconds: Int, routineName: String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedExercise by remember { mutableStateOf(ExerciseType.LOOK_AWAY) }
    var isFinished by remember { mutableStateOf(false) }
    var totalSecondsSpent by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.testTag("animated_exercise_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Klinik Göz Dinlendirme Rehberi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "20-20-20 & Göz Kuruluğu Egzersizleri",
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
            if (isFinished) {
                CompletionCard(
                    totalSeconds = totalSecondsSpent.coerceAtLeast(20),
                    onFinish = {
                        onComplete(totalSecondsSpent.coerceAtLeast(20), "Göz Rahatlatma Seansı")
                        onClose()
                    }
                )
            } else {
                // Exercise Tabs
                ScrollableTabRow(
                    selectedTabIndex = selectedExercise.ordinal,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ExerciseType.values().forEach { ex ->
                        Tab(
                            selected = selectedExercise == ex,
                            onClick = { selectedExercise = ex },
                            text = {
                                Text(
                                    text = ex.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedExercise == ex) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = ex.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Exercise Content
                AnimatedContent(
                    targetState = selectedExercise,
                    label = "exercise_transition"
                ) { current ->
                    when (current) {
                        ExerciseType.LOOK_AWAY -> LookAwayInteractiveExercise(
                            onComplete = {
                                totalSecondsSpent += 20
                                selectedExercise = ExerciseType.BLINKING
                            },
                            onFinishSession = {
                                totalSecondsSpent += 20
                                isFinished = true
                            }
                        )
                        ExerciseType.BLINKING -> BlinkingInteractiveExercise(
                            onComplete = {
                                totalSecondsSpent += 30
                                selectedExercise = ExerciseType.STRETCH
                            },
                            onFinishSession = {
                                totalSecondsSpent += 30
                                isFinished = true
                            }
                        )
                        ExerciseType.STRETCH -> StretchInteractiveExercise(
                            onComplete = {
                                totalSecondsSpent += 30
                                selectedExercise = ExerciseType.NEAR_FAR
                            },
                            onFinishSession = {
                                totalSecondsSpent += 30
                                isFinished = true
                            }
                        )
                        ExerciseType.NEAR_FAR -> NearFarInteractiveExercise(
                            onComplete = {
                                totalSecondsSpent += 30
                                selectedExercise = ExerciseType.PALMING
                            },
                            onFinishSession = {
                                totalSecondsSpent += 30
                                isFinished = true
                            }
                        )
                        ExerciseType.PALMING -> PalmingInteractiveExercise(
                            onFinishSession = {
                                totalSecondsSpent += 30
                                isFinished = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * 1. 20-20-20 Look Away Exercise
 * Focus away from the screen for 20 seconds. Haptic feedback tells the user when it's done so they DON'T have to stare at their screen!
 */
@Composable
fun LookAwayInteractiveExercise(
    onComplete: () -> Unit,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    var secondsLeft by remember { mutableIntStateOf(20) }
    var isRunning by remember { mutableStateOf(true) }

    LaunchedEffect(isRunning, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000L)
            secondsLeft--
            if (secondsLeft == 0) {
                VibrationHelper.vibrateCompletion(context)
            }
        }
    }

    ExerciseContainer(
        title = "1. Uzağa Bakış (20-20-20 Kuralı)",
        instruction = "Gözlerinizi ekrandan kaldırın! En az 6 metre (20 feet) uzaktaki pencereden dışarı, ağaca veya odanın en uzak noktasına bakın.",
        hint = "📳 Süre bittiğinde telefonunuz titreyecektir. Ekrana bakmanıza gerek yoktur!",
        benefit = ExerciseType.LOOK_AWAY.medicalBenefit
    ) {
        // Visual Countdown Display
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surface
                        )
                    )
                )
                .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (secondsLeft > 0) {
                    Text(
                        text = "$secondsLeft",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "saniye uzağa bakın",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Harika! Kaslar Gevşedi ✨",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controls
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                Text(if (isRunning) "Duraklat" else "Devam Et")
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
                Text("Yeniden Başlat")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtons(
            onFinishSession = onFinishSession,
            onNext = onComplete,
            nextLabel = "Sonraki: Kırpma Masajı 👉"
        )
    }
}

/**
 * 2. Blinking & Meibomian Gland Refresh
 * 3-phase blinking: Close (2s) -> Gently squeeze (2s) -> Open (2s)
 */
@Composable
fun BlinkingInteractiveExercise(
    onComplete: () -> Unit,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    var currentPhase by remember { mutableIntStateOf(0) } // 0: Kapat, 1: Nazikçe Sık, 2: Aç ve Gevşet
    var repetition by remember { mutableIntStateOf(1) }
    val maxRepetitions = 5
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(isDone) {
        if (!isDone) {
            while (repetition <= maxRepetitions) {
                // Phase 0: Close
                currentPhase = 0
                VibrationHelper.vibrateStepChange(context)
                delay(2000L)

                // Phase 1: Squeeze
                currentPhase = 1
                VibrationHelper.vibrateStepChange(context)
                delay(2000L)

                // Phase 2: Open
                currentPhase = 2
                VibrationHelper.vibrateStepChange(context)
                delay(2000L)

                repetition++
            }
            isDone = true
            VibrationHelper.vibrateCompletion(context)
        }
    }

    ExerciseContainer(
        title = "2. Bilinçli Kırpma & Gözyaşı Masajı",
        instruction = "Ekrana odaklanırken göz kırpma refleksimiz %66 azalır. Bu 3 aşamalı döngü gözün nem tabakasını yeniler.",
        hint = "💧 Tekrar: ${repetition.coerceAtMost(maxRepetitions)} / $maxRepetitions",
        benefit = ExerciseType.BLINKING.medicalBenefit
    ) {
        val phaseText = when (currentPhase) {
            0 -> "1. Gözlerini Nazikçe KAPAT 😌"
            1 -> "2. Göz Kapaklarını Hafifçe SIK (Bezleri uyar) 💆"
            else -> "3. Gözlerini AÇ ve Gevşet ✨"
        }

        val phaseColor by animateColorAsState(
            targetValue = when (currentPhase) {
                0 -> MaterialTheme.colorScheme.secondary
                1 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            },
            label = "phase_color"
        )

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = phaseColor.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isDone) "Gözyaşı Tabakası Yenilendi! 💧" else phaseText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = phaseColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                LinearProgressIndicator(
                    progress = { repetition.toFloat() / maxRepetitions },
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = phaseColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtons(
            onFinishSession = onFinishSession,
            onNext = onComplete,
            nextLabel = "Sonraki: 4 Yönlü Esnetme 👉"
        )
    }
}

/**
 * 3. 4-Directional Extraocular Muscle Stretch
 */
@Composable
fun StretchInteractiveExercise(
    onComplete: () -> Unit,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    // 0: Yukarı, 1: Aşağı, 2: Sola, 3: Sağa
    var directionIndex by remember { mutableIntStateOf(0) }
    var secondsInDirection by remember { mutableIntStateOf(3) }
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(isDone) {
        if (!isDone) {
            for (dir in 0..3) {
                directionIndex = dir
                VibrationHelper.vibrateStepChange(context)
                for (sec in 3 downTo 1) {
                    secondsInDirection = sec
                    delay(1000L)
                }
            }
            isDone = true
            VibrationHelper.vibrateCompletion(context)
        }
    }

    val (directionName, directionIcon) = when (directionIndex) {
        0 -> "YUKARI BAKIN ⬆️" to Icons.Default.KeyboardArrowUp
        1 -> "AŞAĞI BAKIN ⬇️" to Icons.Default.KeyboardArrowDown
        2 -> "SOLA BAKIN ⬅️" to Icons.Default.KeyboardArrowLeft
        else -> "SAĞA BAKIN ➡️" to Icons.Default.KeyboardArrowRight
    }

    ExerciseContainer(
        title = "3. 4 Yönlü Göz Kası Esnetme",
        instruction = "Başınızı kesinlikle çevirmeyin! Sadece göz bebeklerinizle gösterilen yöne en uzağa bakın.",
        hint = "🎯 6 ekstraoküler göz kasının tutulmasını çözer.",
        benefit = ExerciseType.STRETCH.medicalBenefit
    ) {
        Box(
            modifier = Modifier
                .size(180.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = directionIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = directionName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "$secondsInDirection sn",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtons(
            onFinishSession = onFinishSession,
            onNext = onComplete,
            nextLabel = "Sonraki: Yakın-Uzak Odak 👉"
        )
    }
}

/**
 * 4. Near-Far Accommodation Flexibility Exercise
 */
@Composable
fun NearFarInteractiveExercise(
    onComplete: () -> Unit,
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    var isNear by remember { mutableStateOf(true) }
    var cycleCount by remember { mutableIntStateOf(1) }
    val maxCycles = 4
    var isDone by remember { mutableStateOf(false) }

    LaunchedEffect(isDone) {
        if (!isDone) {
            while (cycleCount <= maxCycles) {
                isNear = true
                VibrationHelper.vibrateStepChange(context)
                delay(3000L)

                isNear = false
                VibrationHelper.vibrateStepChange(context)
                delay(3000L)

                cycleCount++
            }
            isDone = true
            VibrationHelper.vibrateCompletion(context)
        }
    }

    ExerciseContainer(
        title = "4. Yakın-Uzak Odak Atlama",
        instruction = "Başparmağınızı burnunuzdan 20 cm uzağa tutun.",
        hint = "Döngü: ${cycleCount.coerceAtMost(maxCycles)} / $maxCycles",
        benefit = ExerciseType.NEAR_FAR.medicalBenefit
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNear) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.tertiaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isNear) "🔍 PARMAĞINA ODAKLAN" else "🏔️ EN UZAK NOKTAYA ODAKLAN",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isNear) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isNear)
                        "Tırnağının kenar çizgilerini net görene kadar dikkatlice bak (3 sn)."
                    else
                        "Parmağının arkasından odanın en uzak noktasına veya gökyüzüne bak (3 sn).",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActionButtons(
            onFinishSession = onFinishSession,
            onNext = onComplete,
            nextLabel = "Sonraki: Sıcak Palming 👉"
        )
    }
}

/**
 * 5. Warm Palming Exercise
 */
@Composable
fun PalmingInteractiveExercise(
    onFinishSession: () -> Unit
) {
    val context = LocalContext.current
    var isWarmingHands by remember { mutableStateOf(true) }
    var secondsLeft by remember { mutableIntStateOf(10) }

    LaunchedEffect(isWarmingHands) {
        if (isWarmingHands) {
            secondsLeft = 10
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            isWarmingHands = false
            VibrationHelper.vibrateCompletion(context)
            secondsLeft = 20
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            VibrationHelper.vibrateCompletion(context)
        }
    }

    ExerciseContainer(
        title = "5. Sıcak Avuç İçi Masajı (Palming)",
        instruction = if (isWarmingHands)
            "Ellerinizi 10 saniye boyunca hızlıca birbirine sürterek avuç içlerinizi ısıtın! 🔥"
        else
            "Sıcak avuçlarınızı gözlerinizin üzerine kubbe şeklinde kapatın (baskı yapmayın). Zifiri karanlığı ve sıcaklığı hissedin. 🌌",
        hint = "🧘 Fotoreseptör hücrelerini sıfırlar ve göz sinirini sakinleştirir.",
        benefit = ExerciseType.PALMING.medicalBenefit
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(
                    if (isWarmingHands)
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else
                        Color(0xFF1E1E2E)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isWarmingHands) "🔥 ELLERİNİ SÜRT" else "🌌 DERİN NEFES AL",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isWarmingHands) MaterialTheme.colorScheme.onErrorContainer else Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$secondsLeft sn",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWarmingHands) MaterialTheme.colorScheme.error else Color(0xFF89B4FA)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinishSession,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Seansı Başarıyla Tamamla ✨")
        }
    }
}

@Composable
fun ExerciseContainer(
    title: String,
    instruction: String,
    hint: String,
    benefit: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = instruction,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = hint,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            content()

            Spacer(modifier = Modifier.height(16.dp))

            // Medical benefit box
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
                        text = benefit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    onFinishSession: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilledTonalButton(
            onClick = onFinishSession,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Molayı Bitir")
        }

        Button(
            onClick = onNext,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(nextLabel)
        }
    }
}

@Composable
fun CompletionCard(
    totalSeconds: Int,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
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

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Gözleriniz Dinlendi! 🌿",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Bu seansla siliyer odak kaslarınızı gevşettiniz ve kornea nem tabakasını tazelediniz. Dijital göz yorgunluğunu önleme yolunda harika bir adım attınız!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onFinish,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Harika, Çalışmaya Devam Et ✨")
            }
        }
    }
}
