# GeekBox · 极客工具箱

> 面向 Android 极客、开发者与高级用户的本地化专业工作台。
> **本地运行 · 无广告 · 无联网上传 · 响应迅速 · Material 3 设计**

GeekBox 不是几十个小工具的简单堆砌，而是围绕 **设备 / 应用 / 媒体 / 开发 / 网络 / 系统** 六大模块，并内置 **AI 专区** 的统一工作台。所有数据均在本机处理，不采集、不上传。

> 作者：**wangzi5151**（中国内蒙古）· 邮箱：wangzi5151@sina.com · 开源地址：https://github.com/wangzi5151/geekbox · 包名：`com.wangzi5151.geekbox`

<p align="center">
  <em>原生 Kotlin + Jetpack Compose + Material 3 · 单 Activity · MVVM</em>
</p>

---

## ✨ 功能总览

### 0. AI 专区 · AI
应用内 WebView 聚合国内主流大模型网页：**DeepSeek、豆包、通义千问、文心一言、Kimi、腾讯元宝、智谱清言、讯飞星火、海螺 AI、阶跃 AI、天工 AI、百小应**，并支持自定义网址。登录/验证码由各站点自行处理，GeekBox **不读取、不保存账号密码**。**打开方式可选**：默认「系统浏览器内核（Chrome Custom Tabs，应用内标签页，兼容性最好）」，也可切换为「应用内 WebView」；支持刷新、输入兼容模式、桌面版 UA、清除缓存与 Cookie。

### 1. 设备 · Device
| 工具 | 说明 |
| --- | --- |
| 设备概览 | 厂商 / 型号 / 代号 / Android 版本 / 安全补丁 / 内核 / Build / ABI / 运行时长 / 系统指纹 |
| CPU | 逻辑核心数、每核实时/最小/最大频率、调速器、负载、**自动刷新 + 实时负载曲线** |
| GPU | GPU 型号与频率（Best-effort，部分设备需 Root） |
| 内存 | 物理内存用量、可用量、低内存阈值、Swap、`/proc/meminfo` 明细、**实时占用曲线** |
| 电池 | 电量、充电状态、健康、温度、电压、电池技术、设计容量 |
| 存储 | 内部 / 外部存储总量、可用量与占用条 |
| 屏幕 | 分辨率、DPI、当前与支持的刷新率、HDR 支持 |
| 摄像头 | 镜头方向、硬件级别、最大分辨率、焦距、闪光灯 |
| 手电筒 | 闪光灯开关 |
| 音量 | 媒体 / 铃声 / 闹钟 / 通知 / 系统 / 通话 音量调节 |
| 屏幕测试 | 全屏纯色，检查坏点与亮度均匀性 |
| 设备报告 | 汇总全部设备信息并一键复制 |
| 传感器 | 全部传感器列表（厂商/量程/精度/功耗/唤醒），点击进入**实时数值**页 |
| 温度 | `/sys/class/thermal` 热区温度（按温度着色） |

### 2. 应用 · Apps
- 已安装应用列表：搜索、用户/系统筛选、图标、版本、体积、UID。
- 应用详情：包名、版本/SDK、UID、安装/更新时间、APK 大小、ABI、APK/数据/Native 路径。
- **权限清单**：逐条显示已授予 / 未授予。
- **组件清单**：Activity / Service / Receiver / Provider，高亮 `EXPORTED`（导出组件安全审计）。
- **签名**：SHA-256 / SHA-1 证书指纹。
- 操作：打开、应用信息、**导出 APK**、卸载。
- **APK 文件分析**：选择本地 APK，无需安装即可解析清单、权限、组件与签名。

### 3. 媒体 · Media
**音频解码**：选择常见音频（MP3 / AAC / FLAC / OGG …），查看标题 / 艺术家 / 时长 / 码率 / 采样率等元数据，并解码导出为无损 **WAV(PCM)**。**视频信息**：分辨率 / 时长 / 码率 / 旋转 / 帧率 / 编码。

### 4. 开发 · DevTools
JSON 格式化 / 压缩 / 校验 · XML 格式化 · Base64 · Hash(MD5/SHA-1/256/512，支持文件) · HEX · 正则匹配与分组 · URL 编解码 · 时间戳互转 · UUID 批量生成 · **二维码生成** · **条形码生成(Code128/EAN/Code39)** · **颜色转换(HEX/RGB/HSL)** · **文本工具(统计/批量转换)** · **文本差异(逐行)** · **进制转换** · **Cron 解析** · **随机密码/PIN 生成** · **子网计算器(IP/CIDR)** · **JWT 解析**。

### 5. 网络 · Network
网络信息（接口 / IPv4 / IPv6 / WiFi / DNS） · **实时速率监控（上/下行 + 曲线）** · **网络连接表（TCP/UDP，Shizuku 完整）** · **监听端口** · **应用流量统计** · **WiFi 扫描** · **DNS 查询** · **DNS 基准测试（多 DNS 解析测速）** · **反向解析 (PTR)** · **Ping** · **Traceroute** · **重定向追踪** · **NTP 校时** · **Wake-on-LAN** · **HTTP 调试** · **TLS 证书检查（颁发者/到期/SAN/指纹）** · **WHOIS 查询** · **下载器（含测速）** · **TCP 端口扫描** · **局域网扫描**。

### 6. 系统 · System
**Shizuku 集成**（免 Root 获取 shell 级能力） · **Shell 命令库**（Shizuku / Root / 本地，内置 **200+ 只读诊断命令，分 17 个分类，可搜索**，输出区置顶实时显示） · **Logcat**（三种来源） · **进程管理器**（搜索 / 结束进程） · **应用管理**（强制停止 / 冻结 / 启用 / 清除数据 / 卸载） · **应用存储占用** · **dumpsys 浏览器** · **挂载/分区查看** · **Intent / URI 调试** · Root / SELinux / Shizuku 能力探测 · 系统属性（getprop） · 常用系统设置快捷入口 · **关于**（主题 / 动态取色 / 隐私说明）。

---

## 🔐 权限与能力说明（重要）

GeekBox 坚持 **最小权限 + 能力自动降级** 原则。

**无需任何运行时权限** 即可使用：设备信息、内存、电池、存储、传感器、应用列表与 APK 分析、全部开发工具、全部网络诊断。

以下能力受 Android 沙箱限制，遵循「有则增强，无则降级」：

| 能力 | 普通 | Shizuku（免 Root） | Root |
| --- | --- | --- | --- |
| 系统 Logcat（读取其他应用日志） | 受限 | ✅ 完整（shell 身份） | ✅ 完整 |
| 网络连接表 / 进程列表 | 受限 | ✅ 完整 | ✅ 完整 |
| 结束进程 / 应用冻结停用 / 清除数据 | 不可用 | ✅ shell 身份 | ✅ root 身份 |
| Shell 命令 | 受限 | ✅ shell 身份 | ✅ root 身份 |
| GPU 实时占用、完整全核频率、部分温度传感器 | 视设备而定 | 视设备而定 | ✅ 完整 |

> **Shizuku** 是社区标准的免 Root 提权方案（通过 ADB/无线调试启动）。GeekBox 集成了 Shizuku：首次使用需在「系统 → Shizuku」中授权，之后 Logcat / 进程 / Shell 均可以 shell 身份运行。GeekBox **不会**静默提权，所有特权操作都由用户主动发起。

### 已声明权限
| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 网络诊断（Ping/DNS/HTTP/端口/局域网） |
| `ACCESS_NETWORK_STATE` | 判断当前网络类型 |
| `ACCESS_WIFI_STATE` | 读取 WiFi 连接信息 |
| `ACCESS_FINE_LOCATION` | WiFi 扫描（Android 12 及以下的扫描要求） |
| `NEARBY_WIFI_DEVICES` | WiFi 扫描（Android 13+，`neverForLocation`） |
| `QUERY_ALL_PACKAGES` | 列出本机已安装应用（用于应用/APK 分析） |
| `PACKAGE_USAGE_STATS` | 各应用流量与存储占用统计（需在系统设置中手动授予） |

> 以上权限仅在本机使用，任何数据都不会上传。

---

## 🧱 技术栈

- **语言**：Kotlin 1.9.24
- **UI**：Jetpack Compose（BOM 2024.04.01）+ Material 3
- **架构**：单 Activity + Navigation-Compose + Repository + `Async<T>` 状态封装
- **异步**：Kotlin Coroutines（含自动轮询刷新）
- **免 Root 提权**：Shizuku API 13.1.5
- **二维码**：ZXing core
- **构建**：Gradle 8.2.1 + AGP 8.2.1 + JDK 17
- **最低支持**：Android 8.0（API 26） · 目标 API 34

---

## 🗂 项目结构

```
app/src/main/java/com/tinyai/geekbox/
├── MainActivity.kt
├── navigation/            # 底部导航 + 路由
├── core/
│   ├── model/             # ToolItem / Module
│   ├── ui/                # 主题、通用组件、Async 封装
│   └── util/              # Formatters / Codecs / JsonFormatter / Shell / SysFs
└── feature/
    ├── device/            # 设备模块（Repository + Screens）
    ├── apps/              # 应用与 APK 分析
    ├── devtools/          # 开发工具箱
    ├── network/           # 网络诊断
    └── system/            # 系统与调试
```

分层约定：`Repository` 只负责系统数据采集；`Screen` 只负责渲染；纯逻辑集中在 `core/util`，**可单元测试**（`app/src/test`）。

---

## 🔨 构建

### 使用 Android Studio
1. 打开项目根目录 `geekbox/`。
2. 等待 Gradle 同步，运行 `app` 配置即可。

### 命令行
```bash
# Debug
./gradlew assembleDebug
# Release（需 keystore.properties）
./gradlew assembleRelease
```

产物：
- Debug：`app/build/outputs/apk/debug/app-debug.apk`
- Release：`app/build/outputs/apk/release/app-release.apk`

### 在 Termux / aarch64 上构建的说明
本项目已在仅含命令行工具链的 aarch64 Termux 环境中成功构建。关键点：
- 使用系统 `aapt2` 覆盖 x86_64 版 SDK 工具：
  `android.aapt2FromMavenOverride=/path/to/aapt2`（见 `gradle.properties`）。
- JDK 17：`export JAVA_HOME=$PREFIX/lib/jvm/java-17-openjdk`。

### 签名发布
复制 `keystore.properties.example` 为 `keystore.properties` 并填入你自己的密钥信息（该文件与 `keystore/` 已被 `.gitignore` 忽略，请勿提交密钥）。

```properties
storeFile=keystore/your-release.jks
storePassword=******
keyAlias=******
keyPassword=******
```

生成密钥：
```bash
keytool -genkeypair -v -keystore keystore/release.jks -alias geekbox \
  -keyalg RSA -keysize 2048 -validity 10000
```

---

## 🧪 测试

```bash
./gradlew testDebugUnitTest
```
覆盖 JSON 解析/格式化/校验、Base64/HEX/URL 编解码、摘要算法、字节/时间格式化等纯逻辑。

---

## 🗺 Roadmap
- [ ] Shizuku 深度集成（免 Root 的系统级能力）
- [ ] 进程/流量/网络连接实时监控
- [ ] APK 反编译级分析（DEX / 资源清单）
- [ ] 主题自定义与深色/浅色一键切换
- [ ] 多语言（English / 简体中文）

---

## 🤝 贡献
欢迎 Issue 与 PR。提交前请确保 `./gradlew testDebugUnitTest assembleDebug` 通过，并保持现有代码风格（无冗余注释、职责单一、纯逻辑可测）。

---

## 📄 许可证
本项目采用 [MIT License](LICENSE)。

> 使用本项目进行安全测试 / 调试时，请遵守当地法律法规，仅对你有权操作设备与网络使用。
