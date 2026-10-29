# Campus-Trade 软件测试

本工程用于《实验三 软件测试实验报告》的可执行验证，覆盖校园二手交易平台 10 个模块、50 个测试用例。
代码只依赖 JDK，不需要 Maven、Spring Boot 或 MySQL。

## 目录

```
Campus-Trade/
├── src/CampusMarketTest.java     # 测试程序（10  modules × 5 cases）
├── out/                          # 编译输出（自动生成）
├── 测试结果_回归.csv              # 运行后生成
└── 测试结果_首轮.csv              # --legacy 模式生成的缺陷复现结果
```

## 在 IntelliJ IDEA 中运行

1. 打开工程后等待索引完成，`src` 已标记为源码目录，Project SDK 为 JDK 21。
2. 打开 `CampusMarketTest.java`，点击 `main` 方法左侧的绿色三角运行。
3. 需要复现首轮缺陷时，在 Run/Debug Configurations 的 Program arguments 里填 `--legacy` 再运行。

## 命令行运行

```bash
cd "/Users/dongwenjie/IdeaProjects/Campus-Trade"

# 编译
javac -encoding UTF-8 -d out/production/Campus-Trade src/CampusMarketTest.java

# 修复后回归：预期 50/50 通过
java -cp out/production/Campus-Trade CampusMarketTest

# 首轮缺陷复现：预期 40/50 通过
java -cp out/production/Campus-Trade CampusMarketTest --legacy
```

## 预期输出

```
合计 50 个用例：通过 50，失败 0，通过率 100.0%      # 默认模式
合计 50 个用例：通过 40，失败 10，通过率 80.0%      # --legacy 模式
```

首轮失败的 10 条用例分别对应 BUG-01 至 BUG-10，与《实验三 软件测试实验报告》中的缺陷表一一对应。

## 数据库

数据库结构设计与建表脚本在 `db/` 目录：

| 文件 | 说明 |
|---|---|
| `db/schema.sql` | 建库与 13 张表（主外键、索引、CHECK 约束） |
| `db/data.sql` | 示例数据：5 个用户、6 件商品、2 笔订单及支付/评价/举报/消息 |
| `db/README.md` | MySQL 启动、建表、IDEA 数据源配置的完整步骤 |
| `src/DatabaseSmokeTest.java` | 连接校验：表数量、外键、联查、事务回滚 |
| `lib/mysql-connector-j-8.0.31.jar` | JDBC 驱动，已加入模块依赖 |

启动 MySQL 后执行：

```bash
mysql -u root -p < db/schema.sql
mysql -u root -p < db/data.sql
```

随后运行 `DatabaseSmokeTest` 的 `main`，预期 5 项校验全部通过。

## 版本管理（GitHub）

本仓库已准备好 `.gitignore`（排除 `db/db.properties`、`out/`、测试结果 CSV）和一键推送脚本 `git-push.sh`。

1. 在 https://github.com/new 新建仓库，名称建议 `Campus-Trade`，可见性选 **Private**，不要勾选 README/.gitignore。
2. 在项目目录执行：

```bash
./git-push.sh
```

   按提示粘贴仓库地址（形如 `https://github.com/用户名/Campus-Trade.git`），首次推送会要求登录 GitHub。

3. 也可以直接传入地址：

```bash
./git-push.sh https://github.com/用户名/Campus-Trade.git
```

仓库内容包含：`src/` 测试与数据库校验程序、`db/` 建表与示例数据、`docs/` 四份实验报告、`lib/` JDBC 驱动。数据库密码不会入库。
