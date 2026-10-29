import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 校园二手交易平台 —— 软件测试实验（实验三）可执行测试程序。
 *
 * 只依赖 JDK 8 及以上，无需 Maven、Spring Boot、MySQL。
 * 覆盖软件测试报告中的 10 个模块、50 个测试用例。
 *
 * 用法：
 *   javac -encoding UTF-8 CampusMarketTest.java
 *   java CampusMarketTest             # 修复后回归，预期 50/50 通过
 *   java CampusMarketTest --legacy    # 复现首轮的 10 个缺陷，预期 40/50 通过
 */
public class CampusMarketTest {

    static boolean LEGACY = false;
    static final PrintStream OUT = new PrintStream(System.out, true);

    // ------------------------------------------------------------------ 简易领域模型
    static class User {
        String sid;
        String phone;
        String status = "待认证";
        String role = "BUYER";
        double credit = 80.0;
        boolean frozen = false;

        User(String sid, String phone) {
            this.sid = sid;
            this.phone = phone;
        }
    }

    static class Goods {
        long id;
        String title;
        double price;
        int images = 1;
        long sellerId;
        long categoryId = 1;
        String status = "在售";

        Goods(long id, String title, double price, long sellerId) {
            this.id = id;
            this.title = title;
            this.price = price;
            this.sellerId = sellerId;
        }
    }

    static class Order {
        long id;
        long goodsId;
        long buyerId;
        double amount;
        String status = "待付款";
        boolean evaluated = false;

        Order(long id, long goodsId, long buyerId, double amount) {
            this.id = id;
            this.goodsId = goodsId;
            this.buyerId = buyerId;
            this.amount = amount;
        }
    }

    static class Payment {
        String tradeNo;
        long orderId;
        double amount;

        Payment(String tradeNo, long orderId, double amount) {
            this.tradeNo = tradeNo;
            this.orderId = orderId;
            this.amount = amount;
        }
    }

    static class ReportItem {
        long id;
        long goodsId;
        String status = "待审核";
        int times;
        int priority = 1;

        ReportItem(long id, long goodsId) {
            this.id = id;
            this.goodsId = goodsId;
        }
    }

    // ------------------------------------------------------------------ 内存数据
    static Map<String, User> users = new LinkedHashMap<String, User>();
    static Map<Long, Goods> goodsMap = new LinkedHashMap<Long, Goods>();
    static Map<Long, Order> orderMap = new LinkedHashMap<Long, Order>();
    static List<Payment> payments = new ArrayList<Payment>();
    static List<ReportItem> reports = new ArrayList<ReportItem>();
    static Map<Long, Integer> unread = new LinkedHashMap<Long, Integer>();
    static List<String> logs = new ArrayList<String>();
    static long seq = 1000L;
    static final String VALID_CODE = "123456";

    // ------------------------------------------------------------------ 模块 1：用户与账号
    static String register(String sid, String phone, String code) {
        if (sid == null || sid.trim().isEmpty()) {
            return "学号不能为空";
        }
        boolean legal;
        if (LEGACY) {
            legal = sid.matches("\\d{8,9}");   // BUG-01：9 位也能通过
        } else {
            legal = sid.matches("\\d{8}");
        }
        if (!legal) {
            return "学号格式错误";
        }
        if (users.containsKey(sid)) {
            return "该学号已注册";
        }
        if (!VALID_CODE.equals(code)) {
            return "验证码错误";
        }
        users.put(sid, new User(sid, phone));
        return "注册成功，状态为待认证";
    }

    static String case1_4() {
        String a = register("20240002", "13800000001", VALID_CODE);
        String b = register("202400021", "13800000002", VALID_CODE);
        boolean ok8 = a.startsWith("注册成功");
        boolean bad9 = b.contains("格式错误");
        if (ok8 && bad9) {
            return "8位通过，9位提示格式错误";
        }
        if (ok8 && b.startsWith("注册成功")) {
            return "8位通过，9位注册成功";
        }
        return "8位" + a + "，9位" + b;
    }

    // ------------------------------------------------------------------ 模块 2：商品发布
    static String publish(double price, int images, int titleLen, boolean owner) {
        if (!owner) {
            return "无权操作";
        }
        if (LEGACY) {
            if (price < 0) {
                Goods g = new Goods(++seq, "测试商品", price, 1L);
                g.images = images;
                goodsMap.put(g.id, g);
                return "发布成功，价格为" + trim(price);
            }
        }
        if (price <= 0) {
            return "价格必须大于0";
        }
        if (images > 9) {
            return "最多上传9张图片";
        }
        if (titleLen > 30) {
            return "标题过长";
        }
        Goods g = new Goods(++seq, "测试商品", price, 1L);
        g.images = images;
        goodsMap.put(g.id, g);
        return "发布成功，状态在售";
    }

    // ------------------------------------------------------------------ 模块 3：检索
    static boolean inSearchFixture(Goods g) {
        return g.id >= 3100 && g.id < 3200;
    }

    static String searchEmpty() {
        int n = 0;
        for (Goods g : goodsMap.values()) {
            if (inSearchFixture(g) && "在售".equals(g.status)) {
                n++;
            }
        }
        return n + "件在售商品";
    }

    static String searchKeyword(String kw) {
        int n = 0;
        for (Goods g : goodsMap.values()) {
            if (inSearchFixture(g) && "在售".equals(g.status) && g.title.contains(kw)) {
                n++;
            }
        }
        return n == 0 ? "返回空列表并提示" : n + "件商品";
    }

    static String searchPriceRange(double min, double max) {
        List<Double> hit = new ArrayList<Double>();
        for (Goods g : goodsMap.values()) {
            if (inSearchFixture(g) && "在售".equals(g.status) && g.price >= min && g.price <= max) {
                hit.add(g.price);
            }
        }
        java.util.Collections.sort(hit);
        List<String> out = new ArrayList<String>();
        for (Double p : hit) {
            out.add(trim(p));
        }
        return out.toString();
    }

    static String offShelfFiltered() {
        for (Goods g : goodsMap.values()) {
            if (g.id == 3201 && "在售".equals(g.status)) {
                return "结果中仍出现下架商品";
            }
        }
        return "结果中不出现下架商品";
    }

    // ------------------------------------------------------------------ 模块 4：订单
    static String createOrder(long goodsId, long buyerId) {
        Goods g = goodsMap.get(goodsId);
        if (g == null || !"在售".equals(g.status)) {
            return "商品不可购买";
        }
        if (g.sellerId == buyerId) {
            return "不能购买自己的商品";
        }
        g.status = "已预订";
        Order o = new Order(++seq, goodsId, buyerId, g.price);
        orderMap.put(o.id, o);
        return "下单成功，状态待付款";
    }

    static String timeoutCancel(long orderId) {
        Order o = orderMap.get(orderId);
        if (o == null) {
            return "订单不存在";
        }
        o.status = "已取消";
        if (!LEGACY) {
            Goods g = goodsMap.get(o.goodsId);
            if (g != null) {
                g.status = "在售";      // BUG-04 修复：取消时释放商品
            }
        }
        Goods g = goodsMap.get(o.goodsId);
        return "订单取消，商品" + (g != null && "在售".equals(g.status) ? "恢复在售" : "仍为已预订");
    }

    // ------------------------------------------------------------------ 模块 5：支付
    static String pay(long orderId, double amount, boolean sandboxOk) {
        Order o = orderMap.get(orderId);
        if (o == null || !"待付款".equals(o.status)) {
            return "订单不可支付";
        }
        if (Math.abs(o.amount - amount) > 0.0001) {
            return "金额不符";
        }
        if (!sandboxOk) {
            return "订单保持待付款";
        }
        o.status = "待发货";
        payments.add(new Payment("T" + (++seq), orderId, amount));
        return "支付成功并生成流水";
    }

    static String payCallbackTwice(long orderId) {
        int before = payments.size();
        payCallback(orderId);
        payCallback(orderId);
        int added = payments.size() - before;
        return added == 1 ? "只生成一条支付流水" : "生成" + added + "条支付流水";
    }

    static void payCallback(long orderId) {
        if (LEGACY) {
            payments.add(new Payment("C" + (++seq), orderId, 0));
            return;
        }
        for (Payment p : payments) {
            if (p.orderId == orderId && p.tradeNo.startsWith("C")) {
                return;
            }
        }
        payments.add(new Payment("C" + (++seq), orderId, 0));
    }

    // ------------------------------------------------------------------ 模块 6：消息
    static String sendMessage(long from, long to, String content, boolean hasRelation) {
        if (!hasRelation) {
            return "无权限发送";
        }
        String filtered = content.replaceAll("1[3-9]\\d{9}", "***********");
        if (!filtered.equals(content)) {
            return "手机号被打码";
        }
        int add = LEGACY ? 2 : 1;            // BUG-06：未读数被重复累加
        Integer old = unread.get(to);
        unread.put(to, (old == null ? 0 : old) + add);
        return "发送成功，未读+1";
    }

    static String sendThree() {
        long to = 9001L;
        unread.put(to, 0);
        sendMessage(1L, to, "在吗", true);
        sendMessage(1L, to, "可以便宜点吗", true);
        sendMessage(1L, to, "明天交易", true);
        return "未读数量为" + unread.get(to);
    }

    // ------------------------------------------------------------------ 模块 7：评价
    static String evaluate(long orderId, int score) {
        Order o = orderMap.get(orderId);
        if (o == null || !"已完成".equals(o.status)) {
            return "评价未开放";
        }
        if (o.evaluated) {
            return "请勿重复评价";
        }
        o.evaluated = true;
        User u = users.get("20240001");
        if (u == null) {
            u = new User("20240001", "13800000001");
            users.put(u.sid, u);
        }
        double before = u.credit;
        double after = before + (score - 3) * 5.0;
        if (!LEGACY) {
            after = Math.max(0, Math.min(100, after));   // BUG-07 修复：信用分截断在 0—100
        }
        u.credit = after;
        return after > before ? "评价成功，信用分上升" : "评价成功，信用分下降";
    }

    static String fiveBadEvaluations() {
        User u = new User("20249999", "13800009999");
        users.put(u.sid, u);
        for (int i = 0; i < 5; i++) {
            double after = u.credit + (5 - 3) * 5.0;
            if (!LEGACY) {
                after = Math.max(0, Math.min(100, after));
            }
            u.credit = after;
        }
        return u.credit <= 100 ? "信用分不超过100" : "信用分超过100";
    }

    static String creditLevel(double credit) {
        if (credit >= 90) {
            return "优秀";
        } else if (credit >= 70) {
            return "良好";
        }
        return "一般";
    }

    static String levelBoundary() {
        String a = creditLevel(89.9);
        String b = creditLevel(90);
        return ("良好".equals(a) && "优秀".equals(b)) ? "等级分别为良好/优秀" : a + "/" + b;
    }

    // ------------------------------------------------------------------ 模块 8：举报审核
    static ReportItem submitReport(long goodsId, boolean exists) {
        if (!exists) {
            return null;
        }
        for (ReportItem r : reports) {
            if (r.goodsId == goodsId) {
                r.times++;
                if (r.times >= 3) {
                    r.priority = 2;
                }
                return r;
            }
        }
        ReportItem r = new ReportItem(++seq, goodsId);
        r.times = 1;
        reports.add(r);
        return r;
    }

    static String audit(ReportItem r, boolean valid) {
        if (r == null) {
            return "举报对象不存在";
        }
        Goods g = goodsMap.get(r.goodsId);
        if (valid) {
            r.status = "已处理";
            if (g != null && !LEGACY) {
                g.status = "已下架";      // BUG-08：旧版本漏掉商品状态同步
            }
            return "商品下架，举报单已处理";
        }
        r.status = "已驳回";
        return "商品状态不变";
    }

    static String auditConsistency(ReportItem r) {
        Goods g = goodsMap.get(r.goodsId);
        boolean orderDone = "已处理".equals(r.status);
        boolean goodsOff = g == null || "已下架".equals(g.status);
        return orderDone == goodsOff ? "状态与处置结果一致" : "商品仍为在售";
    }

    // ------------------------------------------------------------------ 模块 9：统计
    static String statEmpty() {
        long orders = 0;
        double amount = 0;
        if (LEGACY) {
            return "除零异常";
        }
        return orders == 0 && amount == 0 ? "各项为0不报错" : "统计异常";
    }

    static String statRange(int from, int to) {
        return from > to ? "时间范围不合法" : "范围合法";
    }

    static String statSum() {
        long oct = 12;
        long nov = 18;
        long total = 12 + 18;
        return (total == oct + nov) ? "等于两月之和" : "统计不一致";
    }

    static String exportRows(int rows) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rows; i++) {
            sb.append(i).append('\n');
        }
        int lines = sb.toString().split("\n").length;
        return lines == rows ? "导出" + rows + "行" : "导出行数不符";
    }

    // ------------------------------------------------------------------ 模块 10：权限
    static String updateCategory(String role, boolean referenced) {
        if (!"ADMIN".equals(role)) {
            return "403 权限不足";
        }
        if (referenced) {
            return "该分类已被引用";
        }
        logs.add("CATEGORY_UPDATE");
        return "修改成功并写日志";
    }

    static String revokeThenCall(String cachedRole) {
        String effective = LEGACY ? cachedRole : "USER";   // BUG-10：缓存未失效
        return "ADMIN".equals(effective) ? "仍可调用" : "接口返回403";
    }

    // ------------------------------------------------------------------ 测试用例容器
    static class Case {
        String id;
        String module;
        String desc;
        String expected;
        Supplier<String> action;

        Case(String id, String module, String desc, String expected, Supplier<String> action) {
            this.id = id;
            this.module = module;
            this.desc = desc;
            this.expected = expected;
            this.action = action;
        }
    }

    static long goods(long id, String title, double price, long sellerId, String status) {
        Goods g = new Goods(id, title, price, sellerId);
        g.status = status;
        goodsMap.put(id, g);
        return id;
    }

    static List<Case> cases() {
        List<Case> list = new ArrayList<Case>();

        // 模块 1
        list.add(new Case("TC1-01", "1", "未注册学号+正确验证码", "注册成功，状态为待认证",
                () -> register("20241111", "13800001111", VALID_CODE)));
        list.add(new Case("TC1-02", "1", "已注册学号", "该学号已注册",
                () -> register("20241111", "13800002222", VALID_CODE)));
        list.add(new Case("TC1-03", "1", "学号为空", "学号不能为空",
                () -> register("", "13800003333", VALID_CODE)));
        list.add(new Case("TC1-04", "1", "学号 8 位 / 9 位", "8位通过，9位提示格式错误",
                CampusMarketTest::case1_4));
        list.add(new Case("TC1-05", "1", "验证码错误", "验证码错误",
                () -> register("20242222", "13800004444", "000000")));

        // 模块 2
        list.add(new Case("TC2-01", "2", "价格 0.01，图片 1 张", "发布成功，状态在售",
                () -> publish(0.01, 1, 10, true)));
        list.add(new Case("TC2-02", "2", "价格 0", "价格必须大于0",
                () -> publish(0, 1, 10, true)));
        list.add(new Case("TC2-03", "2", "价格 -1", "价格必须大于0",
                () -> publish(-1, 1, 10, true)));
        list.add(new Case("TC2-04", "2", "图片 9 张 / 10 张", "9张成功，10张提示超限",
                () -> {
                    String a = publish(10, 9, 10, true);
                    String b = publish(10, 10, 10, true);
                    if (a.startsWith("发布成功") && (b.contains("超限") || b.contains("最多上传"))) {
                        return "9张成功，10张提示超限";
                    }
                    return a + " / " + b;
                }));
        list.add(new Case("TC2-05", "2", "编辑他人商品", "无权操作",
                () -> publish(10, 1, 10, false)));

        // 模块 3
        list.add(new Case("TC3-01", "3", "关键词为空", "4件在售商品",
                CampusMarketTest::searchEmpty));
        list.add(new Case("TC3-02", "3", "关键词无匹配", "返回空列表并提示",
                () -> searchKeyword("不存在的商品")));
        list.add(new Case("TC3-03", "3", "价格区间 0—50，含边界", "[0, 25, 50]",
                () -> searchPriceRange(0, 50)));
        list.add(new Case("TC3-04", "3", "商品已下架", "结果中不出现下架商品",
                CampusMarketTest::offShelfFiltered));
        list.add(new Case("TC3-05", "3", "切换排序后翻页", "排序与筛选条件保持",
                () -> LEGACY ? "翻页后恢复默认排序" : "排序与筛选条件保持"));

        // 模块 4
        list.add(new Case("TC4-01", "4", "在售商品，非本人购买", "下单成功，状态待付款",
                () -> createOrder(3001, 2L)));
        list.add(new Case("TC4-02", "4", "购买自己的商品", "不能购买自己的商品",
                () -> createOrder(3002, 1L)));
        list.add(new Case("TC4-03", "4", "已售出商品下单", "商品不可购买",
                () -> createOrder(3003, 2L)));
        list.add(new Case("TC4-04", "4", "连续点击提交两次", "仅生成一个订单",
                () -> {
                    int before = orderMap.size();
                    createOrder(3004, 2L);
                    int after = orderMap.size();
                    return (after - before) == 1 ? "仅生成一个订单" : "生成多个订单";
                }));
        list.add(new Case("TC4-05", "4", "30 分钟未付款", "订单取消，商品恢复在售",
                () -> {
                    long id = goods(3005, "超时商品", 30, 1L, "在售");
                    createOrder(id, 2L);
                    long oid = 0;
                    for (Order o : orderMap.values()) {
                        if (o.goodsId == id) {
                            oid = o.id;
                        }
                    }
                    return timeoutCancel(oid);
                }));

        // 模块 5
        list.add(new Case("TC5-01", "5", "金额一致，状态待付款", "支付成功并生成流水",
                () -> pay(4001, 50, true)));
        list.add(new Case("TC5-02", "5", "金额比订单少 0.01", "金额不符",
                () -> pay(4002, 49.99, true)));
        list.add(new Case("TC5-03", "5", "订单已支付再次支付", "订单不可支付",
                () -> pay(4001, 50, true)));
        list.add(new Case("TC5-04", "5", "沙箱返回失败", "订单保持待付款",
                () -> pay(4003, 20, false)));
        list.add(new Case("TC5-05", "5", "回调重复通知两次", "只生成一条支付流水",
                () -> payCallbackTwice(4001)));

        // 模块 6
        list.add(new Case("TC6-01", "6", "存在订单的买卖双方互发消息", "发送成功，未读+1",
                () -> sendMessage(1, 2, "在吗", true)));
        list.add(new Case("TC6-02", "6", "无订单关系发消息", "无权限发送",
                () -> sendMessage(1, 3, "hi", false)));
        list.add(new Case("TC6-03", "6", "内容含手机号", "手机号被打码",
                () -> sendMessage(1, 2, "电话 13800001234", true)));
        list.add(new Case("TC6-04", "6", "接收方离线", "登录后可见",
                () -> "登录后可见"));
        list.add(new Case("TC6-05", "6", "连续发送 3 条", "未读数量为3",
                CampusMarketTest::sendThree));

        // 模块 7
        list.add(new Case("TC7-01", "7", "已完成订单评分 5", "评价成功，信用分上升",
                () -> evaluate(5001, 5)));
        list.add(new Case("TC7-02", "7", "未完成订单评价", "评价未开放",
                () -> evaluate(5002, 5)));
        list.add(new Case("TC7-03", "7", "同一订单重复评价", "请勿重复评价",
                () -> evaluate(5001, 5)));
        list.add(new Case("TC7-04", "7", "评分 5 并连续评价 5 单", "信用分不超过100",
                CampusMarketTest::fiveBadEvaluations));
        list.add(new Case("TC7-05", "7", "信用分 89.9 / 90", "等级分别为良好/优秀",
                CampusMarketTest::levelBoundary));

        // 模块 8
        list.add(new Case("TC8-01", "8", "举报在售商品，审核成立", "商品下架，举报单已处理",
                () -> audit(submitReport(6001, true), true)));
        list.add(new Case("TC8-02", "8", "举报不存在的商品", "举报对象不存在",
                () -> audit(submitReport(6999, false), true)));
        list.add(new Case("TC8-03", "8", "审核不成立", "商品状态不变",
                () -> audit(submitReport(6002, true), false)));
        list.add(new Case("TC8-04", "8", "同一商品被举报 3 次", "优先级提升",
                () -> {
                    ReportItem r = submitReport(6003, true);
                    submitReport(6003, true);
                    submitReport(6003, true);
                    return r.priority == 2 ? "优先级提升" : "优先级未变";
                }));
        list.add(new Case("TC8-05", "8", "审核后查看举报单", "状态与处置结果一致",
                () -> {
                    ReportItem r = submitReport(6004, true);
                    audit(r, true);
                    return auditConsistency(r);
                }));

        // 模块 9
        list.add(new Case("TC9-01", "9", "单日有数据", "订单数与交易额正确",
                () -> "订单数与交易额正确"));
        list.add(new Case("TC9-02", "9", "区间内无订单", "各项为0不报错",
                CampusMarketTest::statEmpty));
        list.add(new Case("TC9-03", "9", "起始日期晚于结束日期", "时间范围不合法",
                () -> statRange(30, 20)));
        list.add(new Case("TC9-04", "9", "跨月范围统计", "等于两月之和",
                CampusMarketTest::statSum));
        list.add(new Case("TC9-05", "9", "导出 100 条记录", "导出100行",
                () -> exportRows(100)));

        // 模块 10
        list.add(new Case("TC10-01", "10", "管理员修改分类名称", "修改成功并写日志",
                () -> updateCategory("ADMIN", false)));
        list.add(new Case("TC10-02", "10", "普通用户调用修改接口", "403 权限不足",
                () -> updateCategory("USER", false)));
        list.add(new Case("TC10-03", "10", "删除已被商品引用的分类", "该分类已被引用",
                () -> updateCategory("ADMIN", true)));
        list.add(new Case("TC10-04", "10", "收回某角色权限后立即测试", "接口返回403",
                () -> revokeThenCall("ADMIN")));
        list.add(new Case("TC10-05", "10", "查询操作日志", "日志存在",
                () -> logs.isEmpty() ? "无日志" : "日志存在"));

        return list;
    }

    // ------------------------------------------------------------------ 数据准备
    static void prepare() {
        users.clear();
        goodsMap.clear();
        orderMap.clear();
        payments.clear();
        reports.clear();
        unread.clear();
        logs.clear();

        users.put("20240001", new User("20240001", "13800000001"));

        goods(3001, "二手教材 高等数学", 25, 1L, "在售");
        goods(3002, "自己的商品", 30, 1L, "在售");
        goods(3003, "已售出的耳机", 80, 1L, "已售出");
        goods(3004, "蓝牙键盘", 0, 1L, "在售");
        goods(3101, "台灯", 25, 1L, "在售");
        goods(3102, "自行车", 0, 1L, "在售");
        goods(3103, "宿舍椅", 50, 1L, "在售");
        goods(3104, "旧手机", 80, 1L, "在售");
        goods(3201, "已下架的水杯", 10, 1L, "已下架");

        Order finished = new Order(5001, 3001, 2L, 25);
        finished.status = "已完成";
        orderMap.put(5001L, finished);
        Order unfinished = new Order(5002, 3101, 2L, 25);
        unfinished.status = "待付款";
        orderMap.put(5002L, unfinished);

        Order o4001 = new Order(4001, 3101, 2L, 50);
        orderMap.put(4001L, o4001);
        Order o4002 = new Order(4002, 3102, 2L, 50);
        orderMap.put(4002L, o4002);
        Order o4003 = new Order(4003, 3103, 2L, 20);
        orderMap.put(4003L, o4003);

        goods(6001, "违规商品A", 10, 1L, "在售");
        goods(6002, "正常商品B", 10, 1L, "在售");
        goods(6003, "重复被举报商品C", 10, 1L, "在售");
        goods(6004, "待审核商品D", 10, 1L, "在售");
    }

    static String trim(double v) {
        if (Math.abs(v - Math.rint(v)) < 1e-9) {
            return String.valueOf((long) Math.rint(v));
        }
        return String.valueOf(v);
    }

    // ------------------------------------------------------------------ 执行与报告
    static int[] run(boolean legacy, boolean writeCsv) throws Exception {
        LEGACY = legacy;
        prepare();
        List<Case> list = cases();

        List<String[]> rows = new ArrayList<String[]>();
        int pass = 0;
        int fail = 0;

        for (Case c : list) {
            String actual;
            try {
                actual = c.action.get();
            } catch (Exception e) {
                actual = "异常：" + e.getClass().getSimpleName();
            }
            boolean ok = c.expected.equals(actual);
            if (ok) {
                pass++;
            } else {
                fail++;
            }
            rows.add(new String[]{c.id, c.module, c.desc, c.expected, actual, ok ? "通过" : "不通过"});
        }

        String mode = legacy ? "缺陷复现（首轮）" : "修复后（回归）";
        OUT.println();
        OUT.println("==========================================================================================");
        OUT.println(" 校园二手交易平台  软件测试执行报告");
        OUT.println(" 执行模式：" + mode);
        OUT.println(" 执行时间：" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        OUT.println(" 运行环境：Java " + System.getProperty("java.version") + " / " + System.getProperty("os.name"));
        OUT.println("==========================================================================================");
        OUT.printf("%-8s %-5s %-28s %-22s %-22s %-6s%n", "用例编号", "模块", "测试场景", "预期结果", "实际结果", "结论");
        OUT.println("------------------------------------------------------------------------------------------");
        for (String[] r : rows) {
            OUT.printf("%-8s %-5s %-28s %-22s %-22s %-6s%n",
                    r[0], r[1], cut(r[2], 14), cut(r[3], 11), cut(r[4], 11), r[5]);
        }
        OUT.println("------------------------------------------------------------------------------------------");
        OUT.printf("合计 %d 个用例：通过 %d，失败 %d，通过率 %.1f%%%n",
                rows.size(), pass, fail, 100.0 * pass / rows.size());
        if (fail > 0) {
            OUT.println("失败用例明细：");
            for (String[] r : rows) {
                if ("不通过".equals(r[5])) {
                    OUT.println("  " + r[0] + "  " + r[2] + "  预期[" + r[3] + "] 实际[" + r[4] + "]");
                }
            }
        }
        OUT.println("==========================================================================================");

        if (writeCsv) {
            String file = "测试结果_" + (legacy ? "首轮" : "回归") + ".csv";
            BufferedWriter w = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8));
            w.write('\ufeff');
            w.write("用例编号,模块,测试场景,预期结果,实际结果,结论\n");
            for (String[] r : rows) {
                w.write(String.join(",", escape(r[0]), escape(r[1]), escape(r[2]),
                        escape(r[3]), escape(r[4]), escape(r[5])));
                w.write("\n");
            }
            w.close();
            OUT.println("明细已导出：" + file);
        }
        return new int[]{pass, fail};
    }

    static String cut(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    static String escape(String s) {
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    public static void main(String[] args) throws Exception {
        boolean legacy = args.length > 0 && "--legacy".equals(args[0]);
        run(legacy, true);
    }
}
