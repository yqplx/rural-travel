package com.rural.travel.guide.vo;

import lombok.Data;

import java.util.List;

/** S2 批量内容生产结果 */
@Data
public class ContentProduceVO {
    private Long entityId;
    private String entityName;
    private Integer produced;
    private Integer skipped;
    /** AI增强 / 本地模板 */
    private String engine;
    /** 生产前后的内容完备度 g_i */
    private Double contentScoreBefore;
    private Double contentScoreAfter;
    private List<Long> contentIds;
}
