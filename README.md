# Campus-Trade 校园二手交易平台

“2024 级网络工程第 2 组”的软件工程课程项目，现已从实验文档和业务规则原型继续实现为一个可运行的 Web 最小成品（MVP）。

当前版本包含 Vue 3 响应式页面、Spring Boot REST API、MySQL 持久化、登录身份、商品发布与图片上传、订单锁货、沙箱支付、退款和卖家结算。原来的六份实验报告与测试程序保持不变。

## 已实现功能

| 模块 | 功能 |
|---|---|
| 账号 | 学号/手机号登录、BCrypt 密码、学号注册、HTTP-only 会话 Cookie、登录状态恢复 |
| 市场 | 关键词搜索、分类/校区筛选、价格与热度排序、分页、商品详情 |
| 商品 | 图片上传、发布商品、我的发布、下架商品、浏览计数 |
| 订单 | 事务锁货、防止购买自己的商品、取消订单、沙箱支付、退款、确认收货 |
| 结算 | 创建支付流水、退款状态、98% 卖家应结金额、结算状态 |
| 页面 | 桌面端与手机端自适应，包含首页、登录注册、详情、发布、订单和我的发布 |
| 部署 | 本地 Jar 运行、Docker Compose 部署、数据库初始化脚本 |

## 技术架构

```text
浏览器
  │
  ├── Vue 3 + Vue Router + Vite
  │        │ /api
  │        ▼
  └── Spring Boot 3.4 + JDBC + HikariCP
           │
           └── MySQL 8.0 / 9.x
```

前端生产文件已经构建到 `server/src/main/resources/static`，因此只启动 Spring Boot 即可访问完整页面。

## 快速启动

### 1. 环境要求

- JDK 21 或更高版本
- Maven 3.6.3 或更高版本
- MySQL 8.0 或 9.x
- Node.js 20+ 与 pnpm/npm，仅在修改前端源码时需要

### 2. 配置并初始化 MySQL

```bash
cp db/db.properties.example db/db.properties
```

编辑 `db/db.properties`，填写本机 MySQL 用户名和密码。然后执行：

```bash
./scripts/setup-db.sh
```

脚本会提示确认，因为 `schema.sql` 会删除并重建 `campus_trade` 中同名的 14 张表。也可以手动导入：

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/data.sql
```

### 3. 启动应用

```bash
./scripts/run-local.sh
```

访问 [http://localhost:8080](http://localhost:8080)。

课程演示账号密码统一为 `Campus@123`：

| 账号 | 姓名 | 角色 |
|---|---|---|
| `20240004` | 陈浩 | 买家 |
| `20240002` | 李晓明 | 卖家 |
| `20240003` | 王雪 | 卖家 |
| `20240001` | 董文杰 | 管理员 |

支付为课程沙箱流程，不会产生真实扣款。

## 在 IntelliJ IDEA 中运行

1. 打开仓库根目录。
2. 在 `server/pom.xml` 上右键，选择 **Add as Maven Project**。
3. 将 `com.campustrade.CampusTradeApplication` 设为运行入口。
4. 运行配置的工作目录设为仓库根目录 `Campus-Trade`。
5. 配置环境变量 `DB_USERNAME` 和 `DB_PASSWORD`；数据库地址默认为本机 `campus_trade`。
6. 启动后访问 `http://localhost:8080`。

原有的 `CampusMarketTest` 与 `Experiment8Test` IDEA 运行配置仍保留，不受 Web 应用运行配置影响。

## 前端开发

开发时需要分别启动后端和 Vite：

```bash
# 终端 1：仓库根目录
./scripts/run-local.sh

# 终端 2
cd web
pnpm install
pnpm dev
```

访问 [http://localhost:5173](http://localhost:5173)。Vite 会将 `/api` 与 `/uploads` 代理到 `8080`。

修改前端后重新生成可随 Spring Boot 一起运行的文件：

```bash
./scripts/build.sh
```

## Docker 部署

### 本地演示

```bash
docker compose up --build
```

默认开放：

- Web 应用：`http://localhost:8080`
- MySQL：`localhost:13306`

如果端口已被其他程序占用，可以在启动前覆盖宿主机端口：

```bash
APP_HOST_PORT=18080 MYSQL_HOST_PORT=13307 docker compose up --build
```

`docker-compose.yml` 中的密码和 `APP_AUTH_SECRET` 仅用于本地演示，正式部署前必须替换。

### 云服务器生产部署

仓库提供独立的 `docker-compose.production.yml`。它使用随机密码、限制 Java 和 MySQL
内存，并且不向宿主机映射 MySQL 端口。

完整操作步骤见 [轻量服务器部署说明](docs/轻量服务器部署说明.md)。

## 主要 API

| 方法 | 地址 | 说明 |
|---|---|---|
| `POST` | `/api/auth/register` | 注册 |
| `POST` | `/api/auth/login` | 登录并设置会话 Cookie |
| `GET` | `/api/auth/me` | 当前用户 |
| `GET` | `/api/goods` | 搜索和筛选商品 |
| `GET` | `/api/goods/{id}` | 商品详情 |
| `POST` | `/api/goods` | 发布商品 |
| `POST` | `/api/uploads/images` | 上传 JPG/PNG/WebP 图片 |
| `GET` | `/api/goods/mine` | 我的发布 |
| `POST` | `/api/orders` | 创建订单并锁定商品 |
| `GET` | `/api/orders?role=buyer` | 我买到的订单 |
| `GET` | `/api/orders?role=seller` | 我卖出的订单 |
| `POST` | `/api/orders/{id}/pay` | 沙箱支付 |
| `POST` | `/api/orders/{id}/cancel` | 取消待付款订单 |
| `POST` | `/api/orders/{id}/refund` | 全额沙箱退款 |
| `POST` | `/api/orders/{id}/complete` | 确认收货并结算 |

## 数据库

[schema.sql](db/schema.sql) 包含 14 张表、21 个外键、CHECK 约束和订单/资金相关索引。核心资金状态保存在 `t_payment`，商品与订单通过 `t_order.goods_id → t_goods.seller_id` 关联卖家，不在订单表重复保存卖家字段。

详细说明见 [数据库实验说明](db/README.md)。

## 测试与实验成果

### 数据库结构校验

```bash
javac -encoding UTF-8 -d out/production/Campus-Trade \
  -cp lib/mysql-connector-j-8.0.31.jar \
  src/DatabaseSmokeTest.java

java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" \
  DatabaseSmokeTest
```

### 核心业务回归

```bash
javac -encoding UTF-8 -d out/production/Campus-Trade \
  src/CampusMarketTest.java

java -cp out/production/Campus-Trade CampusMarketTest
java -cp out/production/Campus-Trade CampusMarketTest --legacy
```

### 实验八测试

```bash
javac -encoding UTF-8 -d out/production/Campus-Trade \
  src/CampusMarketTest.java src/Experiment8Test.java

java -cp out/production/Campus-Trade Experiment8Test
```

### 实验报告

- [实验一 项目选题与任务分解](<docs/实验一 项目选题与任务分解.docx>)
- [实验二 需求分析](<docs/实验二 需求分析实验报告.docx>)
- [实验三 软件设计](<docs/实验三 软件设计实验报告.docx>)
- [实验三 软件测试](<docs/实验三 软件测试实验报告.docx>)
- [实验五 数据库结构设计](<docs/实验五 数据库结构设计实验报告.docx>)
- [实验八 软件测试](<docs/实验八 软件测试实验报告.docx>)
- [实验八 八人个人测试报告](<docs/实验八 八人个人测试报告/>)
- [实验八 八人个人测试报告压缩包](<docs/实验八 八人个人测试报告.zip>)

## 仓库结构

```text
Campus-Trade/
├── server/       Spring Boot API、静态页面、资源配置
├── web/          Vue 3 页面源码、图片与 Vite 配置
├── db/           MySQL 结构、演示数据和说明
├── src/          课程业务规则与测试夹具
├── docs/         实验报告
├── scripts/      构建、数据库初始化和本地启动脚本
├── Dockerfile
├── docker-compose.yml
└── docker-compose.production.yml
```

## 当前边界与下一步

- 当前支付、退款与结算为沙箱状态流转，不调用真实支付平台。
- 图片保存到本地目录；生产环境应改为对象存储与图片压缩。
- 尚未实现实时聊天、评价提交、举报审核和完整管理后台。
- 下一步可增加 JUnit/API 集成测试、WebSocket 消息、统一审计日志，并在 API 稳定后接入微信小程序。
