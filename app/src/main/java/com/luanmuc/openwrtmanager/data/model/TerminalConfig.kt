package com.luanmuc.openwrtmanager.data.model

import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 终端配置
 */
data class TerminalConfig(
    val fontSize: Int = 14,
    val fontFamily: String = "monospace",
    val theme: TerminalTheme = TerminalTheme.DARK,
    val scrollbackSize: Int = 1000,
    val bellEnabled: Boolean = true,
    val cursorStyle: CursorStyle = CursorStyle.BLOCK
)

/**
 * 终端主题
 */
enum class TerminalTheme {
    DARK,
    LIGHT,
    SOLARIZED_DARK,
    SOLARIZED_LIGHT,
    DRACULA,
    MONOKAI;
    val displayName: String
        get() = when (this) {
            DARK -> Loc.s(R.string.enum_053)
            LIGHT -> Loc.s(R.string.enum_054)
            SOLARIZED_DARK -> Loc.s(R.string.enum_055)
            SOLARIZED_LIGHT -> Loc.s(R.string.enum_056)
            DRACULA -> "Dracula"
            MONOKAI -> "Monokai"
        }
}

/**
 * 光标样式
 */
enum class CursorStyle {
    BLOCK,
    UNDERLINE,
    BAR;
    val displayName: String
        get() = when (this) {
            BLOCK -> Loc.s(R.string.enum_057)
            UNDERLINE -> Loc.s(R.string.enum_058)
            BAR -> Loc.s(R.string.enum_059)
        }
}

/**
 * 命令历史记录
 */
data class CommandHistory(
    val id: Long = 0,
    val command: String = "",
    val output: String = "",
    val exitCode: Int = 0,
    val timestamp: Long = 0,
    val duration: Long = 0
)

/**
 * 快捷命令
 */
data class QuickCommand(
    val id: String = "",
    val name: String = "",
    val command: String = "",
    val description: String = "",
    val category: String = Loc.s(R.string.s0015)
)
