package com.luanmuc.openwrtmanager.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.luanmuc.openwrtmanager.data.model.CommandHistory
import com.luanmuc.openwrtmanager.data.model.CursorStyle
import com.luanmuc.openwrtmanager.data.model.QuickCommand
import com.luanmuc.openwrtmanager.data.model.TerminalConfig
import com.luanmuc.openwrtmanager.data.model.TerminalTheme
import com.luanmuc.openwrtmanager.util.DebugMode
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import com.luanmuc.openwrtmanager.R
import com.luanmuc.openwrtmanager.util.Loc

/**
 * 终端Repository
 * 负责终端配置、命令历史、快捷命令等功能
 */
class TerminalRepository private constructor(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("terminal_config", Context.MODE_PRIVATE)
    private val luciRepository: LuciRepository = LuciRepository.getInstance(context)
    
    /**
     * 获取终端配置
     */
    fun getTerminalConfig(): TerminalConfig {
        return TerminalConfig(
            fontSize = prefs.getInt("fontSize", 14),
            fontFamily = prefs.getString("fontFamily", "monospace") ?: "monospace",
            theme = TerminalTheme.valueOf(prefs.getString("theme", TerminalTheme.DARK.name) ?: TerminalTheme.DARK.name),
            scrollbackSize = prefs.getInt("scrollbackSize", 1000),
            bellEnabled = prefs.getBoolean("bellEnabled", true),
            cursorStyle = CursorStyle.valueOf(prefs.getString("cursorStyle", CursorStyle.BLOCK.name) ?: CursorStyle.BLOCK.name)
        )
    }
    
    /**
     * 保存终端配置
     */
    fun saveTerminalConfig(config: TerminalConfig) {
        prefs.edit()
            .putInt("fontSize", config.fontSize)
            .putString("fontFamily", config.fontFamily)
            .putString("theme", config.theme.name)
            .putInt("scrollbackSize", config.scrollbackSize)
            .putBoolean("bellEnabled", config.bellEnabled)
            .putString("cursorStyle", config.cursorStyle.name)
            .apply()
    }
    
    /**
     * 获取命令历史记录
     */
    fun getCommandHistory(limit: Int = 50): List<CommandHistory> {
        if (DebugMode.isDebugMode) {
            return getFakeHistory()
        }
        
        val json = prefs.getString("command_history", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            val history = mutableListOf<CommandHistory>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                history.add(
                    CommandHistory(
                        id = obj.optLong("id", 0),
                        command = obj.optString("command", ""),
                        output = obj.optString("output", ""),
                        exitCode = obj.optInt("exitCode", 0),
                        timestamp = obj.optLong("timestamp", 0),
                        duration = obj.optLong("duration", 0)
                    )
                )
            }
            history.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * 添加命令历史记录
     */
    fun addCommandHistory(history: CommandHistory) {
        val allHistory = getCommandHistory(100).toMutableList()
        allHistory.add(0, history)
        val limited = allHistory.take(100)
        
        val arr = JSONArray()
        for (h in limited) {
            arr.put(JSONObject().apply {
                put("id", h.id)
                put("command", h.command)
                put("output", h.output)
                put("exitCode", h.exitCode)
                put("timestamp", h.timestamp)
                put("duration", h.duration)
            })
        }
        prefs.edit().putString("command_history", arr.toString()).apply()
    }
    
    /**
     * 清空命令历史
     */
    fun clearCommandHistory() {
        prefs.edit().remove("command_history").apply()
    }
    
    /**
     * 获取快捷命令列表
     */
    fun getQuickCommands(): List<QuickCommand> {
        val json = prefs.getString("quick_commands", null) ?: return getDefaultQuickCommands()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                QuickCommand(
                    id = obj.optString("id", ""),
                    name = obj.optString("name", ""),
                    command = obj.optString("command", ""),
                    description = obj.optString("description", ""),
                    category = obj.optString("category", Loc.s(R.string.s0015))
                )
            }
        } catch (e: Exception) {
            getDefaultQuickCommands()
        }
    }
    
    /**
     * 获取默认快捷命令
     */
    private fun getDefaultQuickCommands(): List<QuickCommand> {
        return listOf(
            QuickCommand(
                id = "q1",
                name = Loc.s(R.string.s0220),
                command = "uname -a",
                description = Loc.s(R.string.s0221),
                category = Loc.s(R.string.s0222)
            ),
            QuickCommand(
                id = "q2",
                name = Loc.s(R.string.s0223),
                command = "free -h",
                description = Loc.s(R.string.s0224),
                category = Loc.s(R.string.s0222)
            ),
            QuickCommand(
                id = "q3",
                name = Loc.s(R.string.s0225),
                command = "df -h",
                description = Loc.s(R.string.s0226),
                category = Loc.s(R.string.s0222)
            ),
            QuickCommand(
                id = "q4",
                name = Loc.s(R.string.s0227),
                command = "netstat -tlnp",
                description = Loc.s(R.string.s0228),
                category = Loc.s(R.string.s0174)
            ),
            QuickCommand(
                id = "q5",
                name = Loc.s(R.string.s0229),
                command = "route -n",
                description = Loc.s(R.string.s0230),
                category = Loc.s(R.string.s0174)
            ),
            QuickCommand(
                id = "q6",
                name = Loc.s(R.string.s0231),
                command = "ps aux",
                description = Loc.s(R.string.s0232),
                category = Loc.s(R.string.s0222)
            ),
            QuickCommand(
                id = "q7",
                name = Loc.s(R.string.s0233),
                command = "reboot",
                description = Loc.s(R.string.s0233),
                category = Loc.s(R.string.s0222)
            ),
            QuickCommand(
                id = "q8",
                name = Loc.s(R.string.s0234),
                command = "cat /proc/cpuinfo",
                description = Loc.s(R.string.s0235),
                category = Loc.s(R.string.s0222)
            )
        )
    }
    
    /**
     * 保存快捷命令
     */
    fun saveQuickCommands(commands: List<QuickCommand>) {
        val arr = JSONArray()
        for (cmd in commands) {
            arr.put(JSONObject().apply {
                put("id", cmd.id)
                put("name", cmd.name)
                put("command", cmd.command)
                put("description", cmd.description)
                put("category", cmd.category)
            })
        }
        prefs.edit().putString("quick_commands", arr.toString()).apply()
    }
    
    /**
     * 执行命令（演示模式）
     */
    suspend fun executeCommand(command: String): CommandHistory {
        val startTime = System.currentTimeMillis()
        
        if (DebugMode.isDebugMode) {
            delay(500 + Math.random().toLong() * 500)
            val output = getFakeCommandOutput(command)
            val endTime = System.currentTimeMillis()
            return CommandHistory(
                id = endTime,
                command = command,
                output = output,
                exitCode = 0,
                timestamp = startTime,
                duration = endTime - startTime
            )
        }
        
        // 真实模式：通过LuCI RPC执行命令
        return try {
            val output = luciRepository.executeCommand(command) ?: Loc.s(R.string.s0236)
            val endTime = System.currentTimeMillis()
            CommandHistory(
                id = endTime,
                command = command,
                output = output,
                exitCode = if (output.contains("not found") || output.contains("error")) 1 else 0,
                timestamp = startTime,
                duration = endTime - startTime
            )
        } catch (e: Exception) {
            val endTime = System.currentTimeMillis()
            CommandHistory(
                id = endTime,
                command = command,
                output = Loc.s(R.string.s0237, e.message ?: Loc.s(R.string.s0115)),
                exitCode = -1,
                timestamp = startTime,
                duration = endTime - startTime
            )
        }
    }
    
    /**
     * 假数据：命令输出
     */
    private fun getFakeCommandOutput(command: String): String {
        return when {
            command.contains("uname", true) -> """
                Linux OpenWrt-Router 5.15.120 #0 SMP Mon Jan 15 10:30:00 2024 x86_64 GNU/Linux
            """.trimIndent()
            command.contains("free", true) -> """
                     total        used        free      shared  buff/cache   available
                Mem:       512000      187000      200000        5000      125000      300000
                Swap:           0           0           0
            """.trimIndent()
            command.contains("df", true) -> """
                Filesystem                Size      Used Available Use% Mounted on
                /dev/root               128.0M     45.0M     83.0M  35% /
                tmpfs                   256.0M     10.0M    246.0M   4% /tmp
                tmpfs                   512.0K         0    512.0K   0% /dev
            """.trimIndent()
            command.contains("netstat", true) -> """
                Active Internet connections (only servers)
                Proto Recv-Q Send-Q Local Address           Foreign Address         State
                tcp        0      0 0.0.0.0:22              0.0.0.0:*               LISTEN
                tcp        0      0 0.0.0.0:80              0.0.0.0:*               LISTEN
                tcp        0      0 0.0.0.0:443             0.0.0.0:*               LISTEN
                udp        0      0 0.0.0.0:53              0.0.0.0:*
            """.trimIndent()
            command.contains("ps", true) -> """
                PID USER      VSZ STAT COMMAND
                  1 root     1500 S    init
                  2 root        0 SW   [kthreadd]
                  3 root        0 SW   [ksoftirqd/0]
                123 root     2000 S    /sbin/netifd
                456 root     3000 S    /usr/sbin/uhttpd
                789 root     1000 S    /usr/sbin/dropbear
            """.trimIndent()
            command.contains("ifconfig", true) || command.contains("ip addr", true) -> """
                eth0      Link encap:Ethernet  HWaddr 00:11:22:33:44:55
                          inet addr:192.168.1.1  Bcast:192.168.1.255  Mask:255.255.255.0
                          UP BROADCAST RUNNING MULTICAST  MTU:1500  Metric:1
                          RX packets:12345 errors:0 dropped:0 overruns:0 frame:0
                          TX packets:6789 errors:0 dropped:0 overruns:0 carrier:0

                lo        Link encap:Local Loopback
                          inet addr:127.0.0.1  Mask:255.0.0.0
                          UP LOOPBACK RUNNING  MTU:65536  Metric:1
            """.trimIndent()
            command.contains("uptime", true) -> """
                 14:30:45 up 5 days, 3:42, load average: 0.15, 0.08, 0.05
            """.trimIndent()
            command.contains("openwrt_release", true) || command.contains("cat /etc", true) -> """
                DISTRIB_ID='OpenWrt'
                DISTRIB_RELEASE='23.05.0'
                DISTRIB_REVISION='r23497-6637af95aa'
                DISTRIB_TARGET='x86/64'
                DISTRIB_ARCH='x86_64'
                DISTRIB_DESCRIPTION='OpenWrt 23.05.0'
            """.trimIndent()
            command.contains("logread", true) -> """
                Mon Aug 12 14:30:01 2024 cron.info crond[123]: (root) CMD (/usr/bin/ddns_update)
                Mon Aug 12 14:25:00 2024 daemon.info dnsmasq[456]: read /etc/hosts - 5 addresses
                Mon Aug 12 14:20:15 2024 kern.info kernel: [12345.678] eth0: link up, 1000 Mbps full-duplex
                Mon Aug 12 14:15:00 2024 user.notice firewall: Reloading firewall due to ifup of lan
            """.trimIndent()
            command.contains("top", true) -> """
                Mem: 187000K used, 325000K free, 5000K shrd, 125000K buff
                Load: 0.15 0.08 0.05
                CPU:  5% usr  3% sys  0% nic 90% idle  2% io  0% irq
                  PID  PPID USER     STAT   VSZ %VSZ %CPU COMMAND
                  456     1 root     S     3000   1%   2% /usr/sbin/uhttpd
                  123     1 root     S     2000   0%   1% /sbin/netifd
                  789     1 root     S     1000   0%   0% /usr/sbin/dropbear
            """.trimIndent()
            command.contains("help", true) -> """
                常用命令:
                  uname -a    - 系统信息
                  free -h     - 内存使用
                  df -h       - 磁盘使用
                  uptime      - 运行时间
                  ps          - 进程列表
                  ifconfig    - 网络接口
                  netstat     - 网络连接
                  logread     - 系统日志
                  top         - 系统监控
                  cat /etc/openwrt_release - 版本信息
            """.trimIndent()
            command == "clear" || command == "cls" -> ""
            else -> Loc.s(R.string.s0238, command)
        }
    }
    
    /**
     * 假数据：历史记录
     */
    private fun getFakeHistory(): List<CommandHistory> {
        val now = System.currentTimeMillis()
        return listOf(
            CommandHistory(
                id = now - 3600000,
                command = "uname -a",
                output = "Linux OpenWrt-Router 5.15.120 #0 SMP x86_64 GNU/Linux",
                exitCode = 0,
                timestamp = now - 3600000,
                duration = 100
            ),
            CommandHistory(
                id = now - 1800000,
                command = "df -h",
                output = "Filesystem Size Used Available Use% Mounted on\n/dev/root 128.0M 45.0M 83.0M 35% /",
                exitCode = 0,
                timestamp = now - 1800000,
                duration = 150
            ),
            CommandHistory(
                id = now - 600000,
                command = "free -h",
                output = "total used free shared buff/cache available\nMem: 512000 187000 200000 5000 125000 300000",
                exitCode = 0,
                timestamp = now - 600000,
                duration = 80
            )
        )
    }
    
    companion object {
        @Volatile
        private var instance: TerminalRepository? = null
        
        fun getInstance(context: Context): TerminalRepository {
            return instance ?: synchronized(this) {
                instance ?: TerminalRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}
