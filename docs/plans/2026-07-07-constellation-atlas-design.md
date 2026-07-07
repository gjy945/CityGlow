# 星座连接器(Constellation Atlas)— 设计文档

> 创建日期:2026-07-07
> 状态:已批准,进入实施计划阶段
> 主题方向:数据可视化 + 故事性
> MVP 范围:12 星座 + 希腊/中国神话双卡 + Canvas 动画

## 1. 目标

为 CityGlow 项目新增"星图"页面 `/sky`,用户输入位置+日期+时间后,后端用天文算法算出当晚当地可见星空,前端 Canvas 绘制星点 + 动画连接 12 个主要星座,点击星座弹出希腊神话 + 中国古代星宿对照卡。

## 2. 数据源

### 2.1 星表
- **Bright Star Catalogue**(耶鲁亮星表)精简版
- 只取视星等 ≤ 5.0 的肉眼可见星(约 1600 颗)
- 字段:HIP 编号、视星等(mag)、赤经 RA(度)、赤纬 Dec(度)、英文名(可选)
- 打包为 `backend/src/main/resources/data/bright-stars.json`,约 50KB

### 2.2 星座连线
- 12 个 MVP 星座:猎户 Orion、大熊 Ursa Major、天蝎 Scorpius、仙后 Cassiopeia、北斗(北斗七星)、天琴 Lyra、天鹰 Aquila、天鹅 Cygnus、狮子 Leo、处女 Virgo、双子 Gemini、金牛 Taurus
- 每星座 5-15 条线段,连接 HIP 编号对
- 自建 `constellations.json`,字段:`{name, latin, chinese, stars: [HIP...], lines: [[HIP1, HIP2]...]}`

### 2.3 神话故事
- 12 星座 × 2 文化 = 24 篇故事卡,每篇 80-150 字
- 自建 `myths.json`,字段:`{constellation, culture: "greek"|"chinese", title, story}`

## 3. 算法

赤道坐标 → 地平坐标 → 立体投影:

1. 输入:用户经纬度 (lat, lng) + 日期时间 (UTC)
2. 计算当地恒星时(LST)
3. 对每颗星:赤道坐标 (RA, Dec) → 地平坐标 (方位角 Az, 高度 Alt)
4. 过滤:只保留 Alt > 0(地平线以上)的星
5. 投影:立体投影(stereographic)从天顶到 Canvas 平面

**决策**:算法放后端,与 ForecastService 架构一致,可缓存,前端 bundle 不增加。

## 4. 核心功能

### 4.1 用户流程
1. 进入 `/sky`(导航新增"星图"入口)
2. 默认显示**今晚当地可见星空**:自动取浏览器定位 + 当前时间
3. Canvas 渲染:深空背景 + 星点(大小按视星等) + 12 星座连线动画 + 星座名标签
4. 鼠标悬停星座 → 高亮该星座 + 其他降透明度 + 工具提示
5. 点击星座 → 神话卡弹窗(双栏:希腊 | 中国古代星宿)
6. 顶部控制条:日期选择器 + 时间滑块(0-24h) + 位置下拉(收藏点列表/手动输入)
7. 拖动时间滑块 → 星空实时旋转(预计算 24h 位置,平滑动画)

### 4.2 与现有功能联动
- 位置下拉自动列出已收藏观测点(FavoriteLocation)
- 神话卡底部"加入星图明信片"按钮 → 跳转 StarryPostcard 预填背景

## 5. 视觉设计

**美学方向**:17 世纪铜版画星图 × 东方水墨。借鉴 Andreas Cellarius《Harmonia Macrocosmica》+ 中国古代《步天歌》星图韵味。

### 5.1 色彩
- 主背景:深空黑 `#050811`(比项目主背景 `#0a0e1a` 更深,突出星图)
- 星点:暖白 `#fef3c7` → 冷白 `#e8eaf6`(按光谱类型,可选简化为统一色)
- 星座连线:暗金 `#c5a572` 60% 透明度,悬停时 100% + 辉光
- 星座名:手写感衬线体 Cormorant Garamond,暗金描边
- 神话卡背景:羊皮纸纹理 `rgba(245, 230, 200, 0.05)` 叠加噪点

### 5.2 字体
沿用项目已有三字体,不引入新字体:
- Cormorant Garamond(星座名/神话标题)
- Manrope(神话正文)
- JetBrains Mono(坐标/时间)

### 5.3 动效
- **页面加载**:Canvas 从中心向外辐射展开,星点逐颗淡入(50ms stagger)
- **星座连线**:每星座按线段顺序绘制,带辉光拖尾
- **时间滑块**:星空整体旋转,星点留短暂尾迹(长曝光感)
- **悬停**:星座辉光呼吸效果(2s loop)
- **神话卡弹窗**:从星座中心位置展开,scale + opacity 过渡

### 5.4 装饰元素
- Canvas 四角:铜版画风格装饰角花(SVG)
- 地平线:淡淡的金色环(像古代星盘)
- 方位标:Latin 简写(N=Septentrio, E=Oriens, S=Auster, W=Occidens)+ 中文小字

## 6. 技术架构

### 6.1 后端 API
- `GET /api/v1/sky/constellation-view?lat=&lng=&date=YYYY-MM-DD&hour=0-23`
  - 返回:可见星列表(HIP, mag, az, alt)+ 12 星座连线(每星座的 star HIP 列表 + 线段对)+ 当晚可见星座列表
  - 缓存:同 (lat, lng, date, hour) 缓存 1 小时(Caffeine)
- `GET /api/v1/sky/myths/{constellation}`
  - 返回该星座的希腊 + 中国神话故事卡
  - 缓存:静态数据,启动时加载到内存

### 6.2 文件结构

**后端**:
```
backend/src/main/java/com/cityglow/
├── controller/SkyViewController.java
├── service/
│   ├── StarProjectionService.java
│   └── ConstellationDataService.java
├── domain/
│   ├── StarPoint.java
│   ├── ConstellationView.java
│   ├── SkyViewResult.java
│   └── MythCard.java
└── resources/data/
    ├── bright-stars.json
    ├── constellations.json
    └── myths.json
```

**前端**:
```
frontend/src/
├── views/SkyAtlas.vue
├── components/
│   ├── StarCanvas.vue
│   ├── ConstellationMythCard.vue
│   └── SkyControlBar.vue
├── api/sky.ts
└── router/index.ts (新增 /sky 路由)
```

## 7. 实施分阶段

每阶段一次提交,TDD 风格:

1. **Phase 1 - 数据与算法**:星表 + 连线 + 投影算法 + 后端 API + 单元测试
2. **Phase 2 - 前端 MVP**:StarCanvas 基础渲染 + SkyAtlas 页面 + API 调用
3. **Phase 3 - 交互**:悬停高亮 + 神话卡弹窗 + 24 篇神话内容
4. **Phase 4 - 时间滑块**:24h 预计算 + 时间滑块动画 + 星空旋转
5. **Phase 5 - 视觉打磨**:加载动画 + 装饰元素 + 收藏点联动 + 明信片跳转

## 8. 不做的事(YAGNI)

- 不做全 88 星座(MVP 12 个足够)
- 不做南北半球切换(默认北半球,中国境内观测点)
- 不做行星/月球/太阳位置(只画恒星)
- 不做真实星名国际化(星座名双语即可,星名保留 HIP 编号)
- 不做深空天体(Messier 天体)— 留待后续
- 不做望远镜视场模拟
