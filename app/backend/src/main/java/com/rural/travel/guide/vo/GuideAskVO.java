package com.rural.travel.guide.vo;

import lombok.Data;

import java.util.List;

/** S5 语音问答结果 */
@Data
public class GuideAskVO {
    private String question;
    private String answer;
    /** 命中知识图谱的实体，供前端高亮 */
    private Long entityId;
    private String entityName;
    /** 检索到的知识条目 */
    private List<String> references;
    /** AI增强 / 知识库 / 本地模板 */
    private String engine;
    /** 追问建议 */
    private List<String> followUps;
}
