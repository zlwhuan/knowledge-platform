# Nexus 知识管理平台 - 项目结构与功能地图

> 长期参考文件，记录前后端代码结构、功能模块对应的文件位置、API 接口、数据模型等。
> 最后更新：2026-06-07

---

## 一、技术栈概览

| 层 | 技术 |
|---|------|
| **后端** | Java 17, Spring Boot 4.0.4, Spring Data JPA, MySQL, Lombok, Apache POI, SpringDoc OpenAPI (Swagger) |
| **前端** | Vue 3 (Composition API `<script setup>`), Element Plus, Vue Router, Axios, ECharts, md-editor-v3, Vite 8 |
| **认证** | 自实现 Token 认证（UUID token 存 ConcurrentHashMap，BCrypt 密码加密） |
| **文件存储** | 本地磁盘 `uploads/` 目录 |
| **文档预览** | LibreOffice headless → OnlyOffice → Apache POI 文本提取（多级降级） |

---

## 二、目录结构总览

```
Nexus/
├── backend/                          # Spring Boot 后端
│   ├── pom.xml                       # Maven 依赖配置
│   ├── src/main/java/com/company/knowledge/
│   │   ├── KnowledgePlatformApplication.java  # 启动类
│   │   ├── config/                   # 配置类
│   │   ├── controller/               # REST 控制器 (16个)
│   │   ├── dto/                      # 数据传输对象 (39个)
│   │   ├── entity/                   # JPA 实体 (19个)
│   │   ├── exception/                # 自定义异常 (3个)
│   │   ├── repository/               # JPA 仓库 (18个)
│   │   └── service/                  # 业务服务 (12个，含 impl/)
│   └── src/main/resources/
│       └── application.properties    # 应用配置
├── frontend/                         # Vue 3 前端
│   ├── package.json                  # npm 依赖
│   ├── vite.config.js                # Vite 构建配置
│   ├── index.html                    # 入口 HTML
│   └── src/
│       ├── main.js                   # 应用入口
│       ├── App.vue                   # 根组件（登录门控 + 主布局壳）
│       ├── router/index.js           # 路由定义 (17条)
│       ├── services/api.js           # Axios 实例 + 拦截器
│       ├── stores/                   # 状态管理 (2个)
│       ├── composables/              # 组合式函数 (14个)
│       ├── components/               # 视图组件 (27个)
│       ├── constants/                # 静态常量 (1个)
│       └── assets/                   # 静态资源
├── start-*.bat / *.ps1               # 启动脚本
├── uploads/                          # 上传文件存储
└── previews/                         # 预览缓存文件
```

---

## 三、功能模块 → 文件映射

### 3.1 认证与用户管理

**功能**：登录、Token 认证、用户 CRUD、角色权限管理

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/AuthController.java` | POST `/api/auth/login`, GET `/api/auth/me` |
| 后端-Controller | `controller/UserController.java` | CRUD `/api/users` (仅 ADMIN) |
| 后端-Controller | `controller/RolePermissionController.java` | GET/PUT `/api/roles` 角色权限配置 |
| 后端-Service | `service/AuthService.java` + `service/impl/AuthServiceImpl.java` | 登录验证、BCrypt 密码、Token 会话管理、用户/角色 CRUD |
| 后端-Entity | `entity/UserAccount.java` | 用户账号表 `kp_user_account` |
| 后端-Entity | `entity/RolePermissionConfig.java` | 角色权限配置表 `kp_role_permission` |
| 后端-Entity | `entity/RoleType.java` | 角色枚举: ADMIN, SALES, PRESALES, DELIVERY_OPS, FINANCE, USER, REVIEWER |
| 后端-Repository | `repository/UserAccountRepository.java` | findByUsername, existsByUsername |
| 后端-Repository | `repository/RolePermissionConfigRepository.java` | findByRole |
| 后端-DTO | `dto/LoginRequest.java`, `dto/LoginResponse.java` | 登录请求/响应 |
| 后端-DTO | `dto/UserRequest.java`, `dto/UserResponse.java` | 用户 CRUD 请求/响应 |
| 后端-DTO | `dto/RolePermissionRequest.java`, `dto/RolePermissionResponse.java` | 角色权限请求/响应 |
| 前端-组件 | `components/LoginView.vue` | 登录页面（左右分栏布局） |
| 前端-组件 | `components/UserManagementView.vue` | 用户管理页面 |
| 前端-组件 | `components/RoleManagementView.vue` | 角色权限配置页面 |
| 前端-Store | `stores/auth.js` | Token/User 持久化 (localStorage) |
| 前端-Composable | `composables/useSystemManagement.js` | 用户/角色/分类/系统概览的 CRUD 逻辑 |

---

### 3.2 知识库管理

**功能**：知识条目 CRUD、分类管理、版本历史、批量编辑、Excel 导入导出、Markdown 编辑

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/KnowledgeItemController.java` | CRUD `/api/items`, 批量删除, Excel 导入, 版本历史, 仪表盘统计 |
| 后端-Controller | `controller/CategoryController.java` | CRUD `/api/categories` (仅 ADMIN) |
| 后端-Controller | `controller/SearchController.java` | GET `/api/search` 全局搜索 (知识条目+项目+客户) |
| 后端-Controller | `controller/ExportController.java` | GET `/api/export/projects` 项目导出 Excel |
| 后端-Service | `service/KnowledgeItemService.java` + `impl/KnowledgeItemServiceImpl.java` | 条目 CRUD、Specification 动态查询、版本快照、Excel 导入、治理状态计算 |
| 后端-Service | `service/CategoryService.java` + `impl/CategoryServiceImpl.java` | 分类树构建、父子关系校验 |
| 后端-Entity | `entity/KnowledgeItem.java` | 知识条目表 `kp_knowledge_item`，关联 Category 和 Project |
| 后端-Entity | `entity/KnowledgeItemVersion.java` | 版本历史表 `kp_knowledge_item_version` |
| 后端-Entity | `entity/Category.java` | 分类表 `kp_category`，自引用父子关系 |
| 后端-Repository | `repository/KnowledgeItemRepository.java` | 含自定义统计查询 (无附件/无摘要/有内容/近期更新) |
| 后端-Repository | `repository/KnowledgeItemVersionRepository.java` | 按条目查版本、按条目删除版本 |
| 后端-Repository | `repository/CategoryRepository.java` | 排序查询、存在子分类检查 |
| 后端-DTO | `dto/KnowledgeItemRequest.java`, `dto/KnowledgeItemResponse.java` | 条目请求/响应 |
| 后端-DTO | `dto/KnowledgeItemVersionResponse.java` | 版本历史响应 |
| 后端-DTO | `dto/CategoryRequest.java`, `dto/CategoryResponse.java` | 分类请求/响应 |
| 后端-DTO | `dto/BulkIdsRequest.java`, `dto/BulkStatusUpdateRequest.java` | 批量操作请求 |
| 后端-DTO | `dto/DashboardStatsResponse.java` | 仪表盘统计响应 |
| 前端-组件 | `components/KnowledgeLibraryPage.vue` | 知识库列表页（筛选+表格+分页+批量操作） |
| 前端-组件 | `components/KnowledgeLibraryView.vue` | 知识库旧版视图（含治理状态标签） |
| 前端-组件 | `components/LibraryComposeView.vue` | 知识条目编辑/创建页（Markdown 编辑器+附件拖拽上传） |
| 前端-组件 | `components/LibraryDetailDialog.vue` | 知识条目详情弹窗（只读+附件管理） |
| 前端-组件 | `components/LibraryBatchEditDialog.vue` | 批量编辑弹窗（分类/来源/标签） |
| 前端-组件 | `components/CategoryManagementView.vue` | 分类管理页面 |
| 前端-Composable | `composables/useKnowledgeLibrary.js` | 知识库完整 CRUD 逻辑（条目+分类+附件+分页+批量编辑） |
| 前端-Composable | `composables/useLibraryActions.js` | 知识库高级操作桥接（详情/编辑/创建关联条目） |

---

### 3.3 附件管理与文档预览

**功能**：文件上传/下载、多格式文档预览（Office/PDF/图片/视频/Markdown/文本）

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/AttachmentController.java` | 上传/下载/删除/列表 `/api/attachments` |
| 后端-Controller | `controller/PreviewController.java` | 预览元数据+文件流 `/api/attachments/{id}/preview` |
| 后端-Service | `service/AttachmentService.java` + `impl/AttachmentServiceImpl.java` | 文件存储（UUID 前缀）、元数据管理、异步预览触发 |
| 后端-Service | `service/PreviewService.java` + `impl/PreviewServiceImpl.java` | 多格式预览生成（LibreOffice→OnlyOffice→POI 降级策略） |
| 后端-Service | `service/PreviewWarmupService.java` + `impl/PreviewWarmupServiceImpl.java` | 异步预览预热（上传后后台生成） |
| 后端-Service | `service/PreviewCleanupService.java` + `impl/PreviewCleanupServiceImpl.java` | 定时清理过期预览缓存（每天凌晨3点，30天过期） |
| 后端-Entity | `entity/Attachment.java` | 附件表 `kp_attachment`，关联 KnowledgeItem |
| 后端-Repository | `repository/AttachmentRepository.java` | 按条目查附件、自定义 JOIN FETCH 查询 |
| 后端-DTO | `dto/AttachmentResponse.java`, `dto/AttachmentDetailResponse.java` | 附件响应 |
| 后端-DTO | `dto/PreviewMetaResponse.java` | 预览元数据响应 |
| 后端-Config | `config/AsyncConfig.java` | 异步线程池配置（previewWarmupExecutor） |
| 前端-组件 | `components/AttachmentManagement.vue` | 全局附件浏览管理页面 |
| 前端-Composable | `composables/useAttachmentPreview.js` | 附件预览逻辑（PDF/Office/图片/视频/Markdown/OnlyOffice 集成） |

---

### 3.4 项目跟踪管理

**功能**：项目 CRUD、进度记录、活动日志、甘特图、仪表盘、多角色视角

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/ProjectController.java` | 项目 CRUD + 进度记录 + 活动日志 `/api/projects` |
| 后端-Service | `service/ProjectService.java` + `impl/ProjectServiceImpl.java` | 项目生命周期管理、仪表盘统计、进度自动同步项目状态 |
| 后端-Entity | `entity/Project.java` | 项目表 `kp_project`（含多角色 owner、合同金额、联系人链接等） |
| 后端-Entity | `entity/ProjectProgressRecord.java` | 进度记录表 `kp_project_progress_record` |
| 后端-Entity | `entity/ProjectActivity.java` | 活动日志表 `kp_project_activity` |
| 后端-Repository | `repository/ProjectRepository.java` | JpaSpecificationExecutor 动态查询 |
| 后端-Repository | `repository/ProjectProgressRecordRepository.java` | 最新进度记录查询 |
| 后端-Repository | `repository/ProjectActivityRepository.java` | 按项目查活动 |
| 后端-DTO | `dto/ProjectRequest.java`, `dto/ProjectResponse.java` | 项目请求/响应 |
| 后端-DTO | `dto/ProjectProgressRecordRequest.java`, `dto/ProjectProgressRecordResponse.java` | 进度记录 |
| 后端-DTO | `dto/ProjectActivityRequest.java`, `dto/ProjectActivityResponse.java` | 活动日志 |
| 后端-DTO | `dto/ProjectDashboardResponse.java` | 项目仪表盘响应 |
| 前端-组件 | `components/ProjectTrackerView.vue` | 项目跟踪主视图（组合子组件） |
| 前端-组件 | `components/ProjectTrackerTable.vue` | 项目列表（表格/卡片双视图+筛选） |
| 前端-组件 | `components/ProjectTrackerOverviewChart.vue` | 项目进度 SVG 折线图 |
| 前端-组件 | `components/ProjectTrackerFocusBar.vue` | 角色敏感 KPI 卡片 |
| 前端-组件 | `components/ProjectSummaryPanel.vue` | 项目详情面板（待办+进度时间线） |
| 前端-组件 | `components/ProjectGanttView.vue` | ECharts 甘特图视图 |
| 前端-组件 | `components/ProjectDialogs.vue` | 项目表单+进度弹窗（6个分区+联系人链接编辑器） |
| 前端-Composable | `composables/useProjectTracker.js` | 项目跟踪核心逻辑（CRUD+筛选+仪表盘+子视图切换+待办计算） |
| 前端-Composable | `composables/useProjectTrackerView.js` | 事件转发包装器 |
| 前端-Composable | `composables/useProjectTrackerTable.js` | 表格工具函数（到期状态计算） |
| 前端-Composable | `composables/useProjectTrackerBoard.js` | 仪表盘/看板计算属性 |
| 前端-Composable | `composables/projectDialogOptions.js` | 项目表单选项/字段元数据 |
| 前端-Composable | `composables/projectFormState.js` | 项目/活动/进度表单工厂函数+映射器 |

---

### 3.5 客户关系管理 (CRM)

**功能**：客户公司 CRUD、联系人管理、跟进记录、健康度评估

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/CustomerController.java` | 客户+联系人+跟进记录 CRUD `/api/customers` |
| 后端-Service | `service/CustomerService.java` + `impl/CustomerServiceImpl.java` | 客户管理、级联删除、健康度自动评估、下次跟进日期计算 |
| 后端-Entity | `entity/CustomerCompany.java` | 客户公司表 `kp_customer_company` |
| 后端-Entity | `entity/CustomerContact.java` | 联系人表 `kp_customer_contact` |
| 后端-Entity | `entity/CustomerFollowup.java` | 跟进记录表 `kp_customer_followup` |
| 后端-Repository | `repository/CustomerCompanyRepository.java` | 按更新时间排序 |
| 后端-Repository | `repository/CustomerContactRepository.java` | 按主联系人排序、级联删除 |
| 后端-Repository | `repository/CustomerFollowupRepository.java` | 按跟进时间排序、级联删除 |
| 后端-DTO | `dto/CustomerCompanyRequest.java`, `dto/CustomerCompanyResponse.java` | 客户请求/响应 |
| 后端-DTO | `dto/CustomerContactRequest.java`, `dto/CustomerContactResponse.java` | 联系人请求/响应 |
| 后端-DTO | `dto/CustomerFollowupRequest.java`, `dto/CustomerFollowupResponse.java` | 跟进记录请求/响应 |
| 前端-组件 | `components/CustomerManagementView.vue` | CRM 主页面（客户列表+联系人表+跟进时间线+关联项目） |
| 前端-Composable | `composables/useCustomerManagement.js` | 客户完整 CRUD 逻辑（含表单校验：手机/邮箱正则） |
| 前端-Constants | `constants/customerOptions.js` | 客户相关下拉选项（状态/等级/地区/行业/类型/来源/阶段/决策级别/性别/跟进类型/跟进结果） |

---

### 3.6 培训与考核管理

**功能**：培训记录 CRUD、考核记录 CRUD、考核关联培训

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/TrainingController.java` | 培训记录 CRUD `/api/training-records` |
| 后端-Controller | `controller/AssessmentController.java` | 考核记录 CRUD `/api/assessment-records` |
| 后端-Service | `service/TrainingService.java` + `impl/TrainingServiceImpl.java` | 培训记录管理 |
| 后端-Service | `service/AssessmentService.java` + `impl/AssessmentServiceImpl.java` | 考核记录管理、关联培训标题查询 |
| 后端-Entity | `entity/TrainingRecord.java` | 培训记录表 `kp_training_record` |
| 后端-Entity | `entity/AssessmentRecord.java` | 考核记录表 `kp_assessment_record` |
| 后端-Repository | `repository/TrainingRecordRepository.java` | 按标题/类型查询 |
| 后端-Repository | `repository/AssessmentRecordRepository.java` | 按标题/类型/等级/培训ID查询 |
| 后端-DTO | `dto/TrainingRecordRequest.java`, `dto/TrainingRecordResponse.java` | 培训请求/响应 |
| 后端-DTO | `dto/AssessmentRecordRequest.java`, `dto/AssessmentRecordResponse.java` | 考核请求/响应 |
| 前端-组件 | `components/TrainingManagement.vue` | 培训管理页面（自包含 CRUD） |
| 前端-组件 | `components/AssessmentManagement.vue` | 考核管理页面（自包含 CRUD，关联培训） |

---

### 3.7 数据字典管理

**功能**：系统下拉选项统一管理（项目阶段/状态/客户类型等 19 种字典类型）

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/DictionaryController.java` | 字典 CRUD `/api/dictionaries` |
| 后端-Service | `service/DictionaryService.java` + `impl/DictionaryServiceImpl.java` | 字典管理、分组查询、去重校验 |
| 后端-Entity | `entity/DictionaryItem.java` | 字典项表 `kp_dictionary_item` |
| 后端-Repository | `repository/DictionaryItemRepository.java` | 按类型查询、去重检查 |
| 后端-DTO | `dto/DictionaryItemRequest.java`, `dto/DictionaryItemResponse.java` | 字典项请求/响应 |
| 后端-DTO | `dto/DictionaryGroupResponse.java` | 字典分组响应 |
| 前端-组件 | `components/DictionaryManagementView.vue` | 字典管理页面（三栏布局：类型选择+编辑表单+项目列表） |
| 前端-Composable | `composables/useDictionaries.js` | 字典加载/缓存/编辑逻辑，含 `dictionaryTypeMeta`（19种字典类型定义） |
| 前端-Store | `stores/dictionaries.js` | 字典内存缓存 |

---

### 3.8 首页仪表盘

**功能**：老板驾驶舱、KPI 概览、财务收款率、客户跟进预警、项目执行雷达

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-DTO | `dto/DashboardStatsResponse.java` | 仪表盘统计响应 |
| 后端-DTO | `dto/SystemOverviewResponse.java` | 系统概览响应 |
| 后端-Controller | `controller/SystemController.java` | GET `/api/system/overview` |
| 前端-组件 | `components/HomeDashboard.vue` | 首页仪表盘（KPI 卡片+财务条+客户预警+项目雷达+饼图） |
| 前端-Composable | `composables/useHomeDashboard.js` | 仪表盘计算属性聚合（项目摘要/里程碑/进度记录/焦点项目/行动队列/客户跟进预警） |

---

### 3.9 系统配置与概览

**功能**：系统信息展示、预览配置查看

| 层 | 文件 | 说明 |
|----|------|------|
| 前端-组件 | `components/SystemOverviewView.vue` | 系统概览（信息+维护建议） |
| 前端-组件 | `components/PreviewSettingsView.vue` | 预览配置展示（LibreOffice/OnlyOffice 状态） |
| 前端-组件 | `components/SystemConfigView.vue` | 系统配置占位页 |
| 前端-Composable | `composables/useSystemManagement.js` | 系统概览/仪表盘数据加载 |

---

### 3.10 全局搜索

| 层 | 文件 | 说明 |
|----|------|------|
| 后端-Controller | `controller/SearchController.java` | GET `/api/search?q=xxx` 跨知识条目+项目+客户搜索 |
| 前端 | `App.vue` 中集成 | 全局搜索弹窗（在根组件中） |

---

## 四、横切关注点

### 4.1 认证与权限
- **认证机制**：自实现 UUID Token，存内存 ConcurrentHashMap，前端 localStorage 持久化
- **权限控制**：Controller 层通过 `@RequestParam("X-Auth-Token")` + `authService.requireAnyRole()` 校验
- **角色类型**：ADMIN, SALES, PRESALES, DELIVERY_OPS, FINANCE, USER, REVIEWER
- **权限配置**：`RolePermissionConfig` 实体存储每个角色的 8 个布尔权限开关
- **前端守卫**：`services/api.js` 的响应拦截器检测 401/403 自动跳转登录

### 4.2 异常处理
- `exception/UnauthorizedException.java` → 401
- `exception/ForbiddenException.java` → 403
- `exception/ResourceNotFoundException.java` → 404
- `controller/GlobalExceptionHandler.java` → `@RestControllerAdvice` 统一异常处理

### 4.3 CORS 配置
- `config/WebConfig.java` → `/api/**` 路径的 CORS 配置，允许来源从 `cors.allowed-origins` 属性读取

### 4.4 异步与定时任务
- `config/AsyncConfig.java` → `@EnableAsync` + `@EnableScheduling`，预览预热线程池 (core=1, max=2, queue=200)
- `PreviewCleanupServiceImpl` → 每天凌晨 3 点清理 30 天以上预览缓存

### 4.5 数据初始化
- `config/DataInitializer.java` → `CommandLineRunner`，当前已禁用（空实现）

---

## 五、前端架构说明

### 5.1 路由与视图切换
- `router/index.js` 定义 17 条路由，但路由组件都是空 stub
- **实际视图切换**通过 `App.vue` 中的 `currentView` ref + `AppMainContent.vue` 的条件渲染实现
- `useShellNavigation.js` 管理视图切换 + 路由同步

### 5.2 数据流模式
```
App.vue (状态持有者)
  ├── composables/ (业务逻辑 + API 调用)
  ├── AppSidebar.vue (导航)
  └── AppMainContent.vue (视图分发器)
       ├── HomeDashboard.vue
       ├── KnowledgeLibraryPage.vue
       ├── LibraryComposeView.vue
       ├── ProjectTrackerView.vue
       │    ├── ProjectTrackerFocusBar.vue
       │    ├── ProjectTrackerTable.vue
       │    ├── ProjectTrackerOverviewChart.vue
       │    ├── ProjectSummaryPanel.vue
       │    └── ProjectDialogs.vue
       ├── CustomerManagementView.vue
       ├── TrainingManagement.vue
       ├── AssessmentManagement.vue
       └── ...其他管理视图
```

### 5.3 Composable 职责划分

| Composable | 职责 |
|------------|------|
| `useKnowledgeLibrary.js` | 知识条目+分类+附件的完整 CRUD |
| `useLibraryActions.js` | 知识库与其他视图的桥接操作 |
| `useProjectTracker.js` | 项目跟踪核心逻辑（CRUD+筛选+仪表盘+子视图） |
| `useProjectTrackerView.js` | 项目视图事件转发 |
| `useProjectTrackerTable.js` | 项目表格工具函数 |
| `useProjectTrackerBoard.js` | 项目仪表盘计算属性 |
| `useCustomerManagement.js` | 客户 CRM 完整 CRUD |
| `useSystemManagement.js` | 用户/角色/分类/系统概览管理 |
| `useDictionaries.js` | 字典数据加载/缓存/编辑 |
| `useHomeDashboard.js` | 首页仪表盘数据聚合 |
| `useAttachmentPreview.js` | 附件预览（多格式+OnlyOffice） |
| `useShellNavigation.js` | 侧边栏导航状态管理 |
| `projectFormState.js` | 项目表单工厂函数 |
| `projectDialogOptions.js` | 项目表单选项常量 |

---

## 六、数据库表映射

| 表名 | 实体 | 说明 |
|------|------|------|
| `kp_user_account` | UserAccount | 用户账号 |
| `kp_role_permission` | RolePermissionConfig | 角色权限配置 |
| `kp_knowledge_item` | KnowledgeItem | 知识条目 |
| `kp_knowledge_item_version` | KnowledgeItemVersion | 知识条目版本历史 |
| `kp_category` | Category | 知识分类（自引用树） |
| `kp_attachment` | Attachment | 附件 |
| `kp_project` | Project | 项目 |
| `kp_project_progress_record` | ProjectProgressRecord | 项目进度记录 |
| `kp_project_activity` | ProjectActivity | 项目活动日志 |
| `kp_customer_company` | CustomerCompany | 客户公司 |
| `kp_customer_contact` | CustomerContact | 客户联系人 |
| `kp_customer_followup` | CustomerFollowup | 客户跟进记录 |
| `kp_dictionary_item` | DictionaryItem | 数据字典项 |
| `kp_training_record` | TrainingRecord | 培训记录 |
| `kp_assessment_record` | AssessmentRecord | 考核记录 |
| `exam_paper` | ExamPaper | 试卷（预留） |
| `exam_question` | ExamQuestion | 考题（预留） |
| `exam_record` | ExamRecord | 考试记录（预留） |

---

## 七、API 端点速查

| 模块 | 基础路径 | 端点数 |
|------|----------|--------|
| 认证 | `/api/auth` | 2 |
| 用户 | `/api/users` | 4 |
| 角色权限 | `/api/roles` | 3 |
| 知识条目 | `/api/items` | 9 |
| 分类 | `/api/categories` | 4 |
| 附件 | `/api/attachments` | 5 |
| 预览 | `/api/attachments/{id}/preview` | 2 |
| 项目 | `/api/projects` | 12 |
| 客户 | `/api/customers` | 12 |
| 字典 | `/api/dictionaries` | 5 |
| 培训 | `/api/training-records` | 5 |
| 考核 | `/api/assessment-records` | 6 |
| 搜索 | `/api/search` | 1 |
| 系统 | `/api/system` | 1 |
| 导出 | `/api/export` | 1 |
| **合计** | | **~63** |

---

## 八、启动方式

- **后端**：`start-backend.bat` 或 `Start-Backend.ps1`（Spring Boot，端口 8080）
- **前端**：`start-frontend.bat` 或 `Start-Frontend.ps1`（Vite dev server，端口 5173）
- **一键启动**：`Start-KnowledgePlatform.ps1` 或 `start-menu.bat`
- **数据库**：MySQL，库名 `knowledge_platform`，默认 root/Admin@123
- **Swagger UI**：`http://localhost:8080/swagger-ui.html`

---

## 九、关键业务逻辑备注

1. **知识条目版本快照**：每次更新条目时自动创建 `KnowledgeItemVersion` 记录
2. **项目进度自动同步**：添加/更新进度记录后，自动将最新 stage/status/progress/riskLevel 同步回 Project 实体
3. **客户健康度评估**：根据最近跟进结果自动计算（高→健康，中→一般，其他→关注）
4. **文档预览降级策略**：LibreOffice → OnlyOffice → POI 文本提取 → 原始文件
5. **密码迁移**：登录时自动将旧 SHA-256 哈希迁移到 BCrypt
6. **治理状态**：知识条目根据是否有摘要+附件显示"完整"/"缺摘要"/"缺附件"/"缺摘要/附件"
7. **项目到期计算**：根据进度记录的 nextActionDueDate 计算 overdue/dueSoon/normal 状态
8. **联系人链接**：Project 实体通过 JSON 字段 `projectContactLinks` 关联多个客户联系人
9. **表单校验**：项目 DTO 使用 `@Pattern` 校验 stage/status/riskLevel 合法值，客户 DTO 使用 `@Email`/`@Pattern` 校验格式
10. **客户分页**：`GET /api/customers/paged` 支持后端真分页 + Specification 动态筛选

---

## 十、变更记录

### 2026-06-07 功能完善

**后端改动：**
- `CustomerCompanyRepository` 新增 `JpaSpecificationExecutor` 支持动态查询
- `CustomerServiceImpl` 新增 `listPaged()` 方法，使用 Specification 实现数据库级筛选+分页
- `CustomerController` 新增 `GET /api/customers/paged` 分页接口
- `ApiResponse` 新增分页字段（total, page, size, pages）和 `paged()` 工厂方法
- `CustomerCompanyRequest` 新增 `@Email`/`@Pattern`/`@Size` 校验注解
- `CustomerContactRequest` 新增 `@Email`/`@Pattern`/`@Size` 校验注解
- `ProjectRequest` 新增 `@Pattern` 校验 stage/status/riskLevel 合法值，`@DecimalMin` 校验金额
- `ProjectProgressRecordRequest` 新增 `@Pattern` 校验 stage/status/riskLevel
- `ProjectServiceImpl.apply()` 新增交叉校验（回款≤合同、开始≤结束）

**前端改动：**
- `useCustomerManagement.js` 改为调用分页 API，新增分页状态和 `changeCustomerPage`/`changeCustomerSize`
- `CustomerManagementView.vue` 新增分页组件、空状态、网站/QQ/跟进时间字段、修复按钮文案、补 `CUSTOMER_TYPE_OPTIONS` import
- `projectFormState.js` 修复默认状态值与选项列表不匹配（待签约→未签约等）、补充 `nextAction` 默认值
- `ProjectDialogs.vue` 删除未定义的 `@change` 事件处理器（`onProgressStatusChange`/`onProgressValueChange`）
- `KnowledgeLibraryPage.vue` 新增空状态提示、导出 Excel 按钮
- `App.vue` 新增 `exportItems()` CSV 导出函数
- `LibraryComposeView.vue` 新增未保存更改追踪（dirty 状态 + beforeunload 拦截 + 离开确认）
- `TrainingManagement.vue` auth prop 改为可选、新增空状态、新增 trainingType 必填校验
- `AssessmentManagement.vue` auth prop 改为可选、新增空状态、新增 assessmentType/grade 必填校验
- `AppMainContent.vue` 新增 `customer-page-change`/`customer-size-change`/`export-items` 事件转发
