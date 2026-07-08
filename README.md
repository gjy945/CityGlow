# 城市光影 CityGlow

> 基于光污染监测的星空寻回地图 — JavaWeb 课程设计

当城市的霓虹一寸寸吞噬夜空,我们用一张暗夜地图,寻回最后一片可以仰望的星野。

CityGlow 聚焦光污染这一被忽视的环境议题,聚合气象数据、Bortle 暗夜等级与天文事件,
为观星者提供一份「今夜是否值得抬头」的编辑式指南。后端以 JDK 21 虚拟线程与结构化并发
并行调度多个第三方 API,前端以深空编辑美学呈现一张可交互的星空寻回地图。

## 技术栈

### 后端
- Java 21(LTS)+ Spring Boot 3.2.5
- Spring Data JPA + MySQL 8.0(测试用 H2)
- Spring Security + JWT 0.12.x(用户认证)
- Caffeine 本地缓存(NASA API / 预测结果缓存)
- JDK 21 特性:虚拟线程 / Record / 结构化并发(StructuredTaskScope)
- RestClient 调第三方 API
- Lombok 减负样板代码

### 前端
- Vue 3 + Vite + TypeScript
- Vue Router 4 + Pinia
- TailwindCSS v3(深色模式,自定义深空配色)
- Leaflet 1.9(CartoDB Dark Matter 暗色瓦片)
- vue-i18n 9(中 / 英 / 日 三语切换)
- 自定义 Canvas(月相绘制 + 星座连线 + 立体投影)

### 外部 API
- Open-Meteo(免费开源气象数据,无需 API Key)
- NASA APOD(每日天文一图)
- NASA DONKI(已移除,极光预报)
- NASA NeoWs(已移除,近地小行星)

## 功能模块

1. **暗夜地图** — Leaflet 暗色地图 + Bortle 光污染覆盖 + 点击查询观星指数,
   右上角嵌入 NASA APOD 缩略图,点击查看大图与说明。
2. **观星条件预测** — 虚拟线程并行聚合气象数据,生成 0-100 指数(SVG 环形),
   附 Canvas 月相、日落 / 天文昏影终 / 日出时间轴。
3. **天文事件时间轴** — 2026 全年 13 条事件(流星雨 / 日月食 / 行星动态),
   横向滚动 + 实时倒计时 + 详情弹窗,类型色编码。
4. **星空明信片** — 结构化并发生成明信片(并行压缩 + 水印 + 元数据),
   暗金边框 + 邮票邮戳装饰,画廊网格展示,详情页可分享。
5. **星图连接器** — Canvas 立体投影 12 个主要星座(猎户 / 大熊 / 天蝎 / 仙后等),
   鼠标悬停高亮 + 点击弹出希腊神话 / 中国古代星宿对照卡,
   时间滑块拖动星空实时旋转,17 世纪铜版画美学。
6. **用户系统** — 注册 / 登录 + JWT 鉴权 + 收藏观测点(经纬度坐标)。
7. **多语言切换** — 中文 / 英文 / 日文 三语,前端 vue-i18n 动态切换。

## 快速开始

### 环境要求
- JDK 21(需启用 `--enable-preview` 以支持结构化并发)
- MySQL 8.0
- Node.js 18+
- Maven 3.9+

### 后端启动
```bash
cd backend
# 在 backend/ 下创建 .env 填入 API Key 和 MySQL 密码(参考下方配置)
# 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS cityglow DEFAULT CHARSET utf8mb4"
# 启动(虚拟线程由 spring.threads.virtual.enabled 自动开启)
mvn spring-boot:run
```
后端运行在 http://localhost:8080

### 前端启动
```bash
cd frontend
npm install
npm run dev
```
前端运行在 http://localhost:5173,Vite proxy 自动转发 `/api` 与 `/uploads` 到后端。

### API Key 与数据库配置
在 `backend/.env` 中配置(`application.yml` 已通过 `${ENV_VAR}` 读取):
```
NASA_APOD_API_KEY=你的NASA APOD Key
MYSQL_PASSWORD=你的MySQL密码
# 可选,默认 localhost:3306
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
```
> 气象数据使用 Open-Meteo(免费开源,无需 API Key)。

## 演示流程

1. 打开首页(暗夜地图),点击地图任意位置
2. 右侧滑出观星条件预测面板,查看指数环与月相
3. 顶部点击「天文事件」,查看时间轴与下一事件倒计时
4. 点击事件卡片查看观测建议详情
5. 点击「星图」,查看今晚当地可见星空与星座连线
6. 点击星座查看希腊 / 中国神话故事
7. 点击「星空明信片」,上传一张照片生成明信片
8. 在画廊查看所有明信片,点击进入详情页

## JDK 21 特性应用

| 特性 | 应用位置 |
|---|---|
| 虚拟线程 | `ForecastService` 用 `Executors.newVirtualThreadPerTaskExecutor()` 并行调气象 API;`spring.threads.virtual.enabled=true` |
| Record | 所有 DTO:`AstroData` / `ForecastResult` / `PostcardResult` / `SkyViewResult` / `StarPoint` 等 |
| 结构化并发 | `PostcardService` 用 `StructuredTaskScope.ShutdownOnFailure` 并行压缩 + 水印,任一失败即关闭全部 |
| Pattern Matching | 事件类型 `METEOR` / `ECLIPSE` / `PLANET` 分发(前端 typeMeta + 后端枚举) |

> 注:结构化并发为 JDK 21 PREVIEW 特性,`pom.xml` 已配置 `--enable-preview`,
> `maven-surefire-plugin` 同样启用以保证测试运行。

## 项目结构

```
CityGlow/
├── backend/                      Spring Boot 后端
│   ├── src/main/java/com/cityglow/
│   │   ├── config/               SecurityConfig / JwtAuthenticationFilter / CacheConfig / AstroEventSeeder / WebMvcConfig
│   │   ├── controller/           7 个 REST 控制器(astro / events / apod / logs / auth / favorite / sky)
│   │   ├── domain/               Record DTO(含 ApiResponse 统一响应)
│   │   ├── entity/               JPA 实体(User / AstroEvent / ObservationLog / FavoriteLocation)
│   │   ├── repository/           Spring Data JPA 仓库
│   │   ├── service/              ForecastService / PostcardService / AuthService / ConstellationDataService / StarProjectionService 等
│   │   └── util/                 Bortle / 月相 / 指数算法 / JwtUtil / i18n Messages
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── data/                 星座数据(亮星表 / 连线 / 神话)
│   ├── src/test/                 单元 / 集成测试
│   ├── uploads/                  明信片图片静态资源
│   └── pom.xml
├── frontend/                     Vue 3 前端
│   ├── src/
│   │   ├── api/                  axios client + 领域 API(forecast / events / apod / logs / auth / favorite / sky)
│   │   ├── assets/main.css       全局样式(深空配色 / 毛玻璃 / 淡入动画)
│   │   ├── components/           DarkSkyLeaflet / MoonPhaseCanvas / StarCanvas / ConstellationMythCard / BortleInfoModal / LanguageSwitcher 等
│   │   ├── router/               Vue Router
│   │   ├── stores/               Pinia(auth / favorites)
│   │   ├── i18n/                 三语 locales(zh / en / ja)
│   │   ├── views/                6 个主视图(DarkSkyMap / ForecastPanel / AstroTimeline / SkyAtlas / StarryPostcard / Favorites / Login)+ NotFound
│   │   ├── App.vue               顶部导航(响应式汉堡菜单 + 语言切换)
│   │   └── main.ts
│   ├── tailwind.config.js        自定义深空色板 + 字体
│   └── vite.config.ts            proxy /api /uploads
├── docs/
│   ├── USER_MANUAL.md            用户操作手册
│   └── plans/                    设计文档与实施计划
└── README.md
```

## 测试

后端单元 / 集成测试(使用 H2 内存库,`@Profile("!test")` 隔离种子数据):
```bash
cd backend
mvn clean test
```

前端类型检查 + 构建:
```bash
cd frontend
npm run build
```

## API 一览

| 方法 | 路径 | 说明 | 鉴权 |
|---|---|---|---|
| GET | `/api/v1/astro/forecast?lat=&lng=` | 观星指数预测(虚拟线程聚合) | 公开 |
| GET | `/api/v1/events` | 2026 全年天文事件列表 | 公开 |
| GET | `/api/v1/events/{id}` | 单条事件详情 | 公开 |
| GET | `/api/v1/apod` | NASA 每日天文一图 | 公开 |
| GET | `/api/v1/sky/constellation-view?lat=&lng=&date=&hour=` | 星图视图(可见星 + 12 星座连线) | 公开 |
| GET | `/api/v1/sky/myths/{constellation}` | 星座神话故事卡(希腊 + 中国) | 公开 |
| POST | `/api/v1/auth/register` | 用户注册 | 公开 |
| POST | `/api/v1/auth/login` | 用户登录 | 公开 |
| GET | `/api/v1/favorites` | 列出收藏观测点 | 需 JWT |
| POST | `/api/v1/favorites` | 添加收藏观测点 | 需 JWT |
| DELETE | `/api/v1/favorites?lat=&lng=` | 删除收藏观测点 | 需 JWT |
| POST | `/api/v1/logs/upload` | 上传星空明信片 | 公开 |
| GET | `/api/v1/logs` | 明信片画廊列表 | 公开 |
| GET | `/api/v1/logs/{id}` | 单张明信片详情 | 公开 |
| GET | `/uploads/**` | 明信片图片静态资源 | 公开 |

## License

MIT(课程设计)
