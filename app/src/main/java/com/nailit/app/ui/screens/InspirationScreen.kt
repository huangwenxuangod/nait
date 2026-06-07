package com.nailit.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nailit.app.core.catalog.TemplateCatalog

private val InspirationBg = Color(0xFFF8F4F0)
private val InspirationText = Color(0xFF161211)
private val InspirationMuted = Color(0xFF7E726A)
private val InspirationCard = Color(0xFFFFFFFF)

@Composable
fun InspirationScreen(
    selectedId: String?,
    onSelectId: (String) -> Unit,
) {
    val items = TemplateCatalog.items

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InspirationBg)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "灵感",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = InspirationText,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.8).sp,
                ),
            )
            Text(
                text = "先挑一个大致喜欢的方向，再回首页开始试戴。",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = InspirationMuted,
                ),
            )
        }

        Surface(
            color = InspirationCard,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 4.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFFD4A3A3),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "热门美甲灵感",
                    modifier = Modifier.padding(start = 28.dp),
                    style = MaterialTheme.typography.titleSmall.copy(
                        color = InspirationText,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(items) { item ->
                val isSelected = item.id == selectedId
                Surface(
                    color = InspirationCard,
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = if (isSelected) 6.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectId(item.id) },
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(176.dp)
                                .clip(RoundedCornerShape(22.dp)),
                        ) {
                            Image(
                                painter = painterResource(item.imageRes),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Transparent,
                                                    Color(0x663A2621),
                                                )
                                            )
                                        )
                                )
                                Text(
                                    text = "当前已选",
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(12.dp),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    ),
                                )
                            }
                        }
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                color = InspirationText,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 2,
                        )
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = InspirationMuted,
                                lineHeight = 18.sp,
                            ),
                            maxLines = 2,
                        )
                    }
                }
            }
        }
    }
}
