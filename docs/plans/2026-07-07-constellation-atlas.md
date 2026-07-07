# 星座连接器(Constellation Atlas)— 实施计划

> 创建日期:2026-07-07
> 设计文档:2026-07-07-constellation-atlas-design.md
> 执行方式:subagent-driven(每任务 spawn implementer + reviewer)

## 任务执行规范

每个任务严格遵循 TDD:
1. 写失败测试 → 2. 运行确认失败 → 3. 写最小实现 → 4. 运行确认通过 → 5. 提交

提交信息格式:`feat(sky): <任务标题简述>`

---

## Phase 1 — 数据与算法(后端)

### Task 1.1 — 创建 StarPoint record
- **上下文**:单颗可见星的数据载体
- **测试**:`StarPointTest` — 验证 record 字段(HIP, mag, az, alt)
- **实现**:`domain/StarPoint.java`
  ```java
  public record StarPoint(int hip, double mag, double az, double alt) {}
  ```
- **验证**:`mvn test -Dtest=StarPointTest`

### Task 1.2 — 创建 ConstellationView record
- **上下文**:单个星座的可见数据(含星点 + 连线 + 名称)
- **测试**:`ConstellationViewTest` — 验证字段
- **实现**:`domain/ConstellationView.java`
  ```java
  public record ConstellationView(
      String name, String latin, String chinese,
      List<StarPoint> stars, List<int[]> lines  // lines: [[idx1, idx2]...]
  ) {}
  ```
- **验证**:`mvn test -Dtest=ConstellationViewTest`

### Task 1.3 — 创建 SkyViewResult record
- **上下文**:整个星图视图的顶层响应
- **测试**:`SkyViewResultTest` — 验证字段 + null 兜底
- **实现**:`domain/SkyViewResult.java`
  ```java
  public record SkyViewResult(
      List<StarPoint> visibleStars,
      List<ConstellationView> constellations,
      String observerLat, String observerLng,
      String date, int hour
  ) {}
  ```
- **验证**:`mvn test -Dtest=SkyViewResultTest`

### Task 1.4 — 创建 MythCard record
- **上下文**:神话故事卡数据载体
- **测试**:`MythCardTest` — 验证字段
- **实现**:`domain/MythCard.java`
  ```java
  public record MythCard(
      String constellation, String culture,  // "greek" | "chinese"
      String title, String story
  ) {}
  ```
- **验证**:`mvn test -Dtest=MythCardTest`

### Task 1.5 — 创建 bright-stars.json 星表
- **上下文**:精简版亮星表(≤ 5.0 等)
- **实现**:`resources/data/bright-stars.json`,字段:`[{hip, mag, ra, dec}]`
- **数据源**:从 astronomy-engine 或 stellarium 数据集精简
- **验证**:文件存在,JSON 可解析,约 1600 颗星

### Task 1.6 — 创建 constellations.json 12 星座连线
- **上下文**:12 个 MVP 星座的连线图
- **实现**:`resources/data/constellations.json`,字段:
  ```json
  [{"name":"orion","latin":"Orion","chinese":"猎户座",
    "stars":[27989,26727,...],"lines":[[0,1],[1,2],...]}]
  ```
- **验证**:JSON 可解析,12 条目,每条 lines 非空

### Task 1.7 — 创建 myths.json 24 篇神话
- **上下文**:12 星座 × 2 文化的神话故事
- **实现**:`resources/data/myths.json`,字段:
  ```json
  [{"constellation":"orion","culture":"greek",
    "title":"猎人与蝎子","story":"..."}]
  ```
- **验证**:JSON 可解析,24 条目

### Task 1.8 — 实现 ConstellationDataService
- **上下文**:加载 3 个 JSON 到内存,提供查询接口
- **测试**:`ConstellationDataServiceTest` — 验证加载 + 按 name 查询
- **实现**:`service/ConstellationDataService.java`
  - `@PostConstruct` 加载 3 个 JSON
  - `getConstellation(name)` → ConstellationView(原始数据,无投影)
  - `getMyth(name, culture)` → MythCard
  - `getAllConstellations()` → List
- **验证**:`mvn test -Dtest=ConstellationDataServiceTest`

### Task 1.9 — 实现 StarProjectionService
- **上下文**:核心算法 — 赤道坐标 → 地平坐标 → 立体投影
- **测试**:`StarProjectionServiceTest` — 验证已知位置(北京 2026-07-07 22:00 看到的猎户座位置)
- **实现**:`service/StarProjectionService.java`
  - `project(double lat, double lng, LocalDate date, int hour, List<Star> stars)` → List<StarPoint>
  - 算法:GMST → LST → 赤道→地平 → 过滤 Alt>0 → 立体投影
- **验证**:`mvn test -Dtest=StarProjectionServiceTest`

### Task 1.10 — 实现 SkyViewController
- **上下文**:REST 接口,组装 SkyViewResult
- **测试**:`SkyViewControllerTest` — MockMvc 验证请求/响应
- **实现**:`controller/SkyViewController.java`
  - `GET /api/v1/sky/constellation-view?lat=&lng=&date=&hour=`
  - `GET /api/v1/sky/myths/{constellation}`
- **验证**:`mvn test -Dtest=SkyViewControllerTest`

### Task 1.11 — 配置 SecurityConfig + CacheConfig
- **上下文**:放行 /sky/** 公开访问,新增 skyViewCache
- **测试**:无需新增,集成测试覆盖
- **实现**:
  - `SecurityConfig.java` 加 `.requestMatchers(GET, "/api/v1/sky/**").permitAll()`
  - `CacheConfig.java` 加 `skyViewCache` Bean(TTL 1h,key=lat,lng,date,hour)
- **验证**:`mvn test -Dtest=SkyViewControllerTest`

---

## Phase 2 — 前端 MVP

### Task 2.1 — 写 api/sky.ts
- **实现**:`frontend/src/api/sky.ts`
  ```ts
  export interface StarPoint { hip: number; mag: number; az: number; alt: number }
  export interface ConstellationView { name; latin; chinese; stars: StarPoint[]; lines: number[][] }
  export interface SkyViewResult { visibleStars; constellations; observerLat; observerLng; date; hour }
  export interface MythCard { constellation; culture; title; story }
  export const skyApi = {
    getSkyView: (lat, lng, date, hour) => apiClient.get('/sky/constellation-view', ...),
    getMyth: (constellation) => apiClient.get(`/sky/myths/${constellation}`),
  }
  ```
- **验证**:TypeScript 编译通过

### Task 2.2 — 写 StarCanvas.vue 基础(星点)
- **实现**:`components/StarCanvas.vue`
  - props: stars, constellations
  - Canvas 渲染:深空背景 + 星点(大小 = 6 - mag)
  - 暂不画连线
- **验证**:页面能看到星点

### Task 2.3 — StarCanvas 加星座连线
- **实现**:在 StarCanvas.vue 加连线绘制
  - 按 constellations[].lines 索引连接星点
  - 暗金色 #c5a572 60% 透明
- **验证**:页面能看到星座连线

### Task 2.4 — 写 SkyControlBar.vue
- **实现**:`components/SkyControlBar.vue`
  - 日期选择器(input type=date)
  - 位置显示(只读,显示当前 lat/lng)
  - "定位"按钮(触发浏览器 geolocation)
- **验证**:控制条能改日期

### Task 2.5 — 写 SkyAtlas.vue 页面
- **实现**:`views/SkyAtlas.vue`
  - 组合 SkyControlBar + StarCanvas
  - onMounted 自动定位 + 加载今晚星图
  - 调用 skyApi.getSkyView
- **验证**:页面能显示星图

### Task 2.6 — router 加 /sky 路由
- **实现**:`router/index.ts` 加 `{ path: '/sky', component: SkyAtlas }`
- **验证**:访问 /sky 能打开页面

### Task 2.7 — App.vue 导航加入"星图"入口
- **实现**:`App.vue` 导航栏加链接
- **i18n**:zh/en/ja 加 `nav.sky` 翻译
- **验证**:点击导航能跳转 /sky

---

## Phase 3 — 交互

### Task 3.1 — StarCanvas 鼠标悬停检测
- **实现**:Canvas mousemove 事件,计算鼠标位置最近的星座(点到线段距离)
- **emit**:hovered-constellation 事件
- **验证**:悬停时 console.log 星座名

### Task 3.2 — 悬停高亮动画
- **实现**:
  - 高亮星座:星点放大 1.5x + 连线 100% 不透明 + 辉光
  - 其他星座:透明度 50%
  - requestAnimationFrame 呼吸效果(2s loop)
- **验证**:视觉上能看清高亮

### Task 3.3 — 写 ConstellationMythCard.vue
- **实现**:`components/ConstellationMythCard.vue`
  - 双栏布局:希腊 | 中国
  - 羊皮纸背景 + 噪点纹理
  - 关闭按钮
  - "加入星图明信片"按钮(emit 事件,先不实现跳转)
- **验证**:组件能渲染

### Task 3.4 — 点击星座弹神话卡
- **实现**:StarCanvas click 事件 → emit selected-constellation → SkyAtlas 显示 ConstellationMythCard
- **调用**:skyApi.getMyth(name)
- **验证**:点击星座弹出神话卡

### Task 3.5 — 神话卡双栏切换
- **实现**:底部按钮"只看希腊" / "只看中国" / "双栏"
- **验证**:切换视图正常

---

## Phase 4 — 时间滑块

### Task 4.1 — 后端 API 支持 hour 参数(已实现)
- **验证**:Task 1.10 已包含 hour 参数

### Task 4.2 — 前端时间滑块组件
- **实现**:SkyControlBar 加时间滑块(0-23h)
- **emit**:hour-change 事件
- **验证**:拖动滑块触发事件

### Task 4.3 — 拖动时星空旋转
- **实现**:
  - 拖动时防抖(200ms)调用 skyApi.getSkyView(lat, lng, date, hour)
  - 加载时显示加载态
  - 平滑过渡(Canvas 重绘时留 1 帧尾迹)
- **验证**:拖动滑块星空变化

---

## Phase 5 — 视觉打磨

### Task 5.1 — Canvas 加载动画
- **实现**:页面加载时,Canvas 从中心辐射展开 + 星点逐颗淡入(50ms stagger)
- **验证**:视觉动画正常

### Task 5.2 — 星座连线绘制动画
- **实现**:每星座按 lines 顺序逐线段绘制,带辉光拖尾(每条 200ms)
- **验证**:动画流畅

### Task 5.3 — 装饰角花 + 地平线环
- **实现**:
  - Canvas 四角 SVG 铜版画角花
  - 地平线淡金色环(半径 = Canvas 短边 * 0.45)
- **验证**:装饰元素显示

### Task 5.4 — 方位标
- **实现**:Canvas 上绘制 N/E/S/W Latin 简写 + 中文小字
- **验证**:方位标显示

### Task 5.5 — 收藏点下拉联动
- **实现**:SkyControlBar 位置下拉列出已收藏观测点(useFavoritesStore)
- **验证**:选择收藏点切换星图

### Task 5.6 — 明信片跳转
- **实现**:神话卡"加入星图明信片"按钮 → router.push('/postcard', { state: { skySnapshot } })
- **验证**:跳转到 StarryPostcard 页面

---

## 验收标准

- [ ] 12 星座全部可见且连线正确
- [ ] 鼠标悬停高亮 + 工具提示
- [ ] 点击星座弹神话卡(希腊 + 中国)
- [ ] 时间滑块 0-23h 星空旋转
- [ ] 收藏点下拉切换位置
- [ ] 加载动画流畅
- [ ] 移动端基本可用(不要求完美)

## 不做的事(YAGNI)

见设计文档第 8 节。
