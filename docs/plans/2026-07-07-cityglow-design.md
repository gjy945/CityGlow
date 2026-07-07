# CityGlow 城市光影 — 设计文档

- **项目**: CityGlow 城市光影 · 夜间光污染监测与星空寻回地图
- **性质**: JavaWeb 课程设计 / 创新实验项目
- **日期**: 2026-07-07
- **状态**: 设计已确认,进入实施计划阶段

---

## 1. 项目立意

关注光污染与环境暗夜保护,利用 GIS 与多源数据聚合技术,构建"寻找最后星空"的沉浸式 Web 应用。用户可查询所在地暗夜等级、获取天文气象预报、记录观星足迹,唤起公众对暗夜保护的关注。

---

## 2. 技术栈

### 后端(新增,本次确认)
- Java 21 (LTS) — 虚拟线程 / Record / 结构化并发 / Pattern Matching
- Spring Boot 3.2.x
- Spring Data JPA + MySQL 8.0
- RestClient (Spring 6.1) 调用第三方 API
- Maven 3.9+
- 图片处理: javax.imageio.ImageIO + Graphics2D (无需原生依赖)

### 前端
- Vue 3 `<script setup>` + TypeScript
- Vite 5
- Vue Router 4
- Pinia
- TailwindCSS (深色模式)
- Leaflet 1.9 + CartoDB Dark Matter 暗色瓦片
- Chart.js (月相 / 指数可视化)
- axios

### 外部 API
- OpenWeatherMap: 当前天气 + 预报 + 天文数据(日出日落月相)
- NASA APOD: 每日天文一图
- 光污染瓦片: Light Pollution Map 透明覆盖层 (Bortle 1-9)

### 部署
- 后端: localhost:8080,JDK 21
- 前端: localhost:5173,Vite dev proxy 转发 `/api` -> `:8080`
- 图片存储: 后端 `uploads/` 本地文件系统

---

## 3. 架构总览

```
CityGlow/
├── backend/                          # Spring Boot 3.2 + JDK 21
│   ├── pom.xml
│   ├── src/main/java/com/cityglow/
│   │   ├── CityGlowApplication.java
│   │   ├── config/
│   │   │   ├── VirtualThreadConfig.java       # spring.threads.virtual.enabled
│   │   │   └── RestClientConfig.java
│   │   ├── controller/
│   │   │   ├── AstroForecastController.java   # GET /api/v1/astro/forecast
│   │   │   ├── ObservationLogController.java  # POST /api/v1/logs/upload, GET /api/v1/logs
│   │   │   ├── AstroEventController.java      # GET /api/v1/events
│   │   │   └── ApodController.java            # GET /api/v1/apod
│   │   ├── service/
│   │   │   ├── ForecastService.java           # 虚拟线程聚合气象
│   │   │   ├── PostcardService.java           # StructuredTaskScope 并行
│   │   │   ├── OpenWeatherClient.java
│   │   │   └── NasaApodClient.java
│   │   ├── domain/                            # Record DTOs
│   │   ├── entity/                            # JPA @Entity
│   │   ├── repository/
│   │   └── util/StargazingIndex.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/schema.sql
│   └── uploads/
│
├── frontend/                          # Vue3 + Vite + TS
│   ├── src/
│   │   ├── main.ts
│   │   ├── App.vue
│   │   ├── router/index.ts
│   │   ├── stores/
│   │   ├── api/
│   │   ├── components/
│   │   │   ├── DarkSkyLeaflet.vue
│   │   │   ├── MoonPhaseCanvas.vue
│   │   │   ├── EventTimeline.vue
│   │   │   └── PostcardCanvas.vue
│   │   ├── views/
│   │   │   ├── DarkSkyMap.vue
│   │   │   ├── ForecastPanel.vue
│   │   │   ├── AstroTimeline.vue
│   │   │   └── StarryPostcard.vue
│   │   └── assets/
│   ├── .env
│   ├── vite.config.ts
│   ├── tailwind.config.js
│   └── tsconfig.json
│
└── docs/plans/
```

---

## 4. 功能模块设计

### 模块 1: 暗夜地图(DarkSkyMap.vue,首页)

- Leaflet 暗色瓦片(CartoDB Dark Matter,无 key)
- 光污染覆盖层: Bortle 1-9 配色 `#000033`→`#FFFF00`
- 标记点: 天文台 / 暗夜公园 / 最佳观星点(后端 GET /api/v1/events)
- 点击地图任意位置 → 右侧滑出 ForecastPanel 子组件
- `navigator.geolocation` 定位

### 模块 2: 观星条件预测(ForecastPanel.vue)

后端 `GET /api/v1/astro/forecast?lat=&lng=`:
- 虚拟线程并行调 OpenWeatherMap 三端点
  - `/2.5/weather` (云量)
  - `/2.5/forecast` (未来预报)
  - `/astronomy` (日出日落月相)
- 查 Bortle 等级(按经纬度查表,简化版)
- `StargazingIndex.calculate()` 返回 0-100
- 响应结构(沿用文档):
```json
{
  "code": 200,
  "data": {
    "score": 85,
    "cloudCover": 10,
    "moonPhase": "New Moon",
    "bortleLevel": 3,
    "message": "今夜适合观星!"
  }
}
```

前端: Chart.js radialGauge 指数仪表盘 + Canvas 月相 + 日落/天文昏影/日出时间轴

### 模块 3: 天文事件时间轴(AstroTimeline.vue)

- 数据: 后端 `astro_events` 表预填 2026 年主要天文事件
- 横向滚动时间轴,每事件卡片: 类型图标 / 日期 / 倒计时
- 详情弹窗: 科普介绍 + 最佳观测方位
- `setInterval` 每秒刷新距下一事件倒计时

### 模块 4: 星空打卡明信片(StarryPostcard.vue)

- 用户上传图 + 经纬度(地图选点)+ 描述
- 后端 `POST /api/v1/logs/upload` (multipart/form-data):
  - `StructuredTaskScope.ShutdownOnFailure()` 并行:
    1. 图片压缩(ImageIO,长边 1920px)
    2. 水印绘制(Graphics2D: 经纬度/时间/月相/Bortle)
    3. 元数据写入(EXIF UserComment)
  - 合并 → 写 observation_logs 表 + 存盘 `uploads/{logId}.jpg`
  - 返回 `{ logId, cardUrl }`
- 分享: `GET /api/v1/logs/{id}` → 前端 `/postcard/{id}` 唯一 URL
- 画廊: `GET /api/v1/logs` 列表

---

## 5. 观星指数算法

```java
// StargazingIndex.java
public static int calculate(double cloudCover, double moonIlluminatedFraction, int bortleLevel) {
    double score = 100;
    score -= Math.max(0, cloudCover - 20) * 1.5;   // 云量: 20% 内不扣
    score -= moonIlluminatedFraction * 30;          // 月相: 满月 -30
    score -= (bortleLevel - 1) * 8;                 // 光污染: Bortle 9 扣 64
    return (int) Math.round(Math.max(0, Math.min(100, score)));
}
// 消息: 80-100 "今夜极佳!" / 60-79 "适合观星" / 40-59 "一般" / <40 "不建议"
```

---

## 6. JDK 21 特性落点

| 特性 | 位置 |
|---|---|
| 虚拟线程 | `spring.threads.virtual.enabled=true` + ForecastService 用 `Executors.newVirtualThreadPerTaskExecutor()` 并行调气象 API |
| Record | 所有 DTO: AstroData / ForecastResult / PostcardResult / OpenWeatherResponse |
| 结构化并发 | PostcardService 用 `StructuredTaskScope.ShutdownOnFailure()` 并行压缩/水印/元数据 |
| Pattern Matching | switch 处理 AstroEvent.eventType 分发渲染 |

---

## 7. 数据库(沿用文档 + 补索引)

3 张表见文档第 4.2 节,补充:
- `observation_logs` 索引 `(latitude, longitude)` 支持地图范围查询
- `astro_events` 索引 `(event_time)` 支持时间轴排序
- `astro_events` 预填 2026 年种子数据

---

## 8. API Key 安全

- 后端 `application.yml` 从环境变量注入:
  - `OPENWEATHER_API_KEY`
  - `NASA_APOD_API_KEY`
- 严禁前端直连第三方 API,所有外部请求经后端代理
- `.gitignore` 排除 `.env` 和 `application-local.yml`

---

## 9. 非功能需求

- 并发: 虚拟线程支持 500+ 并发查询
- 响应: 数据聚合接口 < 800ms,地图加载 < 1s
- 文件校验: 上传 MIME Type 白名单(image/jpeg, image/png)
- UI: 全站深色模式,模拟夜空视觉

---

## 10. 前端美学方向(frontend-design 决策)

- **概念方向**: 深空编辑 · 暗夜守护者的天文志
- **配色**: 主色深空黑 `#0a0e1a`,次色靛蓝 `#1a1f3a`,点缀星辉银 `#e8eaf6` 与暗金 `#c5a572`(明信片质感)
- **字体**: 标题 Cormorant Garamond(古典天文志感),正文 Manrope(现代清晰),数字 JetBrains Mono(仪表读数)
- **质感**: 星点 SVG 噪点背景、毛玻璃面板、月光投射光晕、星图等高线装饰
- **动效**: 首页星空缓慢漂移、月相 Canvas 平滑过渡、时间轴卡片悬浮微动
