package com.viscriptshop.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * 提供 VSS 货币金额的校验、运算和显示格式化功能。
 *
 * <p>货币值使用非负且有限的 {@code double}。运算时使用操作数的十进制表示，避免
 * {@code 0.1} 等常见价格在乘法和累加后因二进制浮点误差导致余额比较失败。
 */
public final class MoneyUtil {
    private static final BigDecimal MAX_VALUE = BigDecimal.valueOf(Double.MAX_VALUE);

    private MoneyUtil() {
    }

    /**
     * 判断金额是否为有限正数。
     *
     * @param amount 待校验的货币金额
     * @return 金额可参与货币运算时返回 {@code true}
     */
    public static boolean isPositive(double amount) {
        return Double.isFinite(amount) && amount > 0;
    }

    /**
     * 将金额规范化为可保存的余额。
     *
     * <p>负数、非数字和无穷值均转换为零。
     *
     * @param amount 待规范化的货币金额
     * @return 非负且有限的余额
     */
    public static double normalize(double amount) {
        return Double.isFinite(amount) && amount > 0 ? amount : 0;
    }

    /**
     * 将两个非负金额相加，并在溢出时饱和到 {@link Double#MAX_VALUE}。
     *
     * @param left 第一个货币金额
     * @param right 第二个货币金额
     * @return 规范化后的总额
     */
    public static double add(double left, double right) {
        return toDouble(decimal(left).add(decimal(right)));
    }

    /**
     * 将货币单价乘以非负数量，并在溢出时饱和。
     *
     * @param amount 单位货币金额
     * @param count 数量倍数
     * @return 规范化后的乘积；任一参数非正时返回零
     */
    public static double multiply(double amount, long count) {
        if (!isPositive(amount) || count <= 0) {
            return 0;
        }
        return toDouble(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(count)));
    }

    /**
     * 使用非负十进制倍率计算金额，并在溢出时饱和。
     *
     * <p>该重载用于折扣率，避免先执行二进制浮点乘法而把无意义的小数尾数写入余额。
     *
     * @param amount 原始金额
     * @param factor 非负倍率
     * @return 规范化后的金额；金额或倍率非正时返回零
     */
    public static double multiply(double amount, double factor) {
        if (!isPositive(amount) || !Double.isFinite(factor) || factor <= 0) {
            return 0;
        }
        return toDouble(BigDecimal.valueOf(amount).multiply(BigDecimal.valueOf(factor)));
    }

    /**
     * 从余额中扣除金额，并阻止结果变为负数。
     *
     * @param balance 可用余额
     * @param amount 待扣除的金额
     * @return 扣除后的非负余额
     */
    public static double subtract(double balance, double amount) {
        BigDecimal result = decimal(balance).subtract(decimal(amount));
        return result.signum() <= 0 ? 0 : toDouble(result);
    }

    /**
     * 使用十进制数值比较余额能否支付指定金额。
     *
     * @param balance 可用余额
     * @param amount 所需金额
     * @return 规范化余额不少于规范化金额时返回 {@code true}
     */
    public static boolean hasEnough(double balance, double amount) {
        return decimal(balance).compareTo(decimal(amount)) >= 0;
    }

    /**
     * 格式化货币金额，并移除仅由零组成的小数尾部。
     *
     * @param amount 待格式化的货币金额
     * @return 规范化金额的完整十进制表示
     */
    public static String format(double amount) {
        return decimal(amount).stripTrailingZeros().toPlainString();
    }

    /**
     * 将货币金额格式化为适合 UI 标签的紧凑形式。
     *
     * <p>小于一千的金额保留完整十进制表示；更大的金额使用 {@code k}、{@code m}、
     * {@code b}、{@code t} 和 {@code q} 后缀，超出该范围时使用科学计数法。需要显示完整值的
     * 提示文本应使用 {@link #format(double)}。
     *
     * @param amount 待格式化的货币金额
     * @return 规范化金额的紧凑表示
     */
    public static String formatCompact(double amount) {
        double normalized = normalize(amount);
        if (normalized < 1_000) {
            return format(normalized);
        }
        if (normalized >= 1_000_000_000_000_000_000D) {
            return String.format(Locale.ROOT, "%.1e", normalized).replace(".0e", "e");
        }

        String[] suffixes = {"", "k", "m", "b", "t", "q"};
        int suffix = 0;
        double scaled = normalized;
        while (scaled >= 1_000 && suffix < suffixes.length - 1) {
            scaled /= 1_000;
            suffix++;
        }
        BigDecimal rounded = BigDecimal.valueOf(scaled).setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros();
        return rounded.toPlainString() + suffixes[suffix];
    }

    private static BigDecimal decimal(double amount) {
        return BigDecimal.valueOf(normalize(amount));
    }

    private static double toDouble(BigDecimal amount) {
        if (amount.signum() <= 0) {
            return 0;
        }
        if (amount.compareTo(MAX_VALUE) >= 0) {
            return Double.MAX_VALUE;
        }
        return amount.doubleValue();
    }
}
