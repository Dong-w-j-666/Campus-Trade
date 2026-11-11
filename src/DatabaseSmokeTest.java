import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 数据库连通性与结构校验（实验三 数据库结构设计配套）。
 *
 * 先启动 MySQL 并执行 db/schema.sql、db/data.sql，再运行本程序。
 *
 * 用法：
 *   java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" DatabaseSmokeTest
 *   java -cp "out/production/Campus-Trade:lib/mysql-connector-j-8.0.31.jar" DatabaseSmokeTest <url> <user> <password>
 */
public class DatabaseSmokeTest {

    static final String DEFAULT_URL =
            "jdbc:mysql://127.0.0.1:3306/campus_trade?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";

    public static void main(String[] args) {
        Properties cfg = loadConfig();
        String url = args.length > 0 ? args[0] : cfg.getProperty("db.url", DEFAULT_URL);
        String user = args.length > 1 ? args[1] : cfg.getProperty("db.user", "root");
        String pwd = args.length > 2 ? args[2] : cfg.getProperty("db.password", "");
        String source = args.length > 0 ? "命令行参数"
                : (new File("db/db.properties").exists() ? "db/db.properties" : "默认值（空密码）");

        int pass = 0;
        int fail = 0;

        System.out.println("==================================================================");
        System.out.println(" 校园二手交易平台  数据库结构校验");
        System.out.println(" 连接地址：" + url);
        System.out.println(" 用户名：" + user);
        System.out.println(" 配置来源：" + source);
        System.out.println("==================================================================");

        try (Connection conn = DriverManager.getConnection(url, user, pwd)) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("数据库版本：" + meta.getDatabaseProductName() + " " + meta.getDatabaseProductVersion());

            // 1. 表数量
            int tables = queryInt(conn, "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = 'campus_trade' AND table_type = 'BASE TABLE'");
            boolean t1 = tables == 13;
            report("共 13 张数据表", t1, "实际 " + tables + " 张");
            pass += t1 ? 1 : 0;
            fail += t1 ? 0 : 1;

            // 2. 表清单
            List<String> names = queryStrings(conn,
                    "SELECT table_name FROM information_schema.tables " +
                            "WHERE table_schema = 'campus_trade' AND table_type = 'BASE TABLE' ORDER BY table_name");
            System.out.println("表清单：" + String.join(", ", names));

            // 3. 外键数量
            int fks = queryInt(conn, "SELECT COUNT(*) FROM information_schema.table_constraints " +
                    "WHERE table_schema = 'campus_trade' AND constraint_type = 'FOREIGN KEY'");
            boolean t2 = fks >= 10;
            report("外键约束已建立（≥10 个）", t2, "实际 " + fks + " 个");
            pass += t2 ? 1 : 0;
            fail += t2 ? 0 : 1;

            // 4. 支付、退款与结算字段
            int paymentFields = queryInt(conn, "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_schema = 'campus_trade' AND table_name = 't_payment' " +
                    "AND column_name IN ('refund_status','refund_reason','settlement_amount','settlement_status','settled_at')");
            boolean t4 = paymentFields == 5;
            report("支付退款结算字段完整", t4, "实际 " + paymentFields + "/5");
            pass += t4 ? 1 : 0;
            fail += t4 ? 0 : 1;

            // 5. 双方评价联合唯一键
            int evalKeyColumns = queryInt(conn, "SELECT COUNT(DISTINCT column_name) FROM information_schema.statistics " +
                    "WHERE table_schema = 'campus_trade' AND table_name = 't_evaluation' " +
                    "AND index_name = 'uk_eval_order_evaluator' AND non_unique = 0 " +
                    "AND column_name IN ('order_id','evaluator_id')");
            boolean t5 = evalKeyColumns == 2;
            report("双方评价联合唯一键", t5, "实际覆盖 " + evalKeyColumns + "/2 列");
            pass += t5 ? 1 : 0;
            fail += t5 ? 0 : 1;

            // 6. 示例数据
            int users = queryInt(conn, "SELECT COUNT(*) FROM t_user");
            int goods = queryInt(conn, "SELECT COUNT(*) FROM t_goods");
            boolean t6 = users >= 5 && goods >= 6;
            report("示例数据已导入", t6, "用户 " + users + " 条，商品 " + goods + " 条");
            pass += t6 ? 1 : 0;
            fail += t6 ? 0 : 1;

            // 7. 多表联查
            String sql = "SELECT g.goods_id, g.title, g.price, c.category_name, u.real_name " +
                    "FROM t_goods g JOIN t_category c ON g.category_id = c.category_id " +
                    "JOIN t_user u ON g.seller_id = u.user_id WHERE g.status = 'ON_SALE' ORDER BY g.goods_id";
            System.out.println("------------------------------------------------------------------");
            System.out.println("在售商品联查（商品 / 分类 / 卖家）：");
            int rows = 0;
            try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    rows++;
                    System.out.printf("  #%d  %-18s %8.2f  %-8s %s%n",
                            rs.getLong(1), rs.getString(2), rs.getDouble(3), rs.getString(4), rs.getString(5));
                }
            }
            boolean t7 = rows == 5;
            report("联查返回 5 条在售商品", t7, "实际 " + rows + " 条");
            pass += t7 ? 1 : 0;
            fail += t7 ? 0 : 1;

            // 8. 写入回滚测试（不污染数据）
            conn.setAutoCommit(false);
            boolean t8;
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO t_message (sender_id, receiver_id, msg_type, content) VALUES (?, ?, 'SYSTEM', ?)")) {
                ps.setLong(1, 1);
                ps.setLong(2, 4);
                ps.setString(3, "冒烟测试消息");
                ps.executeUpdate();
                int inserted = queryInt(conn, "SELECT COUNT(*) FROM t_message WHERE content = '冒烟测试消息'");
                t8 = inserted == 1;
                conn.rollback();
            } catch (Exception e) {
                conn.rollback();
                t8 = false;
            }
            conn.setAutoCommit(true);
            report("写入与回滚正常（事务可用）", t8, t8 ? "已回滚，数据未残留" : "写入或回滚失败");
            pass += t8 ? 1 : 0;
            fail += t8 ? 0 : 1;

        } catch (Exception e) {
            System.out.println("连接失败：" + e.getMessage());
            System.out.println("请确认：1）MySQL 已启动；2）已执行 db/schema.sql 与 db/data.sql；");
            System.out.println("        3）用户名密码正确；4）端口为 3306。");
            System.out.println("配置二选一：");
            System.out.println("  A. 把 db/db.properties.example 复制为 db/db.properties，填入 db.password；");
            System.out.println("  B. Run Configuration 的 Program arguments 填：<url> <user> <password>");
            System.out.println("==================================================================");
            System.exit(1);
        }

        System.out.println("------------------------------------------------------------------");
        System.out.printf("校验结果：通过 %d 项，失败 %d 项，%s%n", pass, fail, fail == 0 ? "数据库结构正常" : "请检查失败项");
        System.out.println("==================================================================");
        if (fail > 0) {
            System.exit(1);
        }
    }

    static void report(String item, boolean ok, String detail) {
        System.out.printf("[%s] %-28s %s%n", ok ? "通过" : "失败", item, detail);
    }

    static Properties loadConfig() {
        Properties p = new Properties();
        File f = new File("db/db.properties");
        if (f.exists()) {
            try (InputStream in = new FileInputStream(f)) {
                p.load(in);
            } catch (Exception ignored) {
                // 读取失败时使用默认值
            }
        }
        return p;
    }

    static int queryInt(Connection conn, String sql) throws Exception {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    static List<String> queryStrings(Connection conn, String sql) throws Exception {
        List<String> list = new ArrayList<String>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(rs.getString(1));
            }
        }
        return list;
    }
}
