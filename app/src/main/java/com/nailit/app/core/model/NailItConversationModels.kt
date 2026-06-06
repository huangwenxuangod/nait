package com.nailit.app.core.model

enum class NailItSender {
    Assistant,
    User,
}

enum class NailItCardType {
    SourcePlan,
    TryOnDecision,
    BomChecklist,
    SopStep,
    Feasibility,
}

data class NailItMessage(
    val id: String,
    val sender: NailItSender,
    val text: String,
    val card: NailItCard? = null,
)

data class NailItCard(
    val type: NailItCardType,
    val title: String,
    val subtitle: String,
    val bullets: List<String> = emptyList(),
    val cta: String? = null,
)

data class NailItQuickAction(
    val label: String,
    val prompt: String,
)
