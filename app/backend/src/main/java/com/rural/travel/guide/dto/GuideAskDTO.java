package com.rural.travel.guide.dto;

import lombok.Data;

/** S5 语音问答（前端已完成 ASR，此处接收文本） */
@Data
public class GuideAskDTO {
    private String question;
    private Long entityId;
    private Long sessionId;
    /** 回答所用方言：普通话 / 方言 */
    private String dialect;
}
