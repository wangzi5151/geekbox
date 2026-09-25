# Architecture

GeekBox 采用 **单 Activity + Compose + 轻量 MVVM/Repository** 架构，强调分层清晰与纯逻辑可测。

## 分层

```
UI (Compose Screens)
      │  只负责渲染与交互
      ▼
State (Async<T> / rememberAsync)
      │  封装 Loading / Success / Error
      ▼
Repository (feature/*/XxxRepository)
      │  唯一负责读取系统 API、/proc、/sys、进程
      ▼
Android Framework / Shell
```

- **UI 层**：`feature/*/***Screen.kt`，无业务逻辑，不直接触碰系统 API。
- **状态层**：`core/ui/Async.kt` 提供 `Async<T>` 与 `rememberAsync`，统一加载/错误/成功三态，避免散落的 `mutableStateOf` 样板。
- **数据层**：每个模块一个 `Repository`，在 `Dispatchers.IO` 上执行耗时读取。
- **工具层**：`core/util` 为纯 Kotlin/Java 逻辑，**不依赖 Android 类**，因此可在 JVM 单元测试中直接验证（`JsonFormatter`、`Codecs`、`Formatters`）。

## 导航

`navigation/GeekBoxApp.kt` 定义五个底部 Tab 与全部子路由：

- 一级路由（Tab）：`device` / `apps` / `devtools` / `network` / `system`
- 二级路由：`device/cpu`、`apps/detail/{pkg}`、`devtools/json` … 等

底部栏仅在 Tab 级路由显示，二级页面通过 `popBackStack()` 返回。

## 设计系统

- `core/ui/theme`：自定义深色优先配色（Cyan/Violet/Emerald），可选的 Android 12+ 动态取色。
- `core/ui/components`：`ScreenScaffold`、`ModuleHeader`、`SectionCard`、`InfoRow`、`StatBar`、`ToolGrid`、`CodeBlock`、`ActionRow` 等复用组件，保证跨模块视觉一致。

## 权限与降级策略

`SystemRepository` 负责探测 Root / SELinux / Shizuku。所有特权能力都遵循：

1. 先尝试非特权路径；
2. 失败时给出明确提示；
3. 绝不因权限缺失而崩溃。

## 可测试性

纯逻辑集中在 `core/util`，`app/src/test` 覆盖：

- `JsonFormatter`：格式化 / 压缩 / 校验 / 空容器 / 字符串保真
- `Codecs`：Base64、HEX、URL、摘要、Base64URL(JWT)
- `Formatters`：字节、运行时长、百分比

执行：`./gradlew testDebugUnitTest`
