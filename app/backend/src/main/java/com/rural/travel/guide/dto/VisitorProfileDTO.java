package com.rural.travel.guide.dto;

import lombok.Data;

import java.util.List;

/** S3 游客画像保存 */
@Data
public class VisitorProfileDTO {
    /** 青年 / 中年 / 银发 / 亲子 */
    private String ageBand;
    /** solo / couple / family / friends / team */
    private String groupType;
    private List<String> interests;
    /** slow / normal / fast */
    private String pace;
    /** 普通话 / 方言 */
    private String preferDialect;
    /** 式(3) 的 k_u 知识背景水平，1~3，决定讲解起步深度 */
    private Integer depthLevel;
    /** 计划游览时长 T_u(分钟) */
    private Integer planMinutes;
    private Integer avoidCrowd;
}
