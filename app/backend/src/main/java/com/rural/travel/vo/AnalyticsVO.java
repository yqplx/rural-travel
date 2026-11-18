package com.rural.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** 数据分析看板 */
@Data
public class AnalyticsVO {
    /** 近30天日客流 [{date,count,level}] */
    private List<DailyHeat> dailyHeat;
    /** 24小时分布 [24] */
    private List<Integer> hourDist;
    /** 分类热度 [{name,browse,rating}] */
    private List<CategoryHeat> categoryHeat;
    /** 客源省份 Top10 [{province,count}] */
    private List<Map<String, Object>> provinceTop;
    /** 价格偏好 [{label,count}] */
    private List<Map<String, Object>> priceDist;
    /** 标签热度 Top10 [{name,count}] */
    private List<Map<String, Object>> tagHeat;
    /** 周末客流占比(%) */
    private Double weekendRatio;
    /** 运营建议 */
    private List<Suggestion> suggestions;
    /** AI 运营总述(模板或LLM润色) */
    private String summary;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class DailyHeat {
        private String date;
        private Integer count;
        private Integer level;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class CategoryHeat {
        private String name;
        private Integer browse;
        private Double rating;
    }

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class Suggestion {
        private String title;
        private String detail;
    }
}
