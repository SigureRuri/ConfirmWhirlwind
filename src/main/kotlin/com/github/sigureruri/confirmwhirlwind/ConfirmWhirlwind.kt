package com.github.sigureruri.confirmwhirlwind

import org.bukkit.ChatColor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.*

class ConfirmWhirlwind : JavaPlugin(), Listener {

    private val commandsToConfirm by lazy {
        config.getStringList("commands-to-confirm")
            .filterNotNull()
            .map { it.toRegex() }
    }

    private val commandsWaitingForConfirming = mutableMapOf<UUID, String>()

    override fun onEnable() {
        saveDefaultConfig()

        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    fun onProcessingCommand(event: PlayerCommandPreprocessEvent) {
        val player = event.player
        val uuid = player.uniqueId
        val executedCommand = event.message
        val executedCommandWithoutSlash = executedCommand.removePrefix("/")

        // 確認が必要なコマンドでない || (入力されたコマンドが待機中のコマンドと一致する)
        //     -> 待機中リストから削除し、コマンドは実行
        // それ以外
        //     -> 待機中コマンドを追加/上書きしてイベントをキャンセル
        if (!commandsToConfirm.any { it.matches(executedCommandWithoutSlash) }
            || commandsWaitingForConfirming[uuid] == executedCommandWithoutSlash) {
            commandsWaitingForConfirming.remove(uuid)
        } else {
            commandsWaitingForConfirming[uuid] = executedCommandWithoutSlash
            event.isCancelled = true

            logger.info("${player.name} tried to issue command: $executedCommand")

            listOf(
                "${ChatColor.DARK_RED}！！  ${ChatColor.RED}${ChatColor.BOLD}確認  ${ChatColor.DARK_RED}！！",
                " ${ChatColor.RED}間違い防止の為、もう一度",
                " ${ChatColor.WHITE}[${executedCommand}]${ChatColor.RED}を実行してください。"
            ).forEach { context.sender.sendMessage(it) }
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        commandsWaitingForConfirming.remove(event.player.uniqueId)
    }

}
