# 数据库说明（Campus-Trade）

软件设计报告里的“数据库结构设计”是**设计文档**（表清单与关系说明），这里补的是可执行的落地部分：建表脚本、示例数据、连通性校验程序。

## 文件

| 文件 | 说明 |
|---|---|
| `db/schema.sql` | 建库 + 13 张表、主外键、索引、CHECK 约束 |
| `db/data.sql` | 5 个用户、6 件商品、2 笔订单及支付/评价/举报/消息等示例数据 |
| `src/DatabaseSmokeTest.java` | JDBC 校验：表数量、外键、示例数据、联查、事务回滚 |
| `lib/mysql-connector-j-8.0.31.jar` | MySQL JDBC 驱动 |

## 第一步：启动 MySQL

你机器上装的是 MySQL 9.5（`/usr/local/mysql-9.5.0-macos15-arm64`），但**数据目录还是空的、服务没启动**，所以 IDEA 的“数据库”面板显示“无数据源”。

推荐用系统设置启动：

1. 打开「系统设置 → MySQL」，点 **Initialize Database**，按提示设置 root 密码（建议记下来）。
2. 再点 **Start MySQL Server**，等状态变成 running。

或者用终端：

```bash
sudo /usr/local/mysql-9.5.0-macos15-arm64/support-files/mysql.server start
```

验证：

```bash
mysql -u root -p -e "select version();"
```

## 第二步：执行建表与示例数据

命令行方式：

```bash
cd "/Users/dongwenjie/IdeaProjects/Campus-Trade"
mysql -u root -p < db/schema.sql
mysql -u root -p < db/data.sql
```

IDEA 方式（对应截图右侧的“数据库”面板）：

1. 点「+ → 数据源 → MySQL」。
2. Host `localhost`，Port `3306`，User `root`，填入密码，Database 先空着。
3. 点「测试连接」，成功后 OK。
4. 右键数据源 → Open console，把 `schema.sql`、`data.sql` 的内容依次粘进去执行。
5. 刷新后可在 `campus_trade` 下看到 13 张表。

Navicat / DataGrip 同理：新建 MySQL 连接 → 运行 SQL 文件。

## 第三步：跑连通性校验

命令行（在项目根目录）：

```bash
javac -encoding UTF-8 -d out/production/Campus-Trade \
  -cp lib/mysql-connector-j-8.0.31.jar \
  src/DatabaseSmokeTest.java

java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" \
  DatabaseSmokeTest
```

如果 root 有密码：

```bash
java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" \
  DatabaseSmokeTest \
  "jdbc:mysql://127.0.0.1:3306/campus_trade?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai" \
  root "你的密码"
```

IDEA 里直接运行 `DatabaseSmokeTest` 的 `main` 即可（驱动已加入模块依赖）。有密码时在 Run Configuration 的 Program arguments 里按 `<url> <user> <password>` 顺序填。

预期结果：

```
[通过] 共 13 张数据表
[通过] 外键约束已建立（≥10 个）
[通过] 示例数据已导入
[通过] 联查返回 5 条在售商品
[通过] 写入与回滚正常（事务可用）
校验结果：通过 5 项，失败 0 项，数据库结构正常
```

## 与设计文档的对应

| 设计报告中的表 | 实际表名 | 来源 |
|---|---|---|
| 用户表 | `t_user` | 实体“用户” |
| 角色表 / 权限表 / 角色权限表 | `t_role` / `t_permission` / `t_role_permission` | 系统管理中的角色权限 |
| 商品分类表 | `t_category` | 实体“商品分类” |
| 商品表 / 商品图片表 | `t_goods` / `t_goods_image` | 实体“商品”及其多值属性 |
| 订单表 | `t_order` | 实体“订单” |
| 支付流水表 | `t_payment` | 实体“支付流水”，订单 1:1 |
| 评价表 | `t_evaluation` | 实体“评价”，订单 1:1 |
| 举报表 | `t_report` | 实体“举报” |
| 消息表 | `t_message` | 实体“消息” |
| 操作日志表 | `t_operation_log` | 系统管理加工产生 |

## 说明

我在 Codex 沙箱里尝试直接拉起 MySQL 9.5 做建表验证，初始化阶段进程收到 SIGSEGV（沙箱对内存映射的限制），因此**数据库服务需要你在本机手动启动一次**。SQL 脚本、驱动和校验程序都已就位，启动后按上面三步执行即可。
