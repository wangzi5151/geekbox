package com.tinyai.geekbox.feature.system

data class ShellCommand(val title: String, val command: String)

data class ShellCategory(val name: String, val commands: List<ShellCommand>)

object ShellLibrary {

    val categories: List<ShellCategory> = listOf(
        ShellCategory(
            "设备与属性",
            listOf(
                ShellCommand("设备型号", "getprop ro.product.model"),
                ShellCommand("厂商", "getprop ro.product.manufacturer"),
                ShellCommand("品牌", "getprop ro.product.brand"),
                ShellCommand("设备代号", "getprop ro.product.device"),
                ShellCommand("产品名", "getprop ro.product.name"),
                ShellCommand("Android 版本", "getprop ro.build.version.release"),
                ShellCommand("SDK 等级", "getprop ro.build.version.sdk"),
                ShellCommand("安全补丁", "getprop ro.build.version.security_patch"),
                ShellCommand("Build ID", "getprop ro.build.id"),
                ShellCommand("Build 指纹", "getprop ro.build.fingerprint"),
                ShellCommand("Build 描述", "getprop ro.build.description"),
                ShellCommand("硬件平台", "getprop ro.hardware"),
                ShellCommand("主板", "getprop ro.board.platform"),
                ShellCommand("引导模式", "getprop ro.bootmode"),
                ShellCommand("主机名", "getprop net.hostname"),
                ShellCommand("内核版本", "cat /proc/version"),
                ShellCommand("全部属性", "getprop"),
                ShellCommand("属性排序", "getprop | sort"),
                ShellCommand("只看 ro.*", "getprop | grep '^\\[ro'"),
                ShellCommand("过滤 dalvik", "getprop | grep -i dalvik"),
                ShellCommand("过滤 wlan", "getprop | grep -i wlan"),
                ShellCommand("过滤 debug", "getprop | grep -i debug")
            )
        ),
        ShellCategory(
            "CPU / 内存",
            listOf(
                ShellCommand("CPU 信息", "cat /proc/cpuinfo"),
                ShellCommand("CPU 核心", "ls /sys/devices/system/cpu | grep cpu[0-9]"),
                ShellCommand("在线核心", "cat /sys/devices/system/cpu/online"),
                ShellCommand("核心0 当前频率", "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq"),
                ShellCommand("核心0 最大频率", "cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq"),
                ShellCommand("核心0 最小频率", "cat /sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq"),
                ShellCommand("调速器", "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor"),
                ShellCommand("可用调速器", "cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors"),
                ShellCommand("负载", "cat /proc/loadavg"),
                ShellCommand("运行时长", "uptime"),
                ShellCommand("运行秒数", "cat /proc/uptime"),
                ShellCommand("内存信息", "cat /proc/meminfo"),
                ShellCommand("内存前 5 行", "cat /proc/meminfo | head -5"),
                ShellCommand("虚拟内存统计", "vmstat"),
                ShellCommand("CPU 时间片", "cat /proc/stat | head -5"),
                ShellCommand("内存概要", "dumpsys meminfo | head -30"),
                ShellCommand("总内存", "dumpsys meminfo | grep -i total"),
                ShellCommand("CPU 占用 TOP", "top -n 1 -b | head -20"),
                ShellCommand("各核频率", "head -n1 /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq"),
                ShellCommand("中断统计", "cat /proc/interrupts | head -30")
            )
        ),
        ShellCategory(
            "存储 / 文件",
            listOf(
                ShellCommand("磁盘占用", "df -h"),
                ShellCommand("inode 占用", "df -i"),
                ShellCommand("挂载信息", "mount"),
                ShellCommand("文件系统", "cat /proc/mounts"),
                ShellCommand("分区表", "cat /proc/partitions"),
                ShellCommand("块设备", "ls -l /dev/block/by-name"),
                ShellCommand("内部存储", "df /data"),
                ShellCommand("外部存储", "df /sdcard"),
                ShellCommand("系统分区", "df /system"),
                ShellCommand("/data 大小", "du -sh /data 2>/dev/null"),
                ShellCommand("tmpfs 挂载", "mount | grep tmpfs"),
                ShellCommand("缓存目录", "ls -la /data/local/tmp"),
                ShellCommand("根目录", "ls -la /"),
                ShellCommand("/proc 目录", "ls /proc | head -40")
            )
        ),
        ShellCategory(
            "网络",
            listOf(
                ShellCommand("网络接口", "ip addr"),
                ShellCommand("路由表", "ip route"),
                ShellCommand("邻居表", "ip neigh"),
                ShellCommand("接口统计", "ip -s link"),
                ShellCommand("Ping", "ping -c 4 8.8.8.8"),
                ShellCommand("接口流量", "cat /proc/net/dev"),
                ShellCommand("路由(proc)", "cat /proc/net/route"),
                ShellCommand("ARP 表", "cat /proc/net/arp"),
                ShellCommand("TCP 连接", "cat /proc/net/tcp"),
                ShellCommand("UDP 连接", "cat /proc/net/udp"),
                ShellCommand("socket 统计", "cat /proc/net/sockstat"),
                ShellCommand("连接表", "ss -tun 2>/dev/null || netstat -tun"),
                ShellCommand("监听端口", "ss -ltn 2>/dev/null || netstat -ltn"),
                ShellCommand("WiFi 信息", "dumpsys wifi | head -60"),
                ShellCommand("连接性", "dumpsys connectivity | head -60"),
                ShellCommand("网络统计", "dumpsys netstats | head -60"),
                ShellCommand("网络策略", "dumpsys netpolicy | head -40"),
                ShellCommand("DNS 配置", "getprop | grep -i dns"),
                ShellCommand("IP 地址(过滤)", "ip addr | grep inet")
            )
        ),
        ShellCategory(
            "包管理 (pm)",
            listOf(
                ShellCommand("所有包", "pm list packages"),
                ShellCommand("系统包", "pm list packages -s"),
                ShellCommand("第三方包", "pm list packages -3"),
                ShellCommand("已禁用包", "pm list packages -d"),
                ShellCommand("已启用包", "pm list packages -e"),
                ShellCommand("包数量", "pm list packages | wc -l"),
                ShellCommand("查找包", "pm list packages | grep chrome"),
                ShellCommand("包路径", "pm path com.android.settings"),
                ShellCommand("包详情", "dumpsys package com.android.settings"),
                ShellCommand("版本号", "dumpsys package com.android.settings | grep versionName"),
                ShellCommand("权限列表", "pm list permissions"),
                ShellCommand("危险权限", "pm list permissions -d -g"),
                ShellCommand("系统特性", "pm list features"),
                ShellCommand("动态库", "pm list libraries"),
                ShellCommand("用户列表", "pm list users"),
                ShellCommand("安装来源", "dumpsys package com.android.settings | grep -i installer"),
                ShellCommand("代码路径", "dumpsys package com.android.settings | grep -i codePath"),
                ShellCommand("包签名摘要", "dumpsys package com.android.settings | grep -i -A2 signatures | head")
            )
        ),
        ShellCategory(
            "Activity / 应用",
            listOf(
                ShellCommand("当前 Activity", "dumpsys activity activities | grep -i mResumedActivity"),
                ShellCommand("顶部 Activity", "dumpsys activity top | head -40"),
                ShellCommand("Activity 栈", "dumpsys activity activities | head -80"),
                ShellCommand("最近任务", "dumpsys activity recents | head -60"),
                ShellCommand("前台窗口", "dumpsys window | grep -i mCurrentFocus"),
                ShellCommand("窗口列表", "dumpsys window windows | grep Window"),
                ShellCommand("输入法", "dumpsys input_method | head -40"),
                ShellCommand("Home 应用", "dumpsys activity | grep -i home"),
                ShellCommand("广播队列", "dumpsys activity broadcasts | head -60"),
                ShellCommand("后台服务", "dumpsys activity services | head -60"),
                ShellCommand("Provider", "dumpsys activity providers | head -40"),
                ShellCommand("通知", "dumpsys notification | head -60"),
                ShellCommand("进程 LRU", "dumpsys activity lru | head -40")
            )
        ),
        ShellCategory(
            "显示 / 屏幕",
            listOf(
                ShellCommand("分辨率", "wm size"),
                ShellCommand("密度", "wm density"),
                ShellCommand("显示信息", "dumpsys display | head -60"),
                ShellCommand("刷新率", "dumpsys display | grep -i refresh"),
                ShellCommand("SurfaceFlinger", "dumpsys SurfaceFlinger | head -60"),
                ShellCommand("窗口策略", "dumpsys window policy | head -40"),
                ShellCommand("屏幕亮度", "settings get system screen_brightness"),
                ShellCommand("灭屏超时", "settings get system screen_off_timeout"),
                ShellCommand("字体缩放", "settings get system font_scale"),
                ShellCommand("显示电源状态", "dumpsys power | grep -i 'Display Power'")
            )
        ),
        ShellCategory(
            "电池 / 电源",
            listOf(
                ShellCommand("电池状态", "dumpsys battery"),
                ShellCommand("充电信息", "dumpsys battery | grep -i charge"),
                ShellCommand("电池温度", "dumpsys battery | grep -i temperature"),
                ShellCommand("电源信息", "dumpsys power | head -60"),
                ShellCommand("唤醒锁", "dumpsys power | grep -i wake"),
                ShellCommand("省电模式", "settings get global low_power"),
                ShellCommand("电池统计", "dumpsys batterystats | head -80"),
                ShellCommand("电池历史", "dumpsys batterystats --history | head -60"),
                ShellCommand("唤醒统计", "dumpsys batterystats | grep -i 'Wake lock'"),
                ShellCommand("电源管理", "dumpsys deviceidle | head -40"),
                ShellCommand("Doze 白名单", "dumpsys deviceidle whitelist")
            )
        ),
        ShellCategory(
            "日志",
            listOf(
                ShellCommand("最近 100 行", "logcat -d -t 100"),
                ShellCommand("错误日志", "logcat -d -t 200 '*:E'"),
                ShellCommand("崩溃缓冲", "logcat -d -b crash"),
                ShellCommand("系统缓冲", "logcat -d -b system -t 100"),
                ShellCommand("主缓冲", "logcat -d -b main -t 100"),
                ShellCommand("事件缓冲", "logcat -d -b events -t 100"),
                ShellCommand("无线电缓冲", "logcat -d -b radio -t 80"),
                ShellCommand("内核日志", "dmesg | tail -60"),
                ShellCommand("过滤 ActivityManager", "logcat -d | grep ActivityManager | tail -50"),
                ShellCommand("过滤 error", "logcat -d | grep -i error | tail -50"),
                ShellCommand("过滤关键字", "logcat -d | grep -i geekbox | tail -30")
            )
        ),
        ShellCategory(
            "进程 / 性能",
            listOf(
                ShellCommand("进程列表", "ps -A"),
                ShellCommand("进程与父进程", "ps -A -o PID,PPID,NAME"),
                ShellCommand("查找进程", "ps -A | grep system"),
                ShellCommand("进程数", "ps -A | wc -l"),
                ShellCommand("TOP", "top -n 1 -b | head -20"),
                ShellCommand("线程 TOP", "top -H -n 1 -b | head -20"),
                ShellCommand("内存排名", "dumpsys meminfo | head -40"),
                ShellCommand("自身状态", "cat /proc/self/status"),
                ShellCommand("自身 fd", "ls /proc/self/fd"),
                ShellCommand("进程目录", "ls /proc | grep -E '^[0-9]+$' | head -40"),
                ShellCommand("调度统计", "cat /proc/schedstat | head -5")
            )
        ),
        ShellCategory(
            "媒体 / 音频",
            listOf(
                ShellCommand("音频引擎", "dumpsys media.audio_flinger | head -80"),
                ShellCommand("音频策略", "dumpsys media.audio_policy | head -60"),
                ShellCommand("媒体会话", "dumpsys media_session | head -60"),
                ShellCommand("相机服务", "dumpsys media.camera | head -60"),
                ShellCommand("播放器", "dumpsys media.player | head -40"),
                ShellCommand("音频状态", "dumpsys audio | head -60"),
                ShellCommand("音频设备", "dumpsys audio | grep -i device"),
                ShellCommand("媒体编解码", "dumpsys media.extractor | head -20")
            )
        ),
        ShellCategory(
            "传感器 / 输入",
            listOf(
                ShellCommand("传感器列表", "dumpsys sensorservice | head -80"),
                ShellCommand("传感器详情", "dumpsys sensorservice | grep -i -A1 Sensor"),
                ShellCommand("输入设备", "dumpsys input | grep -i Device"),
                ShellCommand("输入详情", "dumpsys input | head -60"),
                ShellCommand("触摸设备", "dumpsys input | grep -i touch"),
                ShellCommand("振动器", "dumpsys vibrator | head -40"),
                ShellCommand("输入法列表", "ime list -a")
            )
        ),
        ShellCategory(
            "设置 (settings)",
            listOf(
                ShellCommand("系统设置", "settings list system"),
                ShellCommand("全局设置", "settings list global"),
                ShellCommand("安全设置", "settings list secure"),
                ShellCommand("命名空间", "settings list"),
                ShellCommand("飞行模式", "settings get global airplane_mode_on"),
                ShellCommand("自动旋转", "settings get system accelerometer_rotation"),
                ShellCommand("动画缩放", "settings get global window_animation_scale"),
                ShellCommand("过渡动画", "settings get global transition_animation_scale"),
                ShellCommand("开发者选项", "settings get global development_settings_enabled"),
                ShellCommand("自动时区", "settings get global auto_time_zone"),
                ShellCommand("默认输入法", "settings get secure default_input_method"),
                ShellCommand("USB 调试", "settings get global adb_enabled")
            )
        ),
        ShellCategory(
            "安全 / 权限",
            listOf(
                ShellCommand("SELinux", "getenforce"),
                ShellCommand("当前用户", "id"),
                ShellCommand("AppOps", "dumpsys appops | head -60"),
                ShellCommand("权限概要", "dumpsys package | grep -i permission | head -40"),
                ShellCommand("设备管理员", "dumpsys device_policy | head -40"),
                ShellCommand("密钥库", "dumpsys keystore | head -20"),
                ShellCommand("指纹服务", "dumpsys fingerprint | head -20"),
                ShellCommand("信任代理", "settings get global http_proxy"),
                ShellCommand("锁屏状态", "dumpsys window | grep -i -A2 Keyguard"),
                ShellCommand("SELinux 上下文", "cat /proc/self/attr/current")
            )
        ),
        ShellCategory(
            "系统服务 / 硬件",
            listOf(
                ShellCommand("服务列表", "service list"),
                ShellCommand("服务数量", "service list | wc -l"),
                ShellCommand("蓝牙", "dumpsys bluetooth_manager | head -40"),
                ShellCommand("USB", "dumpsys usb | head -40"),
                ShellCommand("NFC", "dumpsys nfc | head -30"),
                ShellCommand("定位", "dumpsys location | head -40"),
                ShellCommand("温度服务", "dumpsys thermalservice | head -40"),
                ShellCommand("电话注册", "dumpsys telephony.registry | head -60"),
                ShellCommand("电话服务", "dumpsys phone | head -40"),
                ShellCommand("硬件抽象", "lshal 2>/dev/null | head -40"),
                ShellCommand("传感器服务", "dumpsys sensorservice | head -5")
            )
        ),
        ShellCategory(
            "时间 / 闹钟 / 任务",
            listOf(
                ShellCommand("当前时间", "date"),
                ShellCommand("时区", "getprop persist.sys.timezone"),
                ShellCommand("闹钟", "dumpsys alarm | head -80"),
                ShellCommand("任务调度", "dumpsys jobscheduler | head -60"),
                ShellCommand("定时器", "dumpsys deviceidle | head -30"),
                ShellCommand("NTP 状态", "getprop | grep -i ntp"),
                ShellCommand("时区设置", "settings get global auto_time_zone")
            )
        ),
        ShellCategory(
            "调试 / 杂项",
            listOf(
                ShellCommand("环境变量", "env"),
                ShellCommand("内核信息", "uname -a"),
                ShellCommand("内核版本", "uname -r"),
                ShellCommand("架构", "uname -m"),
                ShellCommand("主机名", "hostname"),
                ShellCommand("谁", "whoami"),
                ShellCommand("which su", "which su"),
                ShellCommand("limits", "ulimit -a"),
                ShellCommand("熵值", "cat /proc/sys/kernel/random/entropy_avail"),
                ShellCommand("随机 uuid", "cat /proc/sys/kernel/random/uuid"),
                ShellCommand("进程上限", "cat /proc/sys/kernel/pid_max"),
                ShellCommand("文件句柄上限", "cat /proc/sys/fs/file-max"),
                ShellCommand("加载模块", "lsmod 2>/dev/null | head"),
                ShellCommand("内核启动参数", "cat /proc/cmdline"),
                ShellCommand("命令行(当前)", "cat /proc/self/cmdline | tr '\\0' ' '")
            )
        )
    )

    val totalCount: Int = categories.sumOf { it.commands.size }

    fun search(query: String): List<ShellCommand> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return categories.flatMap { it.commands }
            .filter { it.title.lowercase().contains(q) || it.command.lowercase().contains(q) }
    }
}
