package com.rural.travel.guide.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 多模态数字内容资源：文案/语音稿/图像/视频脚本 × 风格 × 方言 × 深度档 */
@Data
@TableName("mm_content")
public class MmContent {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long entityId;
    /** TEXT文案 / AUDIO语音稿 / IMAGE图像 / VIDEO视频脚本 */
    private String contentType;
    /** 清新 / 活泼 / 文艺 */
    private String style;
    /** 普通话 / 方言 */
    private String dialect;
    /** 讲解深度档 1简约 2标准 3深度,式(3) */
    private Integer depthLevel;
    private String title;
    private String body;
    private String mediaUrl;
    private Integer durationSec;
    /** AI增强 / 本地模板 */
    private String engine;
    private Integer auditStatus;
    private Integer exposure;
    private Integer playCount;
    private Integer finishCount;
    private Integer scoreSum;
    private Integer scoreCount;
    /** 满意度 EMA,式(4) 反馈侧 */
    private Double scoreEma;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ---- 非持久化扩展字段 ----
    @TableField(exist = false)
    private String entityName;
    @TableField(exist = false)
    private String entityType;
}
