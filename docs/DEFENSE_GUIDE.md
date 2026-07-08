# CityGlow JavaWeb 课程设计答辩分工与讲稿指南

> 两人小组答辩参考文档 · 建议总时长 15-20 分钟(讲解 10-12 分钟 + 演示 3-5 分钟 + 答辩 3-5 分钟)

---

## 一、整体分工原则

**JavaWeb 课程设计,核心评分在后端 Java 实现**。两人**平分后端**,
前端作为「项目完整性展示」一句带过即可,不作为讲解重点。

| 角色 | 同学 A | 同学 B |
|---|---|---|
| **主线(后端,平分)** | 后端架构 + JDK 21 并发特性 + JWT 安全 | 数据库设计 + 第三方 API 集成 + REST API 实现 |
| **重点讲解** | Spring Boot 3.2 架构 / 虚拟线程 / 结构化并发 / Spring Security + JWT | JPA 实体与表设计 / Open-Meteo 与 NASA API 集成 / Caffeine 缓存 / 7 个 Controller 实现 |
| **辅线(一句带过)** | 前端用了 Vue3 + Leaflet,实现了地图和星图可视化(不展开) | 前端用了 Canvas 画星座、TailwindCSS 做深色主题、三语切换(不展开) |
| **代码量** | 后端 Service / Config / Util 层约 50% | 后端 Controller / Repository / Entity / DTO 层约 50% |
| **答辩重点** | 技术深度(并发、安全、Spring 原理) | 工程完整度(API 设计、数据建模、第三方集成、缓存策略) |

---

## 二、答辩 PPT 大纲(共 12 页)

| 页码 | 标题 | 主讲人 | 内容 |
|---|---|---|---|
| 1 | 封面 | A | 项目名 + 组员姓名 + 指导老师 + 日期 |
| 2 | 项目背景与目标 | A | 光污染问题 + "寻回最后一片星野" + 项目定位 |
| 3 | 技术栈总览 | A | Spring Boot 3.2 + JDK 21 + JPA + MySQL + Spring Security + Caffeine(后端为主) |
| 4 | 系统架构图 | A | 前后端分离 + 第三方 API + 数据库 + 缓存分层(画一张架构图,突出后端分层) |
| 5 | **数据库设计** | B | 4 张表(users / astro_events / observation_logs / favorite_locations)ER 图 + 关系 |
| 6 | **后端核心 1**:JDK 21 虚拟线程 + 结构化并发 | A | ForecastService 虚拟线程 + PostcardService StructuredTaskScope |
| 7 | **后端核心 2**:第三方 API 集成 + 缓存策略 | B | Open-Meteo(免 key)+ NASA APOD(404 回退)+ Caffeine 三级缓存 |
| 8 | **后端核心 3**:JWT 认证与 Spring Security | A | 注册登录流程 + JWT 签发校验 + SecurityFilterChain + 放行规则 |
| 9 | **后端核心 4**:REST API 设计 + 投影算法 | B | 7 个 Controller 一览 + ApiResponse 统一响应 + 星空投影球面三角算法 |
| 10 | **后端核心 5**:星空投影算法 | B | 赤道坐标→地平坐标→立体投影,北极星高度角验证 |
| 11 | 前端简述 + 功能演示截图 | A | 一页带过:Vue3 + Leaflet + Canvas,6 大功能截图拼贴 |
| 12 | 总结与展望 | A+B | A 讲技术亮点(并发 + 安全),B 讲工程亮点(API + 缓存 + 算法) |

---

## 三、答辩讲稿(详细版)

### 开场(同学 A,1 分钟)

> 各位老师好,我们小组的课程设计项目叫「城市光影 CityGlow」,
> 是一款基于光污染监测的星空寻回地图。
>
> 城市的灯光让 80% 的城市居民看不到银河,我们的项目聚合气象数据、
> 光污染等级和天文事件,告诉用户「今夜是否值得仰望星空」。
>
> 后端用 Spring Boot 3.2 + JDK 21,我们两人**平分后端**:
> 我负责架构搭建、JDK 21 并发特性应用、Spring Security + JWT 认证;
> [同学 B] 负责数据库设计、第三方 API 集成、REST API 实现和星空投影算法。
> 前端用 Vue3 做了可视化展示,待会简单演示,这里先聚焦后端实现。

---

### 后端讲解 1:架构与并发(同学 A,4 分钟)

#### 1. 技术栈与分层架构(1 分钟)

> 后端用 Spring Boot 3.2.5 + JDK 21,严格按经典三层架构组织:
> - **Controller 层**:7 个 REST 控制器,统一用 `ApiResponse<T>` 包装响应,
>   返回 `{code, message, data}` 结构
> - **Service 层**:核心业务逻辑,包括 ForecastService(观星预测)、
>   PostcardService(明信片生成)、AuthService(认证)、StarProjectionService(星空投影)等
> - **Repository 层**:Spring Data JPA,4 个 Repository 接口,自动生成 CRUD
> - **Domain/Entity 层**:Record DTO + JPA 实体
>
> 选 Spring Boot 3.2 而不是 2.x 的原因:3.x 要求 JDK 17+,
> 能支持 JDK 21 的虚拟线程、Record、Pattern Matching 等新特性,
> 3.2 还原生支持虚拟线程(`spring.threads.virtual.enabled=true`)。

#### 2. JDK 21 虚拟线程(1.5 分钟,重点讲)

> JDK 21 的虚拟线程是我重点应用的新特性。
>
> 在 `ForecastService` 里,我需要并行调用 Open-Meteo 的多个端点(当前天气 + 日出日落),
> 传统做法是用 `CompletableFuture` 或 `Executors.newFixedThreadPool()` 线程池,
> 代码复杂且资源开销大 — 平台线程每个占 1MB 栈空间,线程池开多了浪费内存,开少了并发不够。
>
> 我用 `Executors.newVirtualThreadPerTaskExecutor()` 创建虚拟线程执行器,
> 每个任务跑在一个虚拟线程上。虚拟线程由 JVM 管理,不绑定 OS 线程,
> 在 I/O 阻塞时自动让出载体线程给其他虚拟线程,内存开销只有几 KB,
> 可以轻松创建上千个并发任务。
>
> 代码上是 `try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
> var f1 = executor.submit(() -> callWeatherApi()); var f2 = executor.submit(() -> callSunApi());
> return combine(f1.get(), f2.get()); }`,比 CompletableFuture 简洁得多。
>
> 另外,`application.yml` 里配了 `spring.threads.virtual.enabled=true`,
> Tomcat 的请求处理线程也自动切换到虚拟线程,整体吞吐量显著提升。

**答辩可能提问**:
- 老师:虚拟线程和传统线程池有什么区别?
- 答:虚拟线程由 JVM 管理,不绑定 OS 线程,在 I/O 阻塞时会自动让出载体线程,
  内存开销从 MB 级降到 KB 级。传统线程池适合 CPU 密集型,虚拟线程适合 I/O 密集型。
  对于我们这种大量调第三方 API 的场景,虚拟线程优势明显。

#### 3. 结构化并发(1.5 分钟,重点讲)

> 另一个 JDK 21 特性是结构化并发(preview 特性,需 `--enable-preview`)。
>
> 在 `PostcardService` 生成明信片时,需要并行做三件事:
> 压缩图片、查询月相、查询 Bortle 等级。这三件事**任一失败就应该全部取消**,
> 避免部分成功导致脏数据(比如图片压缩成功但 Bortle 查询失败,生成一张没有光污染等级的明信片)。
>
> 我用 `StructuredTaskScope.ShutdownOnFailure` 创建作用域,在里面 `fork` 三个子任务:
>
> ```java
> try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
>     var imageTask = scope.fork(() -> compressImage(file));
>     var moonTask = scope.fork(() -> calculateMoonPhase(lat, lng));
>     var bortleTask = scope.fork(() -> estimateBortle(lat, lng));
>     scope.join();              // 等待全部完成
>     scope.throwIfFailed();     // 任一失败则抛异常,其他已被自动取消
>     return buildResult(imageTask.get(), moonTask.get(), bortleTask.get());
> }
> ```
>
> 如果任一子任务抛异常,作用域自动关闭其他子任务,保证「要么全成功,要么全失败」。
> 这比 `CompletableFuture.allOf` + 手动异常处理简洁得多,代码可读性也更好。

**答辩可能提问**:
- 老师:为什么不用 CompletableFuture?
- 答:CompletableFuture 的异常处理很麻烦,`allOf` 不会在任一失败时取消其他,
  需要手动写取消逻辑。结构化并发天然支持「失败即关闭」,语义更清晰,
  而且代码结构上是同步的 `try-with-resources`,更容易理解。

---

### 后端讲解 2:安全认证(同学 A,2 分钟)

#### 4. Spring Security + JWT 认证(2 分钟)

> 用户认证用 Spring Security + JWT 0.12.x。
>
> **注册流程**:
> 1. 前端 POST `/api/v1/auth/register`,传 username + password
> 2. AuthService 用 BCryptPasswordEncoder 把密码哈希(加盐,防彩虹表)
> 3. 存入 users 表,密码字段是哈希值,不存明文
> 4. 注册成功后直接签发 JWT,免再次登录
>
> **登录流程**:
> 1. 前端 POST `/api/v1/auth/login`,传 username + password
> 2. AuthService 查 users 表,用 `passwordEncoder.matches()` 校验 BCrypt 哈希
> 3. 校验通过后,JwtUtil 用 HS256 算法 + 密钥签发 JWT,claim 里带 userId 和 username
> 4. 返回 Token 给前端,前端存 localStorage,后续请求带 `Authorization: Bearer <token>` 头
>
> **请求鉴权**:
> 我写了 `JwtAuthenticationFilter`,在 SecurityFilterChain 里拦截需要鉴权的请求,
> 5 步流程:提取 Token → 校验签名 → 解析 claim → 查用户是否存在 → 写入 SecurityContext。
>
> `SecurityConfig` 里精确配置了放行规则:
> - 公开接口(` /api/v1/sky/**`、`/api/v1/astro/**`、`/api/v1/auth/**` 等)`permitAll`
> - 收藏接口 `/api/v1/favorites/**` 必须 `authenticated`
> - CSRF 禁用(前后端分离用 JWT,不依赖 Cookie,不需要 CSRF 保护)
> - Session 设为 `STATELESS`(无状态,每次请求都靠 JWT 鉴权)

**答辩可能提问**:
- 老师:JWT 和 Session 有什么区别?
- 答:JWT 是无状态的,服务端不存 Session,Token 自包含用户信息和签名,
  适合分布式和前后端分离架构。缺点是无法主动失效(除非维护黑名单)。
- 老师:JWT 怎么主动失效?(比如用户改密码后)
- 答:标准 JWT 是无状态的,无法主动失效。两种方案:1) 维护 Token 黑名单(Redis);
  2) 缩短 Token 有效期 + Refresh Token。本项目是课程设计,用短期 Token + 前端清 localStorage 的简化方案。

---

### 后端讲解 3:数据库与 API 集成(同学 B,4 分钟)

#### 5. 数据库设计(1 分钟)

> 数据库用 MySQL 8,4 张表:
>
> | 表名 | 说明 | 关键字段 |
> |---|---|---|
> | `users` | 用户表 | id(自增主键)、username(唯一)、password(BCrypt 哈希)、created_at |
> | `astro_events` | 天文事件表 | id、title、event_time、event_type(METEOR/ECLIPSE/PLANET)、description |
> | `observation_logs` | 观星日志(明信片) | id、user_id(外键)、location_name、latitude、longitude、image_url、bortle_level |
> | `favorite_locations` | 收藏观测点 | id、user_id(外键)、name、latitude、longitude、created_at |
>
> 关系:`observation_logs` 和 `favorite_locations` 都通过 `user_id` 关联 `users` 表,
> 是一对多关系。
>
> JPA 实体用 Lombok 的 `@Getter @Setter @NoArgsConstructor` 减负样板代码,
> `ddl-auto=update` 启动时自动建表,不需要手动执行 SQL。
>
> 启动时还有 `AstroEventSeeder` 检查 `astro_events` 表是否为空,为空则插入 2026 全年 13 条天文事件种子数据(流星雨、日月食、行星冲日等),保证时间轴页面有数据。

#### 6. 第三方 API 集成(1.5 分钟,重点讲)

> 后端集成了两个第三方 API:

##### Open-Meteo(气象数据,免费开源)
> Open-Meteo 是开源免费的气象 API,**无需 API Key、无速率限制**,文档在 open-meteo.com。
> 之前用过 OpenWeatherMap,但 One Call API 3.0 需要付费订阅,免费 tier 调用会 401,所以换成了 Open-Meteo。
>
> `OpenMeteoClient` 用 Spring 6.1 的 RestClient(比 RestTemplate 更现代)调用:
> ```
> GET https://api.open-meteo.com/v1/forecast?latitude=39.9&longitude=116.4
>   &current=cloud_cover,temperature_2m,relative_humidity_2m,weather_code
>   &daily=sunrise,sunset&timezone=auto
> ```
> 反序列化到 `OpenMeteoResponse` record,字段用 `@JsonProperty` 映射 NASA 返回的 JSON key。

##### NASA APOD(每日天文一图)
> `NasaApodClient` 调用 `GET https://api.nasa.gov/planetary/apod?api_key=xxx&date=YYYY-MM-DD`。
>
> 这里有个**容错处理**:NASA 按 UTC 时间更新,有时当天数据还没生成会返回 404。
> 我在 `fetchApodFromApi` 方法里做了 404 自动回退 — 当天 404 就回退到前一天重试,
> 最多回退 3 天,连续 4 天都失败才抛异常。这样保证用户始终能看到一张 APOD 图片。
>
> 用 Spring 6.1 RestClient 的 `HttpClientErrorException.NotFound` 异常捕获 404,
> 循环回退,代码简洁。

**答辩可能提问**:
- 老师:为什么用 RestClient 不用 RestTemplate?
- 答:RestClient 是 Spring 6.1 引入的新客户端,API 更现代(流式 DSL),
  支持 URI builder、泛型 body、status handler,功能上对标 OkHttp 但不需要额外依赖。
  RestTemplate 从 Spring 3 就有了,API 比较老,官方文档也推荐用 RestClient 替代。
- 老师:API Key 怎么管理的?
- 答:NASA API Key 存在 `backend/.env` 环境变量文件里,通过 `spring-dotenv` 库
  加载到 Spring Environment,`@Value("${nasa.apod.key}")` 注入。
  `.env` 在 `.gitignore` 里,不会提交到 git。Open-Meteo 不需要 Key,更安全。

#### 7. Caffeine 缓存策略(1.5 分钟,重点讲)

> 第三方 API 调用有网络延迟和速率限制,我用 Caffeine 做本地缓存降低调用频率。
>
> `CacheConfig` 里定义了 3 个 Cache Bean,各自有不同的 TTL:
>
> | Cache Bean | 缓存内容 | TTL | 理由 |
> |---|---|---|---|
> | `apodCache` | NASA APOD 每日图片 | 24 小时 | APOD 一天只更新一次,缓存 24h 避免重复调用 |
> | `forecastCache` | 观星条件预测 | 30 分钟 | 气象数据半小时更新一次,30s 内重复请求直接走缓存 |
> | `skyViewCache` | 星图视图 | 1 小时 | 星空按小时变化,1h 缓存粒度足够 |
>
> 缓存 key 设计:
> - APOD:`date` 字符串(YYYY-MM-DD)
> - 预测:`lat,lng` 拼接(同一地点 30 分钟内复用)
> - 星图:`lat,lng,date,hour` 拼接(同一地点同一小时复用)
>
> 用法是 `cache.get(key, k -> fetchFromApi(k))`,Caffeine 的 `get` 方法原子性地
> 查缓存 → miss 时调 loader → 写缓存,线程安全。
>
> 选 Caffeine 而不是 Redis 的原因:本项目是单机部署,Caffeine 是进程内缓存,
> 零网络延迟,不需要额外部署 Redis,适合课程设计场景。
> 缓存数据(APOD URL、预测结果)丢失影响不大,大不了重新调一次 API。

**答辩可能提问**:
- 老师:Caffeine 缓存的淘汰策略是什么?
- 答:Caffeine 用 Window TinyLfu 算法,结合 LRU 和 LFU 的优点,
  命中率优于 Guava Cache。我配的是 `expireAfterWrite`(写入后多久过期),
  也可以配 `maximumSize`(最大条数)限制内存。

---

### 后端讲解 4:REST API 与算法(同学 B,3 分钟)

#### 8. REST API 设计(1 分钟)

> 后端实现了 7 个 Controller,共 14 个 REST 端点,全部遵循 RESTful 规范:
>
> | 方法 | 路径 | Controller | 说明 |
> |---|---|---|---|
> | GET | `/api/v1/astro/forecast` | AstroForecastController | 观星指数预测 |
> | GET | `/api/v1/events` | AstroEventController | 天文事件列表 |
> | GET | `/api/v1/events/{id}` | AstroEventController | 单条事件 |
> | GET | `/api/v1/apod` | ApodController | NASA 每日天文一图 |
> | GET | `/api/v1/sky/constellation-view` | SkyViewController | 星图视图 |
> | GET | `/api/v1/sky/myths/{constellation}` | SkyViewController | 星座神话 |
> | POST | `/api/v1/auth/register` | AuthController | 注册 |
> | POST | `/api/v1/auth/login` | AuthController | 登录 |
> | GET/POST/DELETE | `/api/v1/favorites` | FavoriteController | 收藏管理 |
> | POST | `/api/v1/logs/upload` | ObservationLogController | 上传明信片 |
> | GET | `/api/v1/logs` | ObservationLogController | 明信片列表 |
> | GET | `/api/v1/logs/{id}` | ObservationLogController | 单张明信片 |
>
> 所有响应统一用 `ApiResponse<T>` 包装:
> ```java
> public record ApiResponse<T>(int code, String message, T data) {
>     public static <T> ApiResponse<T> success(T data) {
>         return new ApiResponse<>(200, "success", data);
>     }
> }
> ```
> 前端 axios 拦截器统一判断 `code === 200`,是则解包返回 `data`,否则 reject。
> 这样 Controller 里只需 `return ApiResponse.success(result)`,不用写 try-catch。

#### 9. 星空投影算法(2 分钟,重点讲)

> 这是项目里最有技术含量的算法,在 `StarProjectionService` 里实现。
>
> **目标**:给定用户位置(经纬度)+ 日期 + 小时,计算某颗星在 Canvas 上的位置。
>
> **算法分三步**:

##### 第 1 步:计算恒星时(GMST)
> 地球自转,星空相对地面观察者旋转。需要先计算格林尼治平恒星时:
> ```
> T = (JD - 2451545.0) / 36525    // JD 是儒略日
> GMST = 280.46061837 + 360.98564736629 * (JD - 2451545.0) + 0.000387933 * T²
> ```
> 儒略日 JD 是从公元前 4713 年 1 月 1 日中午开始的连续天数,用来方便计算时间间隔。

##### 第 2 步:赤道坐标 → 地平坐标
> 恒星在天球上的位置用赤经(RA)+ 赤纬(Dec)表示,这是固定坐标。
> 但地面观察者看到的是方位角(Az)+ 高度角(Alt),会随时间变化。
>
> 转换公式(球面三角):
> ```
> H = GMST + 经度 - 赤经              // 时角
> sin(Alt) = sin(Dec)·sin(Lat) + cos(Dec)·cos(Lat)·cos(H)    // 高度角
> cos(Az) = (sin(Dec) - sin(Alt)·sin(Lat)) / (cos(Alt)·cos(Lat))   // 方位角
> ```
> 方位角还需要根据 `sin(H)` 的正负做象限修正(东半边还是西半边)。

##### 第 3 步:可见性过滤
> 只返回 `Alt > 0` 的星(在地平线以上的才可见),并按视星等排序(亮的在前)。
>
> 然后把每个星座的亮星 + 连线数据一起返回给前端,前端用 Canvas 立体投影绘制。

##### 验证
> **北极星测试**:北极星(Polaris)赤纬 89.26°,从北京(纬度 39.9°)观测,
> 高度角应该约等于纬度(39.9°)。我后端单元测试验证了这一点,
> 计算结果是 39.3°,偏差 0.6°,在容差范围内(算法正确)。

**答辩可能提问**:
- 老师:这个算法是你自己写的吗?参考了什么资料?
- 答:算法是参考天文学教材实现的,公式是标准的球面三角变换。
  数据(亮星表、星座连线)参考了 Stellarium 开源天文软件的格式,手动整理了 12 个 MVP 星座。
- 老师:精度怎么样?
- 答:对于科普级可视化足够了。没考虑岁差(地轴进动,周期 26000 年)、章动、大气折射,
  这些修正对肉眼观测影响极小(小于 1°),但对专业天文计算就不够了。

---

### 前端简述 + 演示(同学 A,1 分钟)

> 前端用 Vue3 + TypeScript + Vite,实现了 6 个功能页面:
> 暗夜地图(Leaflet 暗色瓦片)、观星条件预测、天文事件时间轴、
> 星图连接器(Canvas 投影 12 星座)、星空明信片画廊、用户登录与收藏。
>
> 配色用自定义的深空色板(深空黑 + 暗金 + 月光蓝),支持中英日三语切换。
> 前端不是重点,我们直接演示一下功能。

---

### 演示环节(同学 B 主操,3-5 分钟)

**演示流程**(B 操作,A 补充讲解):

1. **首页暗夜地图**(30 秒)
   - B:展示地图,点击一个位置,看预测面板滑出
   - A:这时后端用虚拟线程并行调了 Open-Meteo 的两个端点,500ms 内返回

2. **天文事件时间轴**(30 秒)
   - B:切到 `/timeline`,展示倒计时,点开一个事件详情
   - A:事件数据存在 MySQL,启动时由 AstroEventSeeder 自动插入 13 条

3. **星图连接器**(1 分钟,重点演示)
   - B:切到 `/sky`,展示今晚星空
   - B:拖动时间滑块,看星空旋转
   - B:悬停猎户座,看高亮效果
   - B:点击猎户座,弹出神话卡
   - A:这里后端做了赤道→地平的坐标投影算法,刚刚讲的球面三角公式

4. **星空明信片**(1 分钟)
   - B:切到 `/postcard`,上传一张图片,填坐标,点生成
   - A:后端用结构化并发并行处理图片压缩 + 水印 + 元数据查询
   - B:展示生成的明信片,暗金边框 + 邮票装饰

5. **用户系统**(30 秒)
   - B:展示注册登录,收藏一个观测点
   - A:JWT 认证,密码 BCrypt 哈希,收藏接口需带 Token

---

### 总结(同学 A + B,1 分钟)

**A(技术总结)**:
> 后端技术亮点是充分应用了 JDK 21 的三个核心特性:
> 虚拟线程提升 I/O 并发性能,结构化并发保证任务原子性,Record 简化 DTO 定义。
> 安全方面用 Spring Security + JWT 做了完整的用户认证和接口鉴权。

**B(工程总结)**:
> 工程上,我们设计了 4 张数据库表,集成了 Open-Meteo 和 NASA 两个第三方 API,
> 用 Caffeine 做了三级缓存降低调用频率,实现了 14 个 RESTful 端点。
> 算法上,自己实现了星空投影的球面三角变换,用北极星高度角验证了正确性。

---

## 四、老师可能提问与参考答案

### 后端架构相关(同学 A 主答)

**Q1:为什么选 Spring Boot 3.2 而不是 2.x?**
> Spring Boot 3.x 要求 JDK 17+,能支持 JDK 21 的新特性(虚拟线程、Record、Pattern Matching)。
> 3.2 还原生支持虚拟线程(`spring.threads.virtual.enabled`),2.x 不支持。
> 另外 3.x 用了 Jakarta EE 9+(包名从 `javax.*` 改成 `jakarta.*`),是未来主流。

**Q2:虚拟线程在什么场景下有优势?什么场景下反而更慢?**
> 优势场景:I/O 密集型(HTTP 调用、数据库查询、文件读写),因为虚拟线程在 I/O 阻塞时会自动让出载体线程。
> 劣势场景:CPU 密集型计算,虚拟线程没有优势,甚至因为调度开销略慢。这种情况用平台线程池更好。

**Q3:JWT Token 怎么主动失效?(比如用户改密码后)**
> 标准 JWT 是无状态的,无法主动失效。两种方案:
> 1. 维护一个 Token 黑名单(Redis),每次校验时查一下
> 2. 缩短 Token 有效期(如 1 小时),配合 Refresh Token 续期
> 本项目是课程设计,用短期 Token + 退出时前端清 localStorage 的简化方案。

### 后端数据与 API 相关(同学 B 主答)

**Q4:Caffeine 缓存和 Redis 缓存有什么区别?为什么选 Caffeine?**
> Caffeine 是本地(进程内)缓存,Redis 是分布式缓存。
> Caffeine 优势:零网络延迟,不需要额外部署,适合单机场景。
> Redis 优势:支持多实例共享,支持持久化,适合分布式部署。
> 本项目是单机部署,且缓存数据(APOD 图片 URL、预测结果)丢失影响不大,选 Caffeine 足够。

**Q5:Open-Meteo 为什么不用 OpenWeatherMap?**
> OpenWeatherMap 的 One Call API 3.0 需要付费订阅,免费 tier 调用会 401。
> Open-Meteo 是开源免费的,无需 API Key,无速率限制,数据质量也足够,
> 更适合课程设计场景。这也是我们开发过程中踩坑后换的。

**Q6:Open-Meteo 的数据准确性怎么样?**
> Open-Meteo 用欧洲中期天气预报中心(ECMWF)和德国气象局(DWD)的数值模型,
> 在欧洲地区精度很高,亚洲地区精度略低但够用。对我们这种「观星指数」的粗粒度场景足够。
> 如果要做精确预报,可以集成中央气象台的 API,但需要审批。

**Q7:数据库表为什么这么设计?考虑过第三范式吗?**
> 4 张表满足第三范式:`users` 和 `observation_logs`、`favorite_locations` 是一对多关系,
> 通过 `user_id` 外键关联。`astro_events` 是独立的种子数据表,不关联用户。
> 没有做物理外键约束(JPA 里没加 `@ForeignKey`),因为应用层已经保证了数据一致性,
> 物理外键会影响删除性能和分库分表扩展。

**Q8:星空投影算法的精度怎么样?**
> 对于科普级可视化足够了。没考虑岁差(地轴进动,周期 26000 年)、章动、大气折射,
> 这些修正对肉眼观测影响极小(小于 1°),但对专业天文计算就不够了。
> 验证方法:北极星高度角应该约等于当地纬度,北京 39.9°,计算结果 39.3°,偏差 0.6°。

**Q9:REST API 的统一响应结构是怎么设计的?**
> 所有接口返回 `ApiResponse<T>` record,结构是 `{code, message, data}`。
> Controller 里只写 `return ApiResponse.success(result)`,不写 try-catch。
> 前端 axios 拦截器统一判断 `code === 200`,是则解包返回 `data`,否则 reject。
> 这样前后端契约清晰,错误处理统一。

**Q10:如果第三方 API 挂了怎么办?有没有降级策略?**
> 有。APOD 接口有 404 自动回退(当天数据没生成就取前一天,最多回退 3 天)。
> Caffeine 缓存也是天然的降级 — API 挂了缓存还在,短时间内还能返回旧数据。
> 极端情况下(连续 4 天 404),抛 RuntimeException,前端显示「加载失败」,
> 不影响其他功能(APOD 是装饰性功能,挂了不影响核心的观星预测)。

---

## 五、答辩前准备清单

### PPT 准备
- [ ] 12 页 PPT(按上面大纲,**重点放在后端**,前端一页带过)
- [ ] 系统架构图(后端三层分层 + 第三方 API + 数据库 + 缓存)
- [ ] 数据库 ER 图(4 张表 + 关系)
- [ ] 虚拟线程 vs 传统线程对比图
- [ ] 结构化并发代码片段截图
- [ ] JWT 认证流程图(注册/登录/鉴权 3 步)
- [ ] 星空投影算法示意图(赤道坐标 → 地平坐标 → Canvas 平面)
- [ ] Caffeine 三级缓存表格(TTL + 理由)
- [ ] REST API 端点一览表
- [ ] 6 大功能截图拼贴(一页,不展开讲)

### 演示准备
- [ ] 后端提前启动好(IDEA + JDK 21 + `--enable-preview`)
- [ ] 前端 `npm run dev` 提前跑起来
- [ ] 浏览器收藏 `http://localhost:5173/`
- [ ] 准备一张测试图片(明信片上传用)
- [ ] 准备一个测试账号(避免现场注册浪费时间)
- [ ] 收藏一两个观测点(演示用)
- [ ] 备用:录屏一份完整演示视频(防止现场网络/API 故障)

### 代码准备
- [ ] 熟悉自己负责的全部后端代码,能快速定位到关键文件
- [ ] 了解对方负责的后端代码,能回答基础问题
- [ ] 准备 3-4 段「最自豪的代码」截图(PPT 备用)
  - A:`ForecastService` 的虚拟线程代码
  - A:`PostcardService` 的 StructuredTaskScope 代码
  - A:`SecurityConfig` + `JwtAuthenticationFilter` 的安全配置
  - B:`OpenMeteoClient` + `NasaApodClient` 的第三方调用
  - B:`StarProjectionService` 的球面三角投影算法
  - B:`CacheConfig` 的三级缓存配置

### 分工确认
- [ ] 两人各自对着讲稿计时,A 控制在 7 分钟(架构+并发+安全),B 控制在 7 分钟(数据库+API+算法)
- [ ] 互相模拟提问,确保能接住对方的问题
- [ ] 确定谁操作演示、谁补充讲解
- [ ] 准备一句「结束语」(感谢老师倾听)

---

## 六、答辩当天注意事项

1. **提前 15 分钟到场**,测试投影、网络、演示环境
2. **PPT 字号不小于 24pt**,确保后排老师能看清
3. **重点讲后端 Java 实现**,前端演示时一句话带过(「这是前端页面,我们重点看后端实现」)
4. **演示时关掉无关浏览器标签**,避免干扰
5. **如果演示出问题**(API 挂了、报错),不要慌,说「这是第三方 API 波动,
   我们有缓存机制」然后切到截图继续讲
6. **老师提问时**,听完整问题再答,不会就说「这个我们后续可以优化」,
   不要硬编
7. **两人配合**,一方答不上来时另一方补充,但不要抢话

---

## 七、加分项提示

如果老师问「项目还有什么可以扩展的」,可以提以下方向(聚焦后端):

1. **WebSocket 实时推送**:天文事件发生时实时通知在线用户(Spring WebSocket)
2. **Redis 分布式缓存 + Token 黑名单**:支持多实例部署 + JWT 主动失效
3. **消息队列异步处理**:明信片生成走 RabbitMQ 异步,避免 HTTP 长连接
4. **更精确的天文算法**:加入岁差、章动、大气折射修正(参考 Jean Meeus《天文算法》)
5. **API 限流**:用 Bucket4j 给第三方 API 调用加速率限制,避免超额
6. **定时任务**:用 Spring Scheduling 每天定时拉取 APOD,预热缓存

---

祝答辩顺利!
