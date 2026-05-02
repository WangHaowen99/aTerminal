package com.aterminal.app.settings

enum class AppLanguage(
    val storageValue: String,
    val displayLabel: String,
) {
    ENGLISH("en", "English"),
    CHINESE("zh", "中文");

    companion object {
        fun fromStorageValue(value: String?): AppLanguage {
            return entries.firstOrNull { it.storageValue == value } ?: ENGLISH
        }
    }
}

data class SettingsStrings(
    val policyEyebrow: String,
    val settingsTitle: String,
    val settingsDescription: String,
    val languageTitle: String,
    val languageDescription: String,
    val agentDefaultsTitle: String,
    val codexDefaultFlagsLabel: String,
    val claudeDefaultFlagsLabel: String,
    val agentDefaultsDescription: String,
    val metadataTransferTitle: String,
    val metadataTransferDescription: String,
    val exportMetadata: String,
    val pasteMetadataJson: String,
    val importMetadata: String,
    val privacyTitle: String,
    val privacyNotice: String,
)

fun settingsStrings(language: AppLanguage): SettingsStrings {
    return when (language) {
        AppLanguage.ENGLISH -> SettingsStrings(
            policyEyebrow = "LOCAL POLICY",
            settingsTitle = "Settings",
            settingsDescription = "Configure default agent flags, SSH behavior, and reading-mode preferences.",
            languageTitle = "Language",
            languageDescription = "Choose the display language for navigation and core settings.",
            agentDefaultsTitle = "Agent defaults",
            codexDefaultFlagsLabel = "Codex default flags",
            claudeDefaultFlagsLabel = "Claude default flags",
            agentDefaultsDescription = "Flags are split like shell arguments and appended to one-tap Codex or Claude launches.",
            metadataTransferTitle = "Metadata transfer",
            metadataTransferDescription = "Export/import hosts and workspaces only. Passwords and private keys are never included.",
            exportMetadata = "Export metadata",
            pasteMetadataJson = "Paste metadata JSON",
            importMetadata = "Import metadata",
            privacyTitle = "Privacy",
            privacyNotice = PrivacyNotice,
        )

        AppLanguage.CHINESE -> SettingsStrings(
            policyEyebrow = "本地策略",
            settingsTitle = "设置",
            settingsDescription = "配置默认 Agent 参数、SSH 行为和阅读模式偏好。",
            languageTitle = "语言",
            languageDescription = "选择导航和核心设置的显示语言。",
            agentDefaultsTitle = "Agent 默认参数",
            codexDefaultFlagsLabel = "Codex 默认参数",
            claudeDefaultFlagsLabel = "Claude 默认参数",
            agentDefaultsDescription = "参数会按 shell 参数解析，并追加到一键启动 Codex 或 Claude 的命令中。",
            metadataTransferTitle = "元数据迁移",
            metadataTransferDescription = "仅导出/导入主机和工作区。密码和私钥永远不会包含在内。",
            exportMetadata = "导出元数据",
            pasteMetadataJson = "粘贴元数据 JSON",
            importMetadata = "导入元数据",
            privacyTitle = "隐私",
            privacyNotice = "终端内容只保留在本设备和远程主机上，除非你主动导出日志或元数据。",
        )
    }
}
