package com.rural.travel.guide.dto;

import lombok.Data;

/** S6 反馈闭环 */
@Data
public class GuideFeedbackDTO {
    private Long entityId;
    private Long contentId;
    /** 1~5 */
    private Integer score;
    private String comment;
}
