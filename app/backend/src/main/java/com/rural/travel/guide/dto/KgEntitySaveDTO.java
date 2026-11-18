package com.rural.travel.guide.dto;

import lombok.Data;

import java.util.List;

/** 管理端：知识图谱实体维护 */
@Data
public class KgEntitySaveDTO {
    private Long id;
    private String countyCode;
    /** SCENIC / HERITAGE / FOOD / VILLAGE / PERSON / CRAFT */
    private String entityType;
    private Long refId;
    private String name;
    private String summary;
    private String detail;
    private String coverImage;
    private Double lng;
    private Double lat;
    private List<String> tags;
    private Integer visitMinutes;
    private Double crowd;
    private Double heat;
    private String openTime;
    private Integer status;
}
