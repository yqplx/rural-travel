package com.rural.travel.guide.dto;

import lombok.Data;

import java.util.List;

/** 管理端：批量多模态内容生产（S2 低成本内容量产） */
@Data
public class ContentBatchDTO {
    private Long entityId;
    /** TEXT / AUDIO / IMAGE / VIDEO，留空表示全模态 */
    private List<String> contentTypes;
    /** 清新 / 活泼 / 文艺，留空表示三风格全出 */
    private List<String> styles;
    /** 普通话 / 方言，留空表示两种都出 */
    private List<String> dialects;
    /** 是否调用用户自有大模型增强，失败自动降级本地模板 */
    private Boolean useAi = true;
}
