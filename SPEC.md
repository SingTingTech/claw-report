# ReportHub - 通用报表系统

## 1. 项目概述

**项目名称：** ReportHub  
**项目类型：** 通用报表平台（类帆软 / Metabase）  
**核心功能：** 支持多数据源对接、自定义 SQL 查询、可视化报表、用户权限管理、数据脱敏的完整报表系统  
**目标用户：** 企业内部数据分析人员、管理员、普通业务人员

---

## 2. 技术架构

### 2.1 技术栈
- **后端：** Java 17 + SpringBoot 3.x + MyBatis-Plus
- **前端：** Vue 3 + Element Plus + Vite
- **内置数据库：** MySQL 8.0（Docker 部署，监听 127.0.0.1）
- **构建工具：** Maven + Docker

### 2.2 项目结构
```
report-hub/
├── backend/               # SpringBoot 后端
│   ├── src/main/java/com/clawreport/
│   │   ├── controller/    # REST API
│   │   ├── service/      # 业务逻辑
│   │   ├── mapper/        # 数据访问
│   │   ├── entity/        # 数据实体
│   │   ├── datasource/    # 数据源适配器（核心扩展点）
│   │   ├── security/     # 权限认证
│   │   ├── mask/          # 数据脱敏
│   │   └── config/        # 配置类
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/init.sql
│   └── Dockerfile
├── frontend/              # Vue3 前端
│   ├── src/
│   │   ├── views/         # 页面
│   │   ├── stores/        # Pinia 状态
│   │   └── api/           # API 调用
│   └── Dockerfile
├── docker-compose.yml     # 编排文件
└── SPEC.md
```

---

## 3. 功能模块

### 3.1 数据源管理
- 支持新增 / 编辑 / 删除数据源
- 支持数据类型：MySQL、PostgreSQL（预留接口，可扩展）
- 数据源配置：名称、描述、JDBC URL、用户名、密码（密码加密存储）
- 连接测试
- **适配器接口：**
  ```java
  public interface DataSourceAdapter {
      String getType();
      Connection getConnection(DataSourceConfig config);
      List<String> getDatabases(DataSourceConfig config);
      List<TableMeta> getTables(DataSourceConfig config, String database);
      List<ColumnMeta> getColumns(DataSourceConfig config, String database, String table);
      PageResult executeQuery(String sql, Map<String, Object> params, int page, int pageSize);
      void testConnection(DataSourceConfig config);
  }
  ```

### 3.2 SQL 查询工作台
- 在线 SQL 编辑器（支持语法高亮）
- 自定义参数 / 查询条件（`${param}` 占位符）
- 分页查询（自动 count + 数据）
- 查询结果在线预览（表格展示）
- 查询历史记录

### 3.3 报表管理
- 创建 / 编辑 / 删除报表
- 报表关联数据源 + SQL 查询
- 参数配置（定义查询参数）
- **报表分组管理**（树形组织架构）

### 3.4 数据脱敏
- **脱敏规则配置：指定「哪个数据源 + 哪个表 + 哪个字段」需要脱敏**
- 脱敏方式：**字段值完全遮蔽为 `****`（全遮蔽，不保留部分信息）**
- 内置支持：手机号、身份证、邮箱、金额、银行账号等（全遮蔽）
- 自定义正则脱敏（全遮蔽）
- 脱敏规则与数据源绑定，查询时自动应用

### 3.5 用户与权限体系

#### 3.5.1 权限类型（两种）
| 权限类型 | 说明 |
|---------|------|
| **菜单权限** | 控制用户可访问哪些功能菜单/页面（如"用户管理"、"数据源管理"菜单是否显示） |
| **数据权限** | 控制用户可访问哪些数据范围（如哪些数据源、哪些报表分组可见） |

#### 3.5.2 权限分配方式
- **方式一：直接分配** → 直接将权限分配给用户
- **方式二：角色分配** → 通过角色批量分配权限（用户属于角色，角色拥有权限）
- 两种方式可叠加，最终权限 = 用户直接权限 ∪ 用户角色权限

#### 3.5.3 预置角色
| 角色代码 | 角色名称 | 菜单权限 | 数据权限 |
|---------|---------|---------|---------|
| ADMIN | 系统管理员 | 全部 | 全部 |
| DATASOURCE_ADMIN | 数据源管理员 | 数据源管理、SQL查询 | 授权数据源 |
| REPORTER | 报表开发者 | SQL查询、报表管理 | 授权报表分组 |
| VIEWER | 普通查看者 | 查看报表 | 授权报表分组 |

#### 3.5.4 菜单权限细项
- `menu:user:view` - 用户查看
- `menu:user:edit` - 用户编辑
- `menu:datasource:view` - 数据源查看
- `menu:datasource:edit` - 数据源编辑
- `menu:query:execute` - SQL执行
- `menu:report:view` - 报表查看
- `menu:report:edit` - 报表编辑
- `menu:mask:edit` - 脱敏规则管理

#### 3.5.5 数据权限细项
- `data:datasource:{id}` - 访问指定数据源
- `data:group:{id}` - 访问指定报表分组及其子分组

### 3.6 报表分组与组织架构

#### 3.6.1 分组管理（树形）
- 分组支持多级树形结构
- 每个分组可有父分组（顶层分组 parent_id = 0）
- 分组可分配给用户/角色（数据权限）

#### 3.6.2 用户-分组关联
- 用户可属于一个或多个分组
- **继承规则**：若用户属于分组 A，而分组 A 是分组 B 的父级，则用户可访问分组 B 下的所有报表
- 处于树顶端的用户可以看到树下所有子分组的报表

#### 3.6.3 示例
```
经营分析（顶级分组）
├── 华东区
│   ├── 上海分公司
│   └── 浙江分公司
└── 华北区
    └── 北京分公司
```
- 若用户属于"华东区"，则可查看华东区及所有子分组的报表
- 若用户属于"上海分公司"，则只能查看上海分公司的报表

---

## 4. 数据库设计

### 4.1 内置库表（MySQL）

| 表名 | 说明 |
|-----|------|
| `rh_user` | 用户表 |
| `rh_role` | 角色表 |
| `rh_user_role` | 用户-角色关联 |
| `rh_menu_permission` | 菜单权限定义表 |
| `rh_data_permission` | 数据权限定义表 |
| `rh_user_menu_permission` | 用户直接菜单权限 |
| `rh_user_data_permission` | 用户直接数据权限 |
| `rh_role_menu_permission` | 角色菜单权限 |
| `rh_role_data_permission` | 角色数据权限 |
| `rh_report_group` | 报表分组表（树形） |
| `rh_user_group` | 用户-分组关联表 |
| `rh_data_source` | 数据源配置 |
| `rh_data_source_mask` | 数据源脱敏规则（表+字段级） |
| `rh_report` | 报表配置 |
| `rh_report_param` | 报表参数 |
| `rh_query_history` | 查询历史 |
| `rh_report_share` | 报表分享 |

---

## 5. API 设计

### 5.1 认证
- `POST /api/auth/login` - 登录
- `POST /api/auth/logout` - 登出
- `GET /api/auth/me` - 当前用户信息

### 5.2 用户与权限
- `GET /api/user` - 用户列表
- `POST /api/user` - 创建用户
- `PUT /api/user/{id}` - 更新用户
- `DELETE /api/user/{id}` - 删除用户
- `GET /api/user/{id}/permissions` - 获取用户所有权限（含直接+角色）
- `POST /api/user/{id}/menu-permissions` - 分配直接菜单权限
- `POST /api/user/{id}/data-permissions` - 分配直接数据权限

### 5.3 角色管理
- `GET /api/role` - 角色列表
- `POST /api/role` - 创建角色
- `PUT /api/role/{id}` - 更新角色
- `DELETE /api/role/{id}` - 删除角色
- `POST /api/role/{id}/menu-permissions` - 分配角色菜单权限
- `POST /api/role/{id}/data-permissions` - 分配角色数据权限

### 5.4 报表分组
- `GET /api/group` - 分组列表（树形）
- `POST /api/group` - 创建分组
- `PUT /api/group/{id}` - 更新分组
- `DELETE /api/group/{id}` - 删除分组

### 5.5 数据源
- `GET /api/datasource` - 列表（按数据权限过滤）
- `POST /api/datasource` - 新增
- `PUT /api/datasource/{id}` - 更新
- `DELETE /api/datasource/{id}` - 删除
- `POST /api/datasource/{id}/test` - 连接测试
- `GET /api/datasource/{id}/tables` - 获取表列表
- `GET /api/datasource/{id}/columns` - 获取列信息

### 5.6 SQL 查询
- `POST /api/query/execute` - 执行查询
- `GET /api/query/history` - 查询历史

### 5.7 报表
- `GET /api/report` - 列表（按数据权限过滤）
- `POST /api/report` - 新增
- `PUT /api/report/{id}` - 更新
- `DELETE /api/report/{id}` - 删除
- `GET /api/report/{id}/data` - 获取报表数据
- `GET /api/report/share/{token}` - 分享访问

### 5.8 脱敏规则
- `GET /api/mask` - 脱敏规则列表
- `POST /api/mask` - 创建规则
- `PUT /api/mask/{id}` - 更新规则
- `DELETE /api/mask/{id}` - 删除规则

---

## 6. 安全配置

- **网络：** 所有服务仅监听 `127.0.0.1`，不暴露公网 IP
- **认证：** JWT Token（过期时间可配置）
- **密码：** BCrypt 加密
- **SQL 注入防护：** 参数化查询 + MyBatis
- **敏感数据：** 脱敏规则（全字段遮蔽 `****`）自动拦截
- **接口权限：** Spring Security 注解拦截
- **数据权限：** 查询时自动根据权限过滤可访问范围

---

## 7. 部署架构

```
宿主机 (127.0.0.1 only)
├── MySQL Container (127.0.0.1:3306)
├── Backend Container (127.0.0.1:8080)
└── Frontend Container (127.0.0.1:3000)
```

---

## 8. 开发进度

- [x] 项目初始化（后端骨架 + 前端骨架）
- [x] 内置 MySQL Docker 部署
- [x] 数据库表设计与初始化脚本
- [x] 用户认证模块（JWT）
- [x] 数据源管理模块 + 适配器接口
- [x] SQL 查询执行器
- [x] 数据脱敏模块（全遮蔽，支持表+字段配置）
- [ ] 报表管理模块 + 报表分组
- [ ] 权限控制模块（菜单权限 + 数据权限）
- [ ] 前端页面开发
- [ ] Docker Compose 打包部署
- [ ] 联调测试

---

*最后更新：2026-03-20*
