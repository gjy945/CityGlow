# 城市光影 CityGlow · 用户操作手册

> 基于光污染监测的星空寻回地图 · JavaWeb 课程设计

---

## 目录

1. [环境准备](#1-环境准备)
2. [首次启动](#2-首次启动)
3. [IDEA 中运行(推荐)](#3-idea-中运行推荐)
4. [功能模块操作](#4-功能模块操作)
5. [常见问题排查](#5-常见问题排查)
6. [API 速查](#6-api-速查)

---

## 1. 环境准备

### 1.1 必备软件

| 软件 | 版本要求 | 下载地址 |
|---|---|---|
| JDK | 21(LTS) | https://adoptium.net/ |
| Maven | 3.9+ | https://maven.apache.org/download.cgi |
| MySQL | 8.0+ | https://dev.mysql.com/downloads/ |
| Node.js | 18+ | https://nodejs.org/ |
| IntelliJ IDEA | Ultimate 或 Community + Spring Boot 插件 | https://www.jetbrains.com/idea/ |

> **注**:气象数据使用 Open-Meteo(https://open-meteo.com/),**免费开源、无需 API Key、无速率限制**,因此环境准备中不需要申请气象 API Key。

### 1.2 验证环境

打开 PowerShell(或终端),逐条执行:

```powershell
java -version        # 应显示 21.x
mvn -version         # 应显示 3.9+
node -v              # 应显示 v18+
mysql --version      # 应显示 8.x
```

若 `mysql` 命令找不到,说明 MySQL 未加入 PATH,可暂不处理(后端用 JDBC 直连,不依赖命令行)。

### 1.3 配置 JDK 21 为系统默认

Windows:
1. 右键「此电脑」→ 属性 → 高级系统设置 → 环境变量
2. 系统变量 → 新建 → `JAVA_HOME` = `D:\Dev-Env\Java\JDK\JDK 21`(你的 JDK 21 安装路径)
3. 系统变量 → `Path` → 编辑 → 新建 → `%JAVA_HOME%\bin`
4. 重启 PowerShell,再次 `java -version` 确认显示 21

---

## 2. 首次启动

### 2.1 克隆/获取项目

项目位于 `D:\Dev-Projects\personal\project\CityGlow\`,目录结构:

```
CityGlow/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue3 前端
└── docs/plans/       # 设计文档
```

### 2.2 配置后端环境变量(关键!)

**第 1 步:创建 `.env` 文件**

进入 `backend/` 目录,复制 `.env.example` 为 `.env`:

```powershell
cd D:\Dev-Projects\personal\project\CityGlow\backend
Copy-Item .env.example .env
```

**第 2 步:编辑 `.env`,填入真实值**

用记事本或 VS Code 打开 `backend/.env`,修改为你的实际值:

```env
# MySQL 数据库连接
MYSQL_HOST=localhost
MYSQL_PORT=3306
MYSQL_USER=root
MYSQL_PASSWORD=你的MySQL密码       # ← 改这里,例如 123456

# 气象数据:Open-Meteo(免费开源,无需 API Key,无需配置)
# 文档: https://open-meteo.com/

# NASA APOD API Key(申请: https://api.nasa.gov/)
NASA_APOD_API_KEY=你的key          # ← 已提供:INoZW4trwlgvV9sKjpD4DpzWGV3GOkw5Xz0IjFCe
```

> ⚠️ `.env` 文件已被 `.gitignore` 排除,不会提交到 git,可安全存放密钥。
>
> 💡 **Open-Meteo 无需 API Key**,气象数据调用直接生效,无需任何配置。

**第 3 步:确认 MySQL 已启动**

- Windows 服务:检查 `MySQL80` 服务是否「正在运行」
- 或 PowerShell:`netstat -ano | findstr :3306` 应有 LISTENING 行

**第 4 步:数据库自动创建**

后端配置了 `createDatabaseIfNotExist=true`,首次启动会自动创建 `cityglow` 数据库,**无需手动建库**。

### 2.3 安装前端依赖

```powershell
cd D:\Dev-Projects\personal\project\CityGlow\frontend
npm install
```

首次安装约 1-2 分钟。

---

## 3. IDEA 中运行(推荐)

### 3.1 导入项目

1. 打开 IDEA → File → Open
2. 选择 `D:\Dev-Projects\personal\project\CityGlow\backend\pom.xml`
3. 点「Open as Project」
4. 等待 Maven 依赖下载完成(右下角进度条)

### 3.2 配置 JDK 21

1. File → Project Structure → Project
2. SDK 选择 21
3. Language level 选择 `21 (Preview) - ...`

### 3.3 配置运行参数(--enable-preview)

后端用了 JDK 21 结构化并发(preview 特性),必须开启 `--enable-preview`:

1. Run → Edit Configurations
2. 选中 `CityGlowApplication`
3. 点「Modify options」→ 勾选「Add VM options」
4. VM options 填:`--enable-preview`
5. Working directory 确认为 `D:\Dev-Projects\personal\project\CityGlow\backend`
6. 点 OK

### 3.4 启动后端

- 点绿色三角▶运行 `CityGlowApplication`
- 控制台出现 `Started CityGlowApplication in x.xxx seconds` 即成功
- 看到 13 条 `insert into astro_events` SQL 表示种子数据已自动插入

### 3.5 启动前端

新开一个终端(IDEA 底部 Terminal):

```powershell
cd D:\Dev-Projects\personal\project\CityGlow\frontend
npm run dev
```

出现:
```
VITE v8.x  ready in xxx ms
➜  Local: http://localhost:5173/
```

### 3.6 访问应用

浏览器打开 http://localhost:5173/

> 若 5173 被占用,Vite 自动切 5174,看终端输出为准。

---

## 4. 功能模块操作

### 4.1 模块 1:暗夜地图(首页)

**入口**:http://localhost:5173/ 或点导航「暗夜地图」

**操作**:

| 操作 | 效果 |
|---|---|
| 鼠标拖拽地图 | 平移视野 |
| 滚轮 / 双指 | 缩放 |
| **点击地图任意位置** | 右侧滑出「观星条件预测」面板,显示该地观星指数 |
| 点击左上角「定位」圆形按钮 | 定位到当前位置(需浏览器授权地理位置) |
| 点击右上角 APOD 缩略图 | 弹出 NASA 每日天文一图大图 + 科普说明 |
| 点击地图上的彩色圆点 | 查看该位置的天文事件标记 |

**地图元素说明**:
- 暗色底图:CartoDB Dark Matter(无 key)
- 彩色圆点 = 天文事件标记
  - 🟡 暗金 = 流星雨(METEOR)
  - 🔵 月光蓝 = 日月食(ECLIPSE)
  - ⚪ 星辉银 = 行星冲日/大距(PLANET)

### 4.2 模块 2:观星条件预测

**入口**:首页点击地图后自动滑出,或直接访问 `/forecast?lat=39.9&lng=116.4`

**面板内容**:

```
┌─────────────────────┐
│  观星指数    [×]    │
├─────────────────────┤
│      ╭───────╮      │
│      │  85   │      │ ← 0-100 分,越高越适合观星
│      ╰───────╯      │
│   今夜极佳!         │ ← 文字描述
├─────────────────────┤
│  [月相Canvas图]     │ ← 月相可视化
│  New Moon           │
├─────────────────────┤
│  云量    10%        │
│  Bortle  3级        │ ← 光污染等级(1=极佳,9=内城)
├─────────────────────┤
│  日落    18:32      │
│  日出    06:12      │
└─────────────────────┘
```

**指数颜色含义**:
- 🟡 暗金 80-100:今夜极佳!适合观星
- 🔵 月光蓝 60-79:适合观星
- 🟠 橙色 40-59:一般
- 🔴 红色 <40:不建议

**算法**:`指数 = 100 - 云量超 20% 部分×1.5 - 月亮照亮比例×30 - (Bortle-1)×8`

### 4.3 模块 3:天文事件时间轴

**入口**:导航「天文事件」或 http://localhost:5173/timeline

**操作**:

| 操作 | 效果 |
|---|---|
| 横向滚动 / 滚轮 | 浏览 2026 全年 13 条天文事件 |
| 点击事件卡片 | 弹出详情:科普介绍 + 最佳观测方位 |
| 按 ESC / 点遮罩 | 关闭详情弹窗 |
| 等待 | 顶部倒计时每秒自动刷新,显示距下一事件的天/时/分/秒 |

**事件类型**:
- METEOR 流星雨(暗金边框)
- ECLIPSE 日月食(月光蓝边框)
- PLANET 行星冲日/大距(星辉银边框)

**NEXT 标记**:下一个即将发生的事件会有脉冲动画 + 「NEXT」角标。

### 4.4 模块 4:星空打卡明信片

**入口**:导航「星空明信片」或 http://localhost:5173/postcard

#### 上传明信片

1. 在上传区拖入图片,或点击选择文件(仅支持 JPEG/PNG,≤10MB)
2. 填写:
   - 地点名称(如「北京灵山」)
   - 纬度(-90 到 90)
   - 经度(-180 到 180)
   - 或点「定位」按钮自动填入当前位置
   - 描述(可选,如「银河清晰可见」)
3. 点「生成明信片」
4. 后端并行处理(虚拟线程 + 结构化并发):
   - 解码压缩图片到长边 1920px
   - 查询该地月相 + Bortle 等级
   - 绘制水印(经纬度/时间/月相/Bortle)
   - 编码 JPEG 存盘
5. 成功后自动跳转到明信片详情页

#### 明信片详情页

展示后端生成的明信片:
- 暗金边框 + 邮票齿孔装饰(右上角)
- 邮戳装饰(右下角,含日期)
- 地点题字(Cormorant Garamond 字体)
- 经纬度 / Bortle 等级 / 创建时间(JetBrains Mono)
- 描述(italic)

#### 画廊

上传表单下方展示所有已生成的明信片,4 列网格(移动端 2 列),点击任意一张查看详情。

---

## 5. 常见问题排查

### 5.1 后端启动报 `Unable to determine Dialect`

**原因**:连不上 MySQL。

**排查**:
1. 确认 MySQL 服务正在运行:`netstat -ano | findstr :3306`
2. 确认 `.env` 中 `MYSQL_PASSWORD` 正确
3. 确认 `.env` 文件在 `backend/` 根目录(不是 `src/main/resources/`)
4. IDEA 中确认 Working directory 是 `backend/`(见 3.3)

**终极方案**:在 IDEA 运行配置 → Environment variables 直接填:
```
MYSQL_PASSWORD=你的密码;NASA_APOD_API_KEY=INoZW4trwlgvV9sKjpD4DpzWGV3GOkw5Xz0IjFCe
```
(注:Open-Meteo 无需 API Key,无需配置)

### 5.2 后端报 `spring-boot-buildpack-platform` 下载失败

**原因**:Maven 缓存损坏。

**解决**:
```powershell
Remove-Item -Recurse -Force "D:\Dev-Cache\MavenRepo\org\springframework\boot\spring-boot-buildpack-platform"
cd D:\Dev-Projects\personal\project\CityGlow\backend
mvn spring-boot:run -U
```
(路径换成你的 Maven 本地仓库路径)

### 5.3 后端报 `--enable-preview` 相关错误

**原因**:PostcardService 用了 JDK 21 结构化并发(preview 特性)。

**解决**:
- IDEA:Run → Edit Configurations → VM options 加 `--enable-preview`(见 3.3)
- 命令行:`mvn spring-boot:run`(pom.xml 已配好)

### 5.4 前端页面空白 / 控制台报 ECONNREFUSED

**原因**:后端没启动,或端口不对。

**解决**:
1. 确认后端在 8080 端口运行:`netstat -ano | findstr :8080`
2. 前端 Vite proxy 已配 `/api` → `:8080`,只需后端启动即可
3. 浏览器 F12 → Network 查看失败请求

### 5.5 点击地图没反应 / 报错

**原因**:Open-Meteo API 调用失败(网络问题 / 服务端故障)。

**排查**:
- 后端控制台看是否有连接超时或 HTTP 错误
- Open-Meteo 是免费服务,偶有波动,重试即可
- 确认网络能访问 `https://api.open-meteo.com/`(浏览器打开测试)

### 5.6 首页 APOD 缩略图不显示

**原因**:NASA APOD API 调用失败。

**说明**:APOD 是装饰性功能,不显示不影响其他功能。后端有 24h 缓存,首次调用失败后短时间内不会重试。

### 5.7 时间轴页面空白

**原因**:种子数据未插入。

**解决**:
1. 查看后端启动日志,应有 13 条 `insert into astro_events`
2. 若无,手动清空表后重启:
   ```sql
   TRUNCATE TABLE astro_events;
   ```
3. 重启后端,AstroEventSeeder 会自动插入

### 5.8 上传明信片报 400

**原因**:文件格式不对或超限。

**要求**:
- 仅支持 JPEG / PNG(MIME 校验)
- 单文件 ≤ 10MB
- 必须填 lat、lng(数字)

### 5.9 端口被占用

**后端 8080**:
```powershell
netstat -ano | findstr :8080 | findstr LISTENING
taskkill /PID <找到的PID> /F
```

**前端 5173**:Vite 会自动切换到 5174/5175,看终端输出。

---

## 6. API 速查

后端基础地址:http://localhost:8080

### 6.1 观星条件预测

```
GET /api/v1/astro/forecast?lat=39.9&lng=116.4
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "score": 68,
    "cloudCover": 10,
    "moonPhase": "New Moon",
    "bortleLevel": 5,
    "message": "适合观星",
    "sunrise": 1700000000,
    "sunset": 1700050000
  }
}
```

### 6.2 天文事件

```
GET /api/v1/events                    # 全部事件(按时间升序)
GET /api/v1/events?type=METEOR        # 按类型筛选
GET /api/v1/events?after=2026-07-01T00:00:00  # 仅未来事件
GET /api/v1/events/1                  # 单个事件详情
```

### 6.3 NASA 每日天文一图

```
GET /api/v1/apod
```

### 6.4 星空明信片

```
POST /api/v1/logs/upload    # multipart: image, lat, lng, locationName, description
GET /api/v1/logs            # 全部明信片列表
GET /api/v1/logs/1          # 单张明信片详情
```

### 6.5 静态资源

```
GET /uploads/{filename}.jpg    # 明信片图片
```

---

## 附录:JDK 21 特性应用

| 特性 | 应用位置 | 说明 |
|---|---|---|
| 虚拟线程 | `ForecastService` | 并行调 OpenWeather 两个端点,提升响应速度 |
| Record | 所有 DTO | `AstroData`、`ForecastResult`、`PostcardResult` 等 |
| 结构化并发 | `PostcardService` | `StructuredTaskScope.ShutdownOnFailure` 并行压缩+水印+元数据 |
| Pattern Matching | 事件类型分发 | switch 处理 METEOR/ECLIPSE/PLANET |

---

## 附录:数据库表结构

```sql
users              -- 用户表(id, username, avatar_url, created_at)
observation_logs   -- 观星日志(id, user_id, location_name, latitude, longitude, image_url, bortle_level, description, created_at)
astro_events       -- 天文事件(id, title, event_time, description, event_type)
```

启动时自动建表(ddl-auto=update),无需手动执行 SQL。

---

如有其他问题,查看源码注释或设计文档 `docs/plans/2026-07-07-cityglow-design.md`。
