import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 * 软件测试实验八的可执行记录程序。
 *
 * 每名组员分别执行一个模块的黑盒等价类测试，以及一个代码片段的
 * 白盒逻辑覆盖测试。程序复用 CampusMarketTest 中的内存业务夹具。
 *
 * 用法：
 *   javac -encoding UTF-8 --release 21 -d out/production/Campus-Trade \
 *       src/CampusMarketTest.java src/Experiment8Test.java
 *   java -cp out/production/Campus-Trade Experiment8Test
 */
public class Experiment8Test {

    static class TestCase {
        final String id;
        final String kind;
        final String input;
        final String expected;
        final Supplier<String> action;

        TestCase(String id, String kind, String input, String expected, Supplier<String> action) {
            this.id = id;
            this.kind = kind;
            this.input = input;
            this.expected = expected;
            this.action = action;
        }
    }

    static class Suite {
        final String tester;
        final String module;
        final String title;
        final List<TestCase> cases;

        Suite(String tester, String module, String title, List<TestCase> cases) {
            this.tester = tester;
            this.module = module;
            this.title = title;
            this.cases = cases;
        }
    }

    static TestCase black(String id, String input, String expected, Supplier<String> action) {
        return new TestCase(id, "黑盒", input, expected, action);
    }

    static TestCase white(String id, String input, String expected, Supplier<String> action) {
        return new TestCase(id, "白盒", input, expected, action);
    }

    static List<TestCase> accountCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        boolean black = "B".equals(prefix);
        String p = black ? "B" : "W";
        if (black) {
            list.add(black("T1-B01", "sid=空", "学号不能为空",
                    () -> CampusMarketTest.register("", "13800000001", "123456")));
            list.add(black("T1-B02", "sid=202400021（9位）", "学号格式错误",
                    () -> CampusMarketTest.register("202400021", "13800000002", "123456")));
            list.add(black("T1-B03", "sid=20240001（已注册）", "该学号已注册",
                    () -> CampusMarketTest.register("20240001", "13800000003", "123456")));
            list.add(black("T1-B04", "验证码=000000", "验证码错误",
                    () -> CampusMarketTest.register("20246666", "13800000004", "000000")));
            list.add(black("T1-B05", "8位新学号+正确验证码", "注册成功，状态为待认证",
                    () -> CampusMarketTest.register("20246666", "13800000005", "123456")));
        } else {
            list.add(white("T1-W01", "sid=null", "学号不能为空",
                    () -> CampusMarketTest.register(null, "13800000001", "123456")));
            list.add(white("T1-W02", "sid=空格串", "学号不能为空",
                    () -> CampusMarketTest.register("   ", "13800000002", "123456")));
            list.add(white("T1-W03", "sid=9位数字", "学号格式错误",
                    () -> CampusMarketTest.register("202400021", "13800000003", "123456")));
            list.add(white("T1-W04", "sid已存在", "该学号已注册",
                    () -> CampusMarketTest.register("20240001", "13800000004", "123456")));
            list.add(white("T1-W05", "验证码错误", "验证码错误",
                    () -> CampusMarketTest.register("20246666", "13800000005", "000000")));
            list.add(white("T1-W06", "有效输入", "注册成功，状态为待认证",
                    () -> CampusMarketTest.register("20246666", "13800000006", "123456")));
        }
        return list;
    }

    static List<TestCase> goodsCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T2-B01", "价格0.01，图片1，标题10，本人", "发布成功，状态在售",
                    () -> CampusMarketTest.publish(0.01, 1, 10, true)));
            list.add(black("T2-B02", "价格0", "价格必须大于0",
                    () -> CampusMarketTest.publish(0, 1, 10, true)));
            list.add(black("T2-B03", "图片10张", "最多上传9张图片",
                    () -> CampusMarketTest.publish(10, 10, 10, true)));
            list.add(black("T2-B04", "标题31字", "标题过长",
                    () -> CampusMarketTest.publish(10, 1, 31, true)));
            list.add(black("T2-B05", "非商品所有者", "无权操作",
                    () -> CampusMarketTest.publish(10, 1, 10, false)));
        } else {
            list.add(white("T2-W01", "四个条件均合法", "发布成功，状态在售",
                    () -> CampusMarketTest.publish(10, 1, 10, true)));
            list.add(white("T2-W02", "owner=false", "无权操作",
                    () -> CampusMarketTest.publish(10, 1, 10, false)));
            list.add(white("T2-W03", "price=0", "价格必须大于0",
                    () -> CampusMarketTest.publish(0, 1, 10, true)));
            list.add(white("T2-W04", "images=10", "最多上传9张图片",
                    () -> CampusMarketTest.publish(10, 10, 10, true)));
            list.add(white("T2-W05", "titleLen=31", "标题过长",
                    () -> CampusMarketTest.publish(10, 1, 31, true)));
        }
        return list;
    }

    static List<TestCase> searchCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T3-B01", "关键词为空", "4件在售商品",
                    CampusMarketTest::searchEmpty));
            list.add(black("T3-B02", "关键词=台灯", "1件商品",
                    () -> CampusMarketTest.searchKeyword("台灯")));
            list.add(black("T3-B03", "关键词=不存在", "返回空列表并提示",
                    () -> CampusMarketTest.searchKeyword("不存在")));
            list.add(black("T3-B04", "价格区间0至50", "[0, 25, 50]",
                    () -> CampusMarketTest.searchPriceRange(0, 50)));
        } else {
            list.add(white("T3-W01", "空关键词，遍历全部夹具", "4件在售商品",
                    CampusMarketTest::searchEmpty));
            list.add(white("T3-W02", "命中一个标题", "1件商品",
                    () -> CampusMarketTest.searchKeyword("台灯")));
            list.add(white("T3-W03", "标题不命中", "返回空列表并提示",
                    () -> CampusMarketTest.searchKeyword("不存在")));
            list.add(white("T3-W04", "价格下界与上界", "[0, 25, 50]",
                    () -> CampusMarketTest.searchPriceRange(0, 50)));
        }
        return list;
    }

    static List<TestCase> orderCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T4-B01", "在售商品，非本人", "下单成功，状态待付款",
                    () -> CampusMarketTest.createOrder(3001, 2L)));
            list.add(black("T4-B02", "购买自己的商品", "不能购买自己的商品",
                    () -> CampusMarketTest.createOrder(3002, 1L)));
            list.add(black("T4-B03", "已售出商品", "商品不可购买",
                    () -> CampusMarketTest.createOrder(3003, 2L)));
            list.add(black("T4-B04", "不存在的商品", "商品不可购买",
                    () -> CampusMarketTest.createOrder(9999, 2L)));
        } else {
            list.add(white("T4-W01", "商品存在且在售，非本人", "下单成功，状态待付款",
                    () -> CampusMarketTest.createOrder(3001, 2L)));
            list.add(white("T4-W02", "商品存在且在售，本人", "不能购买自己的商品",
                    () -> CampusMarketTest.createOrder(3002, 1L)));
            list.add(white("T4-W03", "商品存在但已售出", "商品不可购买",
                    () -> CampusMarketTest.createOrder(3003, 2L)));
            list.add(white("T4-W04", "商品不存在，触发短路", "商品不可购买",
                    () -> CampusMarketTest.createOrder(9999, 2L)));
        }
        return list;
    }

    static List<TestCase> paymentCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T5-B01", "金额一致，待付款", "支付成功并生成流水",
                    () -> CampusMarketTest.pay(4001, 50, true)));
            list.add(black("T5-B02", "金额少0.01", "金额不符",
                    () -> CampusMarketTest.pay(4002, 49.99, true)));
            list.add(black("T5-B03", "沙箱失败", "订单保持待付款",
                    () -> CampusMarketTest.pay(4003, 20, false)));
            list.add(black("T5-B04", "支付成功后再次支付", "订单不可支付",
                    () -> {
                        CampusMarketTest.pay(4001, 50, true);
                        return CampusMarketTest.pay(4001, 50, true);
                    }));
            list.add(black("T5-B05", "同一订单回调两次", "只生成一条支付流水",
                    () -> CampusMarketTest.payCallbackTwice(4001)));
        } else {
            list.add(white("T5-W01", "订单不存在", "订单不可支付",
                    () -> CampusMarketTest.pay(9999, 10, true)));
            list.add(white("T5-W02", "金额不一致", "金额不符",
                    () -> CampusMarketTest.pay(4002, 49.99, true)));
            list.add(white("T5-W03", "渠道失败", "订单保持待付款",
                    () -> CampusMarketTest.pay(4003, 20, false)));
            list.add(white("T5-W04", "全部条件成立", "支付成功并生成流水",
                    () -> CampusMarketTest.pay(4001, 50, true)));
            list.add(white("T5-W05", "重复回调", "只生成一条支付流水",
                    () -> CampusMarketTest.payCallbackTwice(4001)));
        }
        return list;
    }

    static List<TestCase> messageCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T6-B01", "有订单关系，普通文本", "发送成功，未读+1",
                    () -> CampusMarketTest.sendMessage(1, 2, "在吗", true)));
            list.add(black("T6-B02", "无订单关系", "无权限发送",
                    () -> CampusMarketTest.sendMessage(1, 3, "你好", false)));
            list.add(black("T6-B03", "内容含手机号", "手机号被打码",
                    () -> CampusMarketTest.sendMessage(1, 2, "电话 13800001234", true)));
            list.add(black("T6-B04", "连续三条普通消息", "未读数量为3",
                    CampusMarketTest::sendThree));
        } else {
            list.add(white("T6-W01", "relation=true，无手机号", "发送成功，未读+1",
                    () -> CampusMarketTest.sendMessage(1, 2, "在吗", true)));
            list.add(white("T6-W02", "relation=true，有手机号", "手机号被打码",
                    () -> CampusMarketTest.sendMessage(1, 2, "电话 13800001234", true)));
            list.add(white("T6-W03", "relation=false", "无权限发送",
                    () -> CampusMarketTest.sendMessage(1, 3, "你好", false)));
            list.add(white("T6-W04", "重复调用后的计数", "未读数量为3",
                    CampusMarketTest::sendThree));
        }
        return list;
    }

    static List<TestCase> evaluationCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T7-B01", "已完成订单，评分5", "评价成功，信用分上升",
                    () -> CampusMarketTest.evaluate(5001, 5)));
            list.add(black("T7-B02", "待付款订单", "评价未开放",
                    () -> CampusMarketTest.evaluate(5002, 5)));
            list.add(black("T7-B03", "同一订单评价两次", "请勿重复评价",
                    () -> {
                        CampusMarketTest.evaluate(5001, 5);
                        return CampusMarketTest.evaluate(5001, 5);
                    }));
            list.add(black("T7-B04", "评分连续累加边界", "信用分不超过100",
                    CampusMarketTest::fiveBadEvaluations));
        } else {
            list.add(white("T7-W01", "credit=69.9", "一般",
                    () -> CampusMarketTest.creditLevel(69.9)));
            list.add(white("T7-W02", "credit=70", "良好",
                    () -> CampusMarketTest.creditLevel(70)));
            list.add(white("T7-W03", "credit=89.9", "良好",
                    () -> CampusMarketTest.creditLevel(89.9)));
            list.add(white("T7-W04", "credit=90", "优秀",
                    () -> CampusMarketTest.creditLevel(90)));
        }
        return list;
    }

    static List<TestCase> permissionCases(String prefix) {
        List<TestCase> list = new ArrayList<TestCase>();
        if ("B".equals(prefix)) {
            list.add(black("T8-B01", "ADMIN，分类未被引用", "修改成功并写日志",
                    () -> CampusMarketTest.updateCategory("ADMIN", false)));
            list.add(black("T8-B02", "USER，分类未被引用", "403 权限不足",
                    () -> CampusMarketTest.updateCategory("USER", false)));
            list.add(black("T8-B03", "ADMIN，分类已被引用", "该分类已被引用",
                    () -> CampusMarketTest.updateCategory("ADMIN", true)));
        } else {
            list.add(white("T8-W01", "ADMIN且未被引用", "修改成功并写日志",
                    () -> CampusMarketTest.updateCategory("ADMIN", false)));
            list.add(white("T8-W02", "非ADMIN，短路拒绝", "403 权限不足",
                    () -> CampusMarketTest.updateCategory("USER", false)));
            list.add(white("T8-W03", "ADMIN且已被引用", "该分类已被引用",
                    () -> CampusMarketTest.updateCategory("ADMIN", true)));
        }
        return list;
    }

    static List<Suite> suites() {
        List<Suite> list = new ArrayList<Suite>();
        list.add(new Suite("孙振宇", "1", "用户与账号管理", join(
                accountCases("B"), accountCases("W"))));
        list.add(new Suite("王小龙", "2", "商品发布与管理", join(
                goodsCases("B"), goodsCases("W"))));
        list.add(new Suite("孔阳涛", "3", "商品检索与浏览", join(
                searchCases("B"), searchCases("W"))));
        list.add(new Suite("王胜涛", "4", "交易与订单管理", join(
                orderCases("B"), orderCases("W"))));
        list.add(new Suite("娄绥皓", "5", "在线支付与退款结算", join(
                paymentCases("B"), paymentCases("W"))));
        list.add(new Suite("赵洋洋", "6", "即时沟通与消息通知", join(
                messageCases("B"), messageCases("W"))));
        list.add(new Suite("贾晓霖", "7", "评价与信用体系", join(
                evaluationCases("B"), evaluationCases("W"))));
        list.add(new Suite("董文杰", "10", "系统管理与权限控制", join(
                permissionCases("B"), permissionCases("W"))));
        return list;
    }

    static List<TestCase> join(List<TestCase> a, List<TestCase> b) {
        List<TestCase> out = new ArrayList<TestCase>();
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    static int pass;
    static int fail;

    public static void main(String[] args) throws Exception {
        List<String[]> rows = new ArrayList<String[]>();
        System.out.println("==============================================================================");
        System.out.println(" 校园二手交易平台  软件测试实验八");
        System.out.println(" 执行时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        System.out.println(" 运行环境：Java " + System.getProperty("java.version")
                + " / " + System.getProperty("os.name"));
        System.out.println("==============================================================================");

        for (Suite suite : suites()) {
            CampusMarketTest.prepare();
            int suitePass = 0;
            int suiteFail = 0;
            System.out.println();
            System.out.printf("模块%s  %s  测试者：%s%n", suite.module, suite.title, suite.tester);
            System.out.println("------------------------------------------------------------------------------");
            System.out.printf("%-8s %-5s %-27s %-17s %-6s%n",
                    "用例", "类型", "输入/条件", "实际输出", "结论");
            for (TestCase c : suite.cases) {
                CampusMarketTest.prepare();
                String actual;
                try {
                    actual = c.action.get();
                } catch (Exception e) {
                    actual = "异常：" + e.getClass().getSimpleName();
                }
                boolean ok = c.expected.equals(actual);
                if (ok) {
                    pass++;
                    suitePass++;
                } else {
                    fail++;
                    suiteFail++;
                }
                rows.add(new String[]{c.id, suite.tester, suite.module, c.kind,
                        c.input, c.expected, actual, ok ? "通过" : "不通过"});
                System.out.printf("%-8s %-5s %-27s %-17s %-6s%n",
                        c.id, c.kind, cut(c.input, 15), cut(actual, 10),
                        ok ? "通过" : "不通过");
            }
            System.out.printf("模块小计：%d 个用例，通过 %d，失败 %d%n",
                    suite.cases.size(), suitePass, suiteFail);
        }

        System.out.println();
        System.out.println("==============================================================================");
        System.out.printf("实验八合计 %d 个用例：通过 %d，失败 %d，通过率 %.1f%%%n",
                rows.size(), pass, fail, 100.0 * pass / rows.size());
        System.out.println("覆盖情况：8 名组员、8 个模块、8 个白盒代码片段。");
        System.out.println("==============================================================================");
        writeCsv(rows);
        System.out.println("完整明细已导出：测试结果_实验八.csv");
        if (fail > 0) {
            System.exit(1);
        }
    }

    static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    static String escape(String s) {
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    static void writeCsv(List<String[]> rows) throws Exception {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream("测试结果_实验八.csv"), StandardCharsets.UTF_8));
        w.write('\ufeff');
        w.write("用例编号,测试者,模块,测试类型,输入或条件,预期结果,实际结果,结论\n");
        for (String[] r : rows) {
            w.write(String.join(",", escape(r[0]), escape(r[1]), escape(r[2]),
                    escape(r[3]), escape(r[4]), escape(r[5]), escape(r[6]), escape(r[7])));
            w.write("\n");
        }
        w.close();
    }
}
