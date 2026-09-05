package com.viscriptshop.promotion.condition;

import com.lowdragmc.lowdraglib2.configurator.ui.ConfiguratorGroup;
import com.lowdragmc.lowdraglib2.configurator.ui.StringConfigurator;
import com.lowdragmc.lowdraglib2.registry.annotation.LDLRegister;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.viscriptshop.promotion.PromotionContext;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

/** 每日时钟区间；底层保留旧版 tick 字段，已有商店无需迁移。 */
@Getter
@Setter
@LDLRegister(name = GameTimeCondition.ID, registry = PromotionCondition.REGISTRY, priority = 200)
public class GameTimeCondition implements PromotionCondition {
    public static final String ID = "viscript_shop:game_time";

    @Persisted
    private int startTime = 18000;
    @Persisted
    private int endTime = 17999;

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        StringConfigurator start = new StringConfigurator(
                "viscript_shop.data.promotion.condition.game_time_start",
                () -> formatTime(startTime), value -> startTime = parseTime(value, false), "00:00", true);
        StringConfigurator end = new StringConfigurator(
                "viscript_shop.data.promotion.condition.game_time_end",
                () -> formatTime(endTime), value -> endTime = parseTime(value, true), "23:59", true);
        start.setTextValidator(GameTimeCondition::isValidTime);
        end.setTextValidator(GameTimeCondition::isValidTime);
        start.setId("promotion_time_start");
        end.setId("promotion_time_end");
        start.textField.setId("promotion_time_start_input");
        end.textField.setId("promotion_time_end_input");
        start.setTips("viscript_shop.data.promotion.condition.game_time.tip");
        end.setTips("viscript_shop.data.promotion.condition.game_time.tip");
        father.addConfigurators(start, end);
    }

    public static boolean isValidTime(String value) {
        return value != null && value.matches("(?:[01]?\\d|2[0-3]):[0-5]\\d");
    }

    /** 与泰拉饰品时钟一致：世界 tick 0 是 06:00，每 1000 tick 一小时。 */
    public static String formatTime(long ticks) {
        long minute = Math.floorMod(ticks, 24000L) * 60 / 1000;
        minute = (minute + 360) % 1440;
        return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
    }

    /** 包含结束分钟，避免 02:30 只匹配到这一分钟的第一个 tick。 */
    public static int parseTime(String value, boolean endOfMinute) {
        if (!isValidTime(value)) throw new IllegalArgumentException("Invalid clock time: " + value);
        String[] parts = value.split(":");
        int minute = Math.floorMod(Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]) - 360, 1440);
        return endOfMinute ? ((minute + 1) * 1000 + 59) / 60 - 1 : (minute * 1000 + 59) / 60;
    }

    public boolean containsTime(long dayTime) {
        long time = Math.floorMod(dayTime, 24000L);
        int start = Math.floorMod(startTime, 24000);
        int end = Math.floorMod(endTime, 24000);
        return start <= end ? time >= start && time <= end : time >= start || time <= end;
    }

    @Override
    public boolean test(PromotionContext context) {
        return context.player() != null && containsTime(context.player().level().getDayTime());
    }
}
