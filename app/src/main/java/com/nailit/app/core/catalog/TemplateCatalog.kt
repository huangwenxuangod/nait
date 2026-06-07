package com.nailit.app.core.catalog

import androidx.annotation.DrawableRes
import com.nailit.app.R

data class TemplateItem(
    val id: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val imageRes: Int,
    val storagePath: String,
    val sourceAliases: List<String> = emptyList(),
)

object TemplateCatalog {
    val items: List<TemplateItem> = listOf(
        TemplateItem(
            id = "ice-blue-butter-gradient",
            title = "冰川蓝奶油黄渐变",
            subtitle = "蓝黄撞色刷痕 + 金色星点",
            imageRes = R.drawable.ice_blue_butter_gradient,
            storagePath = "tutorials/preset-templates/ice-blue-butter-gradient.jpg",
            sourceAliases = listOf("冰川蓝奶油黄渐变", "ice-blue-butter-gradient"),
        ),
        TemplateItem(
            id = "ice-bear-hand-paint",
            title = "冰糯小熊手绘",
            subtitle = "雾蓝奶白格纹 + 童趣小熊",
            imageRes = R.drawable.ice_bear_hand_paint,
            storagePath = "tutorials/preset-templates/ice-bear-hand-paint.jpg",
            sourceAliases = listOf("冰糯小熊手绘", "ice-bear-hand-paint"),
        ),
        TemplateItem(
            id = "huanhuan-nude",
            title = "嬛嬛裸色",
            subtitle = "低饱和裸粉光泽 + 温柔通勤感",
            imageRes = R.drawable.huanhuan_nude,
            storagePath = "tutorials/preset-templates/huanhuan-nude.jpg",
            sourceAliases = listOf("嬛嬛裸色", "huanhuan-nude"),
        ),
        TemplateItem(
            id = "dark-matte",
            title = "暗黑风磨砂",
            subtitle = "通体黑灰磨砂 + 冷感酷系",
            imageRes = R.drawable.dark_matte,
            storagePath = "tutorials/preset-templates/dark-matte.jpg",
            sourceAliases = listOf("暗黑风磨砂", "dark-matte"),
        ),
        TemplateItem(
            id = "cherry-hailey",
            title = "樱桃海利",
            subtitle = "通透豆沙底 + 樱桃点缀",
            imageRes = R.drawable.cherry_hailey,
            storagePath = "tutorials/preset-templates/cherry-hailey.jpg",
            sourceAliases = listOf("樱桃海利", "cherry-hailey"),
        ),
        TemplateItem(
            id = "french-fade",
            title = "法式渐变",
            subtitle = "裸粉奶透底 + 极细法式晕染",
            imageRes = R.drawable.french_fade,
            storagePath = "tutorials/preset-templates/french-fade.jpg",
            sourceAliases = listOf("法式渐变", "french-fade"),
        ),
        TemplateItem(
            id = "pale-yellow-matte",
            title = "淡黄磨砂",
            subtitle = "奶油淡黄主色 + 留白线条",
            imageRes = R.drawable.pale_yellow_matte,
            storagePath = "tutorials/preset-templates/pale-yellow-matte.jpg",
            sourceAliases = listOf("淡黄磨砂", "pale-yellow-matte"),
        ),
        TemplateItem(
            id = "caramel-painted-gradient",
            title = "焦糖色渐变手绘",
            subtitle = "焦糖琥珀配色 + 手绘波点",
            imageRes = R.drawable.caramel_painted_gradient,
            storagePath = "tutorials/preset-templates/caramel-painted-gradient.jpg",
            sourceAliases = listOf("焦糖色渐变手绘", "caramel-painted-gradient"),
        ),
        TemplateItem(
            id = "dusty-pink-star-gradient",
            title = "粉灰渐变星星",
            subtitle = "粉灰奶雾渐变 + 极简星点",
            imageRes = R.drawable.dusty_pink_star_gradient,
            storagePath = "tutorials/preset-templates/dusty-pink-star-gradient.jpg",
            sourceAliases = listOf("粉灰渐变星星", "dusty-pink-star-gradient"),
        ),
        TemplateItem(
            id = "pink-blue-glass-cat-eye",
            title = "粉蓝渐变玻璃珠猫眼",
            subtitle = "粉蓝偏光猫眼 + 玻璃珠高光",
            imageRes = R.drawable.pink_blue_glass_cat_eye,
            storagePath = "tutorials/preset-templates/pink-blue-glass-cat-eye.jpg",
            sourceAliases = listOf("粉蓝渐变玻璃珠猫眼", "pink-blue-glass-cat-eye"),
        ),
        TemplateItem(
            id = "leopard-cat-eye",
            title = "豹纹猫眼",
            subtitle = "奶咖豹纹点缀 + 轻猫眼反光",
            imageRes = R.drawable.leopard_cat_eye,
            storagePath = "tutorials/preset-templates/leopard-cat-eye.jpg",
            sourceAliases = listOf("豹纹猫眼", "leopard-cat-eye"),
        ),
        TemplateItem(
            id = "green-apple-hand-paint",
            title = "青苹果汁手绘",
            subtitle = "果冻裸粉底 + 清新水果贴绘",
            imageRes = R.drawable.green_apple_hand_paint,
            storagePath = "tutorials/preset-templates/green-apple-hand-paint.jpg",
            sourceAliases = listOf("青苹果汁手绘", "green-apple-hand-paint"),
        ),
        TemplateItem(
            id = "blush-firework-cat",
            title = "腮红渐变烟花猫眼",
            subtitle = "腮红打底 + 细闪烟花猫眼",
            imageRes = R.drawable.blush_firework_cat,
            storagePath = "tutorials/preset-templates/blush-firework-cat.jpg",
            sourceAliases = listOf("blush-firework-cat"),
        ),
        TemplateItem(
            id = "mist-blue-cat-eye",
            title = "雾蓝银线猫眼",
            subtitle = "灰蓝渐层 + 银线冷光",
            imageRes = R.drawable.nail_showcase_02,
            storagePath = "tutorials/preset-templates/mist-blue-cat-eye.jpg",
            sourceAliases = listOf("mist-blue-cat-eye"),
        ),
    )

    val inspirationPreviewIds: List<String> = listOf(
        "ice-blue-butter-gradient",
        "ice-bear-hand-paint",
        "dark-matte",
        "pink-blue-glass-cat-eye",
    )

    fun findById(id: String?): TemplateItem? = items.firstOrNull { it.id == id }

    fun matchBySource(text: String): TemplateItem? {
        val normalized = text.lowercase()
        return items.firstOrNull { item ->
            item.id in normalized || item.sourceAliases.any { alias -> alias.lowercase() in normalized }
        }
    }
}
