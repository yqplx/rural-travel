package com.rural.travel.guide.vo;

import com.rural.travel.guide.entity.KgCounty;
import lombok.Data;

import java.util.List;

/** S1 知识图谱可视化数据 */
@Data
public class GraphVO {
    private List<KgCounty> counties;
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;
    /** 统计数据，供前端概览条使用 */
    private GraphStat stat;

    @Data
    public static class GraphNode {
        private Long id;
        private String name;
        private String entityType;
        private String summary;
        private String coverImage;
        private Double lng;
        private Double lat;
        private List<String> tags;
        private Double heat;
        private Double crowd;
        private Double contentScore;
        private Integer visitMinutes;
        private Integer degree;
    }

    @Data
    public static class GraphEdge {
        private Long id;
        private Long source;
        private Long target;
        private String relationType;
        private Double weight;
        private Integer walkMinutes;
    }

    @Data
    public static class GraphStat {
        private Integer entityCount;
        private Integer relationCount;
        private Integer scenicCount;
        private Integer heritageCount;
        private Integer foodCount;
        /** 已生成内容的实体占比 */
        private Double contentCoverage;
    }
}
