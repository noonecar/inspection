# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

无线电频率使用与在用台站监督检查系统。前后端分离架构，用于管理无线电频率、台站核查、检查任务分配与执行、违规预警及统计报告。

## 环境要求

- JDK 17
- Node.js 24.x
- MySQL 8.0.36

## 常用命令

### 后端 (inspection-backend/)

```bash
cd inspection-backend
.\mvnw.cmd -q -DskipTests spring-boot:run    # 开发运行
.\mvnw.cmd -q compile -DskipTests             # 编译检查
.\mvnw.cmd clean package -DskipTests          # 打包
```

后端地址: `http://localhost:8080`
API 文档: `http://localhost:8080/swagger-ui.html`

### 前端 (inspection-web/)

```bash
cd inspection-web
npm install
npm run dev       # 开发服务器 http://localhost:5173
npm run build     # 生产构建
```

### 数据库

- 数据库名: `radio_analysis`（远程）或 `inspection`（本地）
- `application.yml` 中远程配置（47.109.84.104）当前生效，本地配置被注释
- 环境变量可覆盖: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- 注意：radio_station表中有两个 ID — id和 station_id，id是从1开始，station_id是从1001开始。系统当前关联台站均应采用用station_id字段，不再使用id来关联台站信息！

## 技术栈

| 层 | 技术 |
|---|------|
| 后端 | Spring Boot 3.2.5, MyBatis-Plus 3.5.6, Spring Security 6, JWT (jjwt 0.11.5), SpringDoc 2.5.0, fastjson2 2.0.32 |
| 前端 | Vue 3.4, Element Plus 2.7, Vite 5, Pinia 2, Vue Router 4, ECharts 5, Axios |
| 数据库 | MySQL 8.0 |

## 后端架构

### 包结构

```
com.inspection
├── common
│   ├── annotation/    — @OperationLog 注解
│   ├── aspect/        — OperationLogAspect（AOP 自动记录操作日志）
│   ├── config/        — Spring 配置（SecurityConfig, CorsConfig, WebMvcConfig, JacksonConfig, MybatisPlusConfig, MetaFillHandler）
│   ├── constant/      — AuthConstants（header/token/claim 常量）
│   ├── enums/         — TaskStatus 枚举
│   ├── exception/     — BusinessException, EntityNotFoundException, RegulationViolationException + GlobalExceptionHandler
│   ├── result/        — ApiResponse<T> 统一响应 {success, message, data, timestamp}
│   ├── security/      — JwtAuthenticationFilter, UserDetailsServiceImpl
│   └── utils/         — JwtUtil
├── controller/        — REST 控制器（使用 @RolesAllowed JSR-250 注解鉴权）
├── dto/               — StationDetailDTO（含嵌套 FrequencyUsage 列表）
├── entity/            — MyBatis-Plus 实体（继承 BaseEntity，自动填充 createdAt/updatedAt）
├── mapper/            — MyBatis-Plus Mapper 接口
└── service/           — 业务接口 + impl/ 实现
```

### API 响应格式

所有控制器返回 `ApiResponse<T>`，格式为 `{success, message, data, timestamp}`。前端 Axios 拦截器已解包 `res.data`，组件中直接拿到 data。

### 安全实现

- `SecurityConfig` 使用 `@EnableMethodSecurity(jsr250Enabled = true)`，配合 `@RolesAllowed({"ADMIN", "OPERATOR", "INSPECTOR"})` 注解在方法级别鉴权
- `JwtAuthenticationFilter`（OncePerRequestFilter）从 Authorization header 提取 Bearer token，解析 username 和 role 设置 SecurityContext
- 公开端点: `/api/auth/**`, `/uploads/**`, `/v3/api-docs/**`, `/swagger-ui/**`
- 密码编码: `PasswordEncoderFactories.createDelegatingPasswordEncoder()`

### 异常处理

`GlobalExceptionHandler`（@RestControllerAdvice）统一处理:
- `BusinessException` → 400 (带业务错误码)
- `MethodArgumentNotValidException` → 400 (拼接字段校验信息)
- `AuthenticationException` → 401
- `AccessDeniedException` → 403
- `NoResourceFoundException` → 404
- `HttpRequestMethodNotSupportedException` → 405
- 其他 Exception → 500

### 核心模块

- **认证**: `AuthController` + `JwtAuthenticationFilter` + `JwtUtil`，JWT token 认证
- **用户角色**: `ADMIN`, `OPERATOR`, `INSPECTOR` (见 `AuthConstants.ROLE_*`)
- **实体**: `SysUser`, `RadioStation`, `FrequencyUsage`, `InspectionTask`, `InspectionRecord`, `InspectionStandard`, `Regulation`, `RegulationClause`, `WarningRecord`, `Institution`, `StatisticsReport`, `OperationLog`, `TaskHistory`, `UserInstitution`, `WarningHistory`
- **操作日志**: `@OperationLog(module, operationType)` 注解 + `OperationLogAspect` AOP 自动记录到 `OperationLog` 表和 `operation_log` 表
- **文件上传**: `FileController` + `WebMvcConfig` 将 `uploads/` 目录映射为 `/uploads/**` 静态资源
- **智能法规助手**: `RegulationKnowledgeController` 提供法规知识问答接口

### 关键配置

- `application.yml` — 数据源（当前指向远程 47.109.84.104）、JWT（secret+24h过期）、CORS（含 cpolar 内网穿透通配符）、上传目录
- `SecurityConfig.java` — Spring Security 无状态 + JWT 过滤器链
- `CorsConfig.java` — 跨域配置
- `MetaFillHandler.java` — MyBatis-Plus 自动填充 createdAt/updatedAt

### 任务状态流转

`TaskStatus` 枚举定义: `PENDING_REVIEW("待审核")` → `IN_PROGRESS("进行中")` → `PENDING_REINSPECTION("待复检")` / `COMPLETED("已完成")` / `CANCELLED("已取消")`

- 新建任务 → `待审核`
- 审核通过 → `进行中`（审核驳回 → `已取消`）
- 录入检查记录: 全合格 → `已完成`；任一不合格 → `待复检`
- 复检: 合格 → `已完成`；仍不合格 → `待复检`

### 多台站任务

`InspectionTask` 实体有 `stationId`（单台站）和 `stationIds`（JSON 数组字符串，多台站）。
`InspectionRecord` 按 `(taskId, stationId)` 对保存，每台站独立记录。

**核心联动**: `InspectionRecordServiceImpl.afterRecordChanged()` 在记录创建/更新后汇总所有台站的最新检查记录判定任务状态——全合格才标为已完成并清除预警，任一不合格则标为待复检并生成预警。删除记录后调用 `recalculateTaskStatus()` 重新计算。

### 检查明细动态表

`InspectionDetailController` 处理 6 种检查类别的动态检查表。使用 `NamedParameterJdbcTemplate` 直接操作物理表。通过 `DetailMeta` 内部类定义每种类别的 `(tableName, columns, objectType, itemCode, itemName, checkType, checkMethod)`。列名约定 `check_*` + `check_*_result` 配对，`_result` 后缀列用作预警规则明细提取。

| 检查类别 | detailKey | 物理表 |
|---------|-----------|--------|
| 地面频率使用 | ground-frequency | ground_frequency_records |
| 卫星频率使用 | satellite-frequency | satellite_frequency_records |
| 卫星通信网频率 | satellite-network | satellite_network_records |
| 地面台站 | ground-station | ground_station_records |
| 空间电台 | space-station | space_station_records |
| 卫星地球站 | satellite-earth-station | satellite_earth_station_records |

### 预警历史快照

`WarningRecord` 每次变更（创建/更新/处理）时在 `WarningHistory` 表中保存快照。预警按 `(ruleName, relatedTaskId)` 去重。历史查询按 `taskId` 返回全量变更记录。

### 自动预警规则

检查记录中的布尔标志触发自动预警:
- `harmfulInterference=true` → 高级别
- `result=不合格 && needsRectification=true` → 高级别
- `annualReportSubmitted=false` → 中级别
- `spectrumFeePaid=false` → 中级别
- `technicalPersonnelOk=false` → 中级别
- `increasedFrequencyRequired=true` → 低级别

### 数据库

- `schema.sql` — 所有表的 DDL（6 张动态检查明细表 + 业务表 + 预警历史表）
- `data.sql` — 初始化数据（默认用户、台站、法规等）
- `docs/台站核查.md` — 法规政策文档（业务规则参考）

## 前端架构

### API 模块模式

`src/api/` 下每个文件对应一个后端 Controller，导出函数使用 Axios 实例:
```js
import request from '../utils/request'
export const fetchTasks = (params) => request.get('/api/tasks', { params })
export const createTask = (data) => request.post('/api/tasks', data)
```

### 请求拦截器

`src/utils/request.js`:
- 自动注入 `Bearer token` 到 Authorization header
- POST/PUT/PATCH 请求自动对 payload 做 `normalizePayload` 处理: 将 `Id/Date/At/Until/Deadline` 后缀的空字符串转为 null
- 响应拦截器自动解包 `res.data`，401 时自动退出登录
- `VITE_API_BASE_URL` 环境变量可配置基础路径

### 认证状态

`src/stores/auth.js`（Pinia）: token 和 user 信息持久化到 localStorage。auth store 通过 `setAuth()` / `logout()` 管理登录状态。

### 路由守卫

`src/router/index.js`: `beforeEach` 守卫检查 token 是否存在，以及角色是否有权限访问路由的 `meta.roles`。未登录重定向到 `/login`，无权限重定向到 `/dashboard`。

### 检查模板

`src/constants/inspectionTemplates.js` 定义全部 6 种检查类别的检查项模板（`frequencyTemplates` + `stationTemplates`），每个模板包含 `value`（对应后端 column key）、`itemName`、`checkType`、`checkMethod`、`legalClause`。

### 页面路由

| 路由 | 页面 | 权限 |
|---|---|---|
| `/dashboard` | DashboardView | 全部 |
| `/task` | TaskView | 全部 |
| `/inspection` | InspectionView | 全部 |
| `/data` | DataView | 全部 |
| `/report` | ReportView | 全部 |
| `/regulation` | RegulationView | 全部 |
| `/warning` | WarningView | 全部 |
| `/operation-log` | OperationLogView | ADMIN, OPERATOR |
| `/institution` | InstitutionView | ADMIN |

### InspectionView 多台站处理

多台站任务在检查录入弹窗中显示卡片式 Tab，每个台站独立表单。使用 `multiStationCache` 缓存各台站填写数据，切换 Tab 时自动保存/恢复。保存时遍历所有台站逐条提交到 `POST /api/inspection-details/{category}`。

## 默认账号

- admin / 123456 (管理员)