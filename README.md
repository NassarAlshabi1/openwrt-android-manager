# 🏠 OpenWrt管家（OpenWrt Manager）

> 小米路由器风格的 OpenWrt / ImmortalWrt 路由器管理 APP

一款原生安卓应用，采用小米路由器风格的 UI 设计，让你在手机上轻松管理 OpenWrt 路由器。支持插件管理、软件源配置、固件升级、系统监控等丰富功能。

---

## ✨ 功能特性

### 🎯 8大核心功能

| 功能 | 说明 | 状态 |
|------|------|------|
| 🏠 **首页自定义** | 9种卡片自由组合，拖拽排序，4种预设布局 | ✅ 完成 |
| 🔄 **Session自动续期** | 无感知自动刷新登录态，多设备Session隔离 | ✅ 完成 |
| 📶 **断线自动重连** | 网络恢复自动刷新数据，11个页面离线提示 | ✅ 完成 |
| 🌐 **WebView插件容器** | 自动注入Cookie免登录，移动端优化适配 | ✅ 完成 |
| 📱 **多设备管理** | AES加密存储，增删改查，启动自动连接 | ✅ 完成 |
| 📊 **数据可视化** | CPU/内存/流量实时曲线图，在线设备流量排序 | ✅ 完成 |
| 🌙 **深色模式** | 浅色/深色/跟随系统，全页面适配 | ✅ 完成 |
| 💾 **缓存机制** | 缓存优先策略，按路由器隔离，离线可用 | ✅ 完成 |

### 📦 插件管理
- ✅ 已安装/可用插件列表
- ✅ 8种分类筛选（系统/网络/服务/工具/多媒体/管理/其他）
- ✅ 3种排序方式（名称/大小/安装状态）
- ✅ 插件详情页（版本/大小/分类/依赖/描述）
- ✅ 安装/卸载插件（真实API）
- ✅ 安装进度显示
- ✅ 推荐插件区域
- ✅ 本地APK/IPK安装（文件选择器，自动识别包类型）

### 📚 软件源管理
- ✅ 软件源列表显示
- ✅ 启用/禁用软件源
- ✅ 添加/删除软件源
- ✅ 更新软件源（opkg update）
- ✅ 7个预设软件源（官方/清华/中科大/阿里云/kenzok8/lienol/ImmortalWrt）
- ✅ 自动配置官方源
- ✅ 一键切换软件源
- ✅ 支持opkg和apk两种包管理器
- ✅ 自动适配架构

### 🔧 系统信息检测
- ✅ 完整系统信息（型号/主机名/固件版本/内核/架构）
- ✅ 包管理器自动识别（opkg/apk）
- ✅ 架构自动检测
- ✅ 架构验证（防止安装错误架构插件）
- ✅ 软件源数量统计
- ✅ 已安装/可用插件数量统计

### ⬆️ 固件升级
- ✅ 当前固件信息显示
- ✅ 最新版本检测（支持GitHub Release）
- ✅ 自定义固件服务器
- ✅ 更新日志显示
- ✅ 固件下载（进度条+速度显示）
- ✅ MD5/SHA256校验
- ✅ sysupgrade刷写固件
- ✅ 保留配置选项
- ✅ 刷写前确认对话框
- ✅ 刷写完成自动重启
- ✅ 本地固件升级（文件选择器）

### 📡 网络管理
- ✅ WAN状态显示
- ✅ LAN配置
- ✅ DHCP配置
- ✅ 端口转发规则
- ✅ DDNS动态DNS（添加/删除/编辑）
- ✅ WiFi状态与设置
- ✅ 在线设备列表（12个设备假数据）
- ✅ 4种排序方式（IP/名称/连接时间/流量）
- ✅ 流量显示与信号强度

### ⚙️ 系统管理
- ✅ 系统信息Tab
- ✅ 系统日志
- ✅ 进程管理
- ✅ CPU/内存实时监控
- ✅ 固件升级入口
- ✅ 软件源管理入口
- ✅ 重启路由器
- ✅ 恢复出厂设置

### 🔒 安全与防火墙
- ✅ 防火墙规则管理
- ✅ 端口转发配置（添加/删除/编辑）
- ✅ 网络诊断工具
- ✅ 终端命令（模拟模式，支持uname/df/ps/ifconfig等常用命令）

### 🚀 第四阶段新功能（v2.3.0）

| 功能 | 说明 | 状态 |
|------|------|------|
| 🔔 **通知设置** | 通知总开关、4个通知渠道、通知历史记录 | ✅ 完成 |
| 📊 **流量统计** | 今日/本周/本月流量、流量概览、设备流量排行 | ✅ 完成 |
| 📱 **设备管理增强** | 设备备注、上下线历史、拉黑（防火墙规则）、限速 | ✅ 完成 |
| 🔍 **智能诊断增强** | 全面体检、诊断评分、优化建议、一键修复 | ✅ 完成 |
| 💾 **配置备份恢复** | 备份列表、创建备份、恢复备份、进度显示 | ✅ 完成 |
| 📶 **WiFi增强** | 定时开关、访客网络、信道分析、功率调节 | ✅ 完成 |
| 💻 **终端/SSH** | 终端界面、命令历史、快捷命令、主题设置 | ✅ 完成 |
| 🛒 **插件市场优化** | 插件详情、用户评价、安装/卸载/更新 | ✅ 完成 |
| 🪟 **桌面小部件** | 路由器状态小部件、网速小部件、配置页面 | ✅ 完成 |

### 🔧 后台服务

| 服务 | 说明 | 状态 |
|------|------|------|
| 📡 **路由器监控服务** | 后台监控路由器状态，离线时发送通知 | ✅ 完成 |
| ⏰ **定时任务服务** | 定时获取数据、定时备份、定时检查更新 | ✅ 完成 |
| 📊 **流量统计服务** | 后台持续统计流量数据，30秒统计一次 | ✅ 完成 |
| 🪟 **Widget更新服务** | 定期更新桌面小部件，60秒更新一次 | ✅ 完成 |

### 🎮 演示模式
- ✅ 无需路由器即可体验所有功能
- ✅ 完整的假数据（路由器状态/插件/设备/日志等）
- ✅ 模拟安装/卸载插件
- ✅ 模拟固件升级全流程
- ✅ 终端模拟支持10+常用命令
- ✅ 模拟网络诊断
- ✅ 入口1：我的页面连续点击版本号5次
- ✅ 入口2：设备管理页空状态点击体验按钮

### 🎨 UI设计
- ✅ 小米路由器风格
- ✅ 拟物卡片设计
- ✅ 蓝色主题（#1677FF）
- ✅ 圆润设计语言
- ✅ 底部导航栏
- ✅ 顶部应用栏
- ✅ 骨架屏加载
- ✅ 空状态设计
- ✅ 错误状态设计

---

## 📸 页面预览

### 主要页面
- **首页**：路由器状态、网络速度、快捷功能、系统状态、在线设备等卡片
- **插件管理**：已安装/可用/全部三个Tab，分类筛选，排序功能
- **设备管理**：路由器列表，添加/编辑/删除，切换活动路由器
- **我的页面**：设置项，主题切换，清除缓存，演示模式入口

### 功能页面
- **系统管理**：系统信息、系统日志、进程管理三个Tab
- **网络设置**：WAN/LAN/DHCP配置
- **WiFi设置**：WiFi状态与配置
- **防火墙**：防火墙规则与端口转发
- **DDNS**：动态DNS配置
- **在线设备**：已连接设备列表，流量统计
- **高级功能**：系统信息、存储状态、进程管理、网络诊断、终端命令
- **智能诊断**：一键智能诊断，问题检测与修复建议
- **软件源管理**：软件源列表，增删改查，预设源切换
- **固件升级**：版本检测，下载，刷写全流程
- **WebView插件容器**：直接在APP内打开LuCI插件页面

---

## 🚀 使用说明

### 添加路由器
1. 打开APP，进入「设备管理」页面
2. 点击右下角「+」按钮
3. 填写路由器信息：
   - 设备名称（自定义）
   - 路由器地址（IP或域名）
   - 用户名（默认root）
   - 密码
   - 端口（默认80）
   - 协议（HTTP/HTTPS自动检测）
4. 点击「连接并保存」

### 使用演示模式
**方式一**：我的页面开启
1. 进入「我的」页面
2. 连续点击版本号5次
3. 弹出提示后即可进入演示模式

**方式二**：设备管理页开启
1. 进入「设备管理」页面
2. 空状态下点击「🎮 体验演示模式（无需路由器）」按钮
3. 自动添加演示路由器并进入

### 切换主题
1. 进入「我的」页面
2. 点击「主题设置」
3. 选择：浅色模式 / 深色模式 / 跟随系统

### 管理插件
1. 进入「插件」页面
2. 三个Tab切换：已安装 / 可用 / 全部
3. 顶部分类栏筛选插件分类
4. 右上角排序按钮切换排序方式
5. 点击插件查看详情
6. 点击「安装」或「卸载」按钮操作

### 升级固件
1. 进入「系统管理」页面
2. 切换到「系统信息」Tab
3. 点击底部「固件升级」卡片
4. 点击「检测更新」查看最新版本
5. 点击「下载固件」开始下载
6. 下载完成后点击「开始刷写」
7. 确认保留配置选项后开始升级

### 管理软件源
1. 进入「插件」页面
2. 点击「软件源管理」快捷入口
3. 查看软件源列表
4. 开关切换启用/禁用状态
5. 点击「添加」按钮添加自定义源
6. 点击「预设源」快速切换软件源

---

## 🛠️ 开发说明

### 技术栈
- **语言**：Kotlin
- **架构**：MVVM + Repository
- **UI**：Jetpack Compose + Material Design 3
- **网络**：Retrofit + OkHttp
- **异步**：Kotlin Coroutines + Flow
- **数据库**：Room
- **存储**：SharedPreferences + DataStore
- **最低 SDK**：API 30 (Android 11)
- **目标 SDK**：API 34 (Android 14)
- **编译 SDK**：API 34 (Android 14)
- **Gradle**：8.5
- **Kotlin**：1.9.24

### 架构说明
```
┌─────────────────────────────────────────┐
│              UI Layer                   │
│  (Compose Screens + ViewModels)         │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│           Repository Layer              │
│  (LuciRepository / CacheRepository      │
│   RouterRepository / SettingsRepository)│
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│              Data Layer                 │
│  (Retrofit API + Room Database          │
│   SharedPreferences)                    │
└─────────────────────────────────────────┘
```

### 目录结构
```
app/src/main/java/com/luanmuc/openwrtmanager/
├── data/
│   ├── api/              # API接口
│   │   ├── LuciApiService.kt
│   │   └── RetrofitClient.kt
│   ├── db/               # 数据库
│   │   ├── entity/       # 实体类
│   │   ├── dao/          # DAO接口
│   │   ├── AppDatabase.kt
│   │   └── DatabaseHelper.kt
│   ├── model/            # 数据模型
│   │   └── LuciRpc.kt
│   └── repository/       # 数据仓库
│       ├── LuciRepository.kt
│       ├── CacheRepository.kt
│       ├── RouterRepository.kt
│       └── SettingsRepository.kt
├── ui/
│   ├── base/             # 基类
│   │   └── BaseViewModel.kt
│   ├── components/       # 通用组件
│   │   ├── MiComponents.kt
│   │   ├── LineChart.kt
│   │   ├── OfflineBanner.kt
│   │   └── Skeleton.kt
│   ├── theme/            # 主题
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   ├── Type.kt
│   │   └── ThemeManager.kt
│   ├── home/             # 首页
│   ├── plugins/          # 插件页
│   ├── system/           # 系统管理
│   ├── firmware/         # 固件升级
│   ├── network/          # 网络设置
│   ├── wifi/             # WiFi设置
│   ├── firewall/         # 防火墙
│   ├── ddns/             # DDNS
│   ├── devices/          # 设备管理
│   ├── advanced/         # 高级功能
│   ├── diagnostic/       # 智能诊断
│   ├── profile/          # 我的页面
│   ├── addrouter/        # 添加路由器
│   └── webview/          # WebView插件
├── util/                 # 工具类
│   ├── DebugMode.kt
│   ├── NetworkMonitor.kt
│   ├── EncryptionUtil.kt
│   ├── RetryUtil.kt
│   ├── HttpsDetector.kt
│   └── NetworkDiagnostic.kt
├── MainActivity.kt       # 主Activity
└── OpenWrtApp.kt         # Application
```

### 编译方法
```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 禁用Kotlin增量编译（解决缓存权限问题）
./gradlew assembleDebug --no-daemon
```

### 云端编译
项目使用 GitHub Actions 进行云端编译：
- 每次推送到 main 分支自动触发
- 编译产物自动上传为 Artifact
- 工作流文件：`.github/workflows/build.yml`

---

## 📝 更新日志

### v2.3.0-dev (2024-08-09)

**新增功能**
- ✨ 固件升级功能
  - 当前固件信息显示
  - GitHub Release 版本检测
  - 自定义固件服务器支持
  - 固件下载（进度条+速度显示）
  - MD5/SHA256 校验
  - sysupgrade 刷写固件
  - 保留配置选项
  - 刷写前确认对话框
  - 完整演示模式支持

- ✨ 系统信息检测
  - 完整系统信息展示
  - 包管理器自动识别（opkg/apk）
  - 架构自动检测
  - 架构验证功能
  - 软件源数量统计

- ✨ 软件源管理
  - 7个预设软件源（官方/清华/中科大/阿里云/kenzok8/lienol/ImmortalWrt）
  - 自动配置官方源
  - 一键切换软件源
  - 支持opkg和apk两种包管理器
  - 自动适配架构

- ✨ 插件功能完善
  - 8种分类筛选
  - 3种排序方式
  - 插件详情页
  - 推荐插件区域
  - 本地IPK安装UI
  - 架构验证警告

**优化改进**
- 🎨 小米路由器风格UI全面优化
- 📊 数据可视化图表完善
- 🌙 深色模式全页面适配
- 🔄 Session自动续期优化
- 📶 断线自动重连完善
- 💾 缓存机制全面接入

### v2.2.0-dev (2024-08-07)

**新增功能**
- ✨ 首页自定义功能（9种卡片，拖拽排序，4种预设布局）
- ✨ 多设备管理完整功能（AES加密，增删改查）
- ✨ WebView插件容器（自动登录，移动端优化）
- ✨ 深色模式（浅色/深色/跟随系统）
- ✨ 演示模式（完整假数据，所有功能可模拟）

### v2.1.0-dev (2024-08-05)

**新增功能**
- ✨ 基础框架搭建
- ✨ 首页基本功能
- ✨ 插件管理基础功能
- ✨ 设备管理基础功能
- ✨ 我的页面基础设置

---

## 📄 许可证

MIT License

---

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

---

**注意**：本项目仍处于开发阶段，部分功能可能不完善。

---

## 🌍 Localization / التعريب (v2.4.0)

The app UI has been fully localized into **English (default)** and **Arabic (RTL)** — Chinese resources were removed.

- **Language switcher**: Profile → Settings → Language (Follow system / English / العربية), applied instantly.
- **RTL**: full right-to-left layout support when Arabic is selected.
- **~1,160 strings** translated across all screens, dialogs, widgets, notifications and enums.
- See [`tools/i18n/README.md`](tools/i18n/README.md) for the localization architecture and the key→translation mapping.

## 🧩 OpenWrt version support (v2.4.0)

Added an official firmware support matrix for **OpenWrt 23.05.0 – 23.05.6** (the short notation `23.5.0 – 23.5.6` is accepted and normalized automatically, e.g. `23.5.3` → `23.05.3`, and build suffixes like `r23497-6637af95aa` are stripped).

- Support status badge in **System → firmware info** and on the **Firmware upgrade** screen.
- **Smart diagnostics** now checks the connected router's firmware version against the supported range.
- **Software repository auto-configuration** normalizes the firmware version before building feed URLs.
- Unit tests cover the version matrix (`app/src/test/.../OpenWrtVersionsTest.kt`, `VersionUtilsTest.kt`).
