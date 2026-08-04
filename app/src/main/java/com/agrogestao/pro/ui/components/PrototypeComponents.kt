package com.agrogestao.pro.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agrogestao.pro.ui.theme.AgroGreen100
import com.agrogestao.pro.ui.theme.CardBorder
import com.agrogestao.pro.ui.theme.PrimaryAgroGreen
import com.agrogestao.pro.ui.theme.SurfaceCard
import com.agrogestao.pro.ui.theme.SurfaceSoft
import com.agrogestao.pro.ui.theme.TextDark
import com.agrogestao.pro.ui.theme.TextMuted
import com.agrogestao.pro.ui.theme.TextSecondary

@Composable
fun AppScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    onAction: (() -> Unit)? = null,
    primaryAction: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(SurfaceSoft, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
            Spacer(Modifier.size(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextDark,
                fontSize = 19.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.ExtraBold
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (actionIcon != null && onAction != null) {
            IconButton(
                onClick = onAction,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (primaryAction) PrimaryAgroGreen else SurfaceSoft,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    imageVector = actionIcon,
                    contentDescription = actionDescription,
                    tint = if (primaryAction) Color.White else TextSecondary,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun AgroSectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = TextDark,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (action != null && onAction != null) {
            Text(
                text = action,
                color = PrimaryAgroGreen,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onAction).padding(6.dp)
            )
        }
    }
}

@Composable
fun PrototypeCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceCard,
    borderColor: Color = CardBorder,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content
    )
}

@Composable
fun SmallIconTile(
    icon: ImageVector,
    tint: Color = PrimaryAgroGreen,
    backgroundColor: Color = AgroGreen100,
    size: Int = 34
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(backgroundColor, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun MenuListItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color = TextSecondary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmallIconTile(icon = icon, tint = tint, backgroundColor = SurfaceSoft)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 11.dp)) {
            Text(title, color = TextDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = TextMuted, fontSize = 10.5.sp, lineHeight = 15.sp)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = CardBorder,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
fun InitialsAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim().split(Regex("\\s+")).filter(String::isNotBlank)
        .take(2).joinToString("") { it.take(1).uppercase() }.ifBlank { "AG" }
    Box(
        modifier = modifier
            .size(64.dp)
            .background(PrimaryAgroGreen, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(initials, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(20.dp)
                .background(SurfaceCard, CircleShape)
                .padding(3.dp)
                .background(PrimaryAgroGreen, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        color = TextMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
    )
    Spacer(Modifier.height(10.dp))
}
