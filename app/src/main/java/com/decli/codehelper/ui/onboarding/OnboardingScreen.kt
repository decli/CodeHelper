package com.decli.codehelper.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.decli.codehelper.ui.components.ActionButton
import com.decli.codehelper.ui.components.TextActionButton
import com.decli.codehelper.ui.components.motionDuration

/** 首次启动引导：3 步，每步只讲一件事、只做一个动作，权限申请前先说清楚「为什么」 */
enum class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val primaryAction: String,
    val skipAction: String?,
) {
    Intro(
        icon = Icons.Rounded.Inventory2,
        title = "取件码，一眼看到",
        description = "自动从短信里找出取件码，用最大的字显示出来。不联网、不上传，只在这台手机上工作。",
        primaryAction = "开始使用",
        skipAction = null,
    ),
    SmsPermission(
        icon = Icons.Rounded.VerifiedUser,
        title = "需要允许读取短信",
        description = "取件码都在短信里。接下来系统会弹出询问，请选择「允许」。我们不会发送或修改任何短信。",
        primaryAction = "允许读取短信",
        skipAction = "以后再说",
    ),
    Notification(
        icon = Icons.Rounded.NotificationsActive,
        title = "桌面图标显示待取数量",
        description = "开启通知后，桌面图标角标会显示还有几个包裹没取。不会打扰您，没有声音。",
        primaryAction = "开启通知",
        skipAction = "暂不开启",
    ),
}

@Composable
fun OnboardingScreen(
    step: OnboardingStep,
    onPrimaryAction: () -> Unit,
    onSkip: () -> Unit,
) {
    // 第 1 步禁用系统返回：还没开始就退出没有意义
    BackHandler(enabled = step == OnboardingStep.Intro) {}

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StepIndicator(currentIndex = step.ordinal)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(RoundedCornerShape(44.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = step.description,
                    modifier = Modifier.widthIn(max = 330.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 20.sp,
                        lineHeight = 32.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            ActionButton(
                text = step.primaryAction,
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                height = 64.dp,
                shape = RoundedCornerShape(20.dp),
                textStyle = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
            )
            if (step.skipAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    TextActionButton(
                        text = step.skipAction,
                        icon = null,
                        onClick = onSkip,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        height = 56.dp,
                        textStyle = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(
    currentIndex: Int,
) {
    Row(
        modifier = Modifier.height(24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingStep.entries.forEachIndexed { index, _ ->
            val width by animateDpAsState(
                targetValue = if (index == currentIndex) 28.dp else 10.dp,
                animationSpec = tween(durationMillis = motionDuration(250)),
                label = "stepDotWidth",
            )
            Box(
                modifier = Modifier
                    .width(width)
                    .height(10.dp)
                    .graphicsLayer { alpha = if (index <= currentIndex) 1f else 0.3f }
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
