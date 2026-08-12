# 🚴 鸡翅幸哲迈进OB

**iGPSPORT / 行者 / 迈金 → Outbase 运动数据同步工具**

[![Android](https://img.shields.io/badge/Platform-Android-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-MIT-yellow)](LICENSE)
[![Version](https://img.shields.io/badge/Version-v6.0.9-brightgreen)]()

一款 Android 数据迁移工具，将骑行/跑步等运动记录从三个国内平台批量同步到 Outbase 平台。

---

## ✨ 功能特性

### 支持平台

| 平台 | 登录方式 | 文件格式 | 说明 |
|------|---------|---------|------|
| **iGPSPORT** | WebView (Bearer Token) | FIT | 活动列表分页、原生FIT下载 |
| **行者** | WebView (sessionid Cookie) | GPX→FIT | GPX下载、端内格式转换（含北京时间修正） |
| **迈金/顽鹿OTM** | WebView (JWT) | FIT | 双通道下载（七牛直链+fit_content通用接口） |
| **Outbase** | WebView (sessionId) | - | CDN上传、注册接口入库 |

### 核心功能

- 🔐 **四平台 WebView 登录** — 自动提取凭证，独立存储互不影响
- 📥 **批量同步** — 支持1~1000条记录，可跳过前N条
- 🔄 **GPX→FIT 转换** — 行者专用，本地WebView内完成，含UTC→北京时间修正
- 📤 **多策略上传** — Outbase CDN h5端点 + WebView回退通道
- 📋 **详细日志** — 全过程记录，一键复制，失败原因分类
- 🎯 **单平台/全部来源选择** — 灵活控制同步范围

---

## 🛠️ 技术栈

- **语言**: Kotlin 2.2.0
- **最低SDK**: Android 8.0 (API 26)
- **目标SDK**: Android 14 (API 36)
- **构建工具**: Gradle 8.13 + AGP 8.13.0
- **网络**: OkHttp 4.12
- **协程**: Kotlinx Coroutines 1.7.3
- **UI**: Material Components

---

## 📁 项目结构

```
app/
├── build.gradle                  # 应用模块配置
└── src/main/
    ├── AndroidManifest.xml
    ├── assets/
    │   ├── bridge.html           # WebView桥页面（GPX转FIT+上传回退）
    │   └── gpx2fit.js            # Outbase官方GPX→FIT转换库
    ├── java/com/jichi/ob/
    │   ├── MainActivity.kt       # 主界面+同步调度
    │   ├── api/
    │   │   ├── IgpsportApi.kt    # iGPSPORT接口
    │   │   ├── XingzheApi.kt     # 行者接口
    │   │   ├── MageneApi.kt      # 迈金OTM接口
    │   │   └── OutbaseApi.kt     # Outbase上传
    │   ├── model/Activity.kt     # 数据模型
    │   ├── ui/LoginWebActivity.kt# 四平台WebView登录
    │   └── util/
    │       ├── PrefsManager.kt   # 凭证存储
    │       └── WebBridge.kt      # WebView桥管理
    └── res/                      # 布局、配色、字符串、图标
```

---

## 🚀 快速开始

### 环境要求

- **JDK 21**（完整JDK，含javac）
- **Android SDK**: platforms;android-36 + build-tools;36.0.0
- **Gradle 8.13**（项目自带gradle wrapper）

### 构建步骤

```bash
# 1. 克隆仓库
git clone https://github.com/Anathleticbicyclist/chibixingzhe-jinmaiOB-v6.0.9-dev-docs.git
cd chibixingzhe-jinmaiOB-v6.0.9-dev-docs

# 2. 生成签名密钥（首次需要）
keytool -genkey -v -keystore jichi-ob-release.keystore \
  -alias jichiob -keyalg RSA -keysize 2048 -validity 10000

# 3. 修改 app/build.gradle 中的签名密码
# storePassword '你的密码'
# keyPassword '你的密码'

# 4. 构建
./gradlew assembleRelease

# 5. 产物位置
# app/build/outputs/apk/release/app-release.apk
```

### 注意事项

- ⚠️ 工程需放在**本地磁盘**编译，网络挂载文件系统（如OSS/FUSE）不支持Gradle校验服务
- ⚠️ 首次构建需要下载依赖，建议配置阿里云镜像（已内置在settings.gradle）

---

## 📖 使用说明

1. **安装APK** 到 Android 设备
2. **登录各平台** — 点击对应平台的登录按钮，在WebView中完成登录
3. **设置同步数量** — 滑块选择1~1000条
4. **选择来源** — 单平台或全部来源
5. **开始同步** — 点击"开始同步到Outbase"
6. **查看日志** — 实时显示同步进度和结果

---

## 🔧 核心机制

### GPX→FIT 本地转换（行者专用）

Outbase只接受FIT格式。行者GPX经打包进assets的Outbase官方`gpx2fit.js`在WebView内本地转换，不依赖网络。

关键点：
- `gpx2fitEncoder` 是异步函数（返回Promise），桥接代码必须 `Promise.resolve().then()` 处理
- Android WebView自带DOMParser，该库浏览器分支可直接运行
- GPX为UTC时间，转换前对所有`<time>`标签+8小时，使Outbase展示为北京时间

### Outbase 上传（h5端点 + 回退）

- **主通道**: OkHttp直连 `resource/h5/upload`，浏览器风格请求头，无需鉴权
- **回退通道**: WebView内执行fetch，与官方网页请求环境完全一致

### FIT下载双通道（迈金专用）

- **通道①**: 七牛直链 — 老格式fileKey记录可用
- **通道②**: `fit_content` 接口 — 官方网页端同款，全格式通用

---

## 📊 平台接口说明

### iGPSPORT
- 活动列表: `GET /web-gateway/web-analyze/activity/queryMyActivity`
- FIT下载: `GET /web-gateway/web-analyze/activity/getDownloadUrl/{rideId}`

### 行者
- 活动列表: `GET /pgworkout/?offset=N&limit=M`
- GPX下载: `GET /pgworkout/{id}/gpx`

### 迈金（顽鹿OTM）
- 登录: `POST /api/login`（MD5密码）
- 活动列表: `POST /api/otm/ride_record/list`
- FIT下载: `GET /api/otm/ride_record/analysis/fit_content/{base64(fitUrl)}`

### Outbase
- CDN上传: `POST /zeusfit/resource/h5/upload`
- 注册入库: JSON Body + Sessionid头

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源。

---

## 🙏 鸣谢

感谢以下平台为热爱运动的用户提供的数据记录与存储服务：

- **[iGPSPORT](https://www.igpsport.com/)** — 专业骑行数据平台
- **[行者](https://www.imxingzhe.com/)** — 运动记录与社区平台
- **[迈金/顽鹿OTM](https://www.magene.com/)** — 智能骑行设备与数据平台
- **[Outbase](https://outbase.cn/)** — 运动数据聚合平台

感谢以下骑友(均为骑行爱称)为软件测试提供的帮助：素甲粉、青岛AUV阿哲、清茶、萧、洪斌大哥、鸽子王腰果、rockozhao、胶州一哥大沽河河长赵铁柱、海参

---

## 📞 联系方式

如有问题或建议，欢迎提交 Issue。

---

**鸡翅幸哲迈进OB** — 让运动数据自由流动 🚴‍♂️

