package com.rural.travel.guide;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.rural.travel.guide.dto.KgEntitySaveDTO;
import com.rural.travel.guide.dto.KgRelationSaveDTO;
import com.rural.travel.guide.entity.KgCounty;
import com.rural.travel.guide.entity.KgEntity;
import com.rural.travel.guide.entity.KgRelation;
import com.rural.travel.guide.entity.MmContent;
import com.rural.travel.guide.mapper.KgCountyMapper;
import com.rural.travel.guide.mapper.KgEntityMapper;
import com.rural.travel.guide.mapper.KgRelationMapper;
import com.rural.travel.guide.mapper.MmContentMapper;
import com.rural.travel.guide.vo.GraphVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * S1 县域文旅资源知识图谱：实体与关系的构建、检索与维护。
 * 同时负责维护实体的"内容完备度 g_i"（式(1) 的一项输入）。
 */
@Service
@RequiredArgsConstructor
public class KgService {

    private final KgCountyMapper countyMapper;
    private final KgEntityMapper entityMapper;
    private final KgRelationMapper relationMapper;
    private final MmContentMapper contentMapper;

    /** 默认县域：取第一个启用县域，无数据时回落到内置演示县域 */
    public String defaultCounty() {
        List<KgCounty> list = countyMapper.selectList(
                new LambdaQueryWrapper<KgCounty>().eq(KgCounty::getStatus, 1).orderByAsc(KgCounty::getCode));
        if (!list.isEmpty()) return list.get(0).getCode();
        return "DEMO01";
    }

    public List<KgCounty> counties() {
        return countyMapper.selectList(
                new LambdaQueryWrapper<KgCounty>().eq(KgCounty::getStatus, 1).orderByAsc(KgCounty::getCode));
    }

    public List<KgEntity> list(String countyCode, String entityType) {
        return entityMapper.selectList(new LambdaQueryWrapper<KgEntity>()
                .eq(StringUtils.hasText(countyCode), KgEntity::getCountyCode, countyCode)
                .eq(StringUtils.hasText(entityType), KgEntity::getEntityType, entityType)
                .eq(KgEntity::getStatus, 1)
                .orderByDesc(KgEntity::getHeat));
    }

    /** 路线规划用：仅取有坐标的实体，保证式(2) 的接驳时长可算 */
    public List<KgEntity> listRoutable(String countyCode, Collection<String> entityTypes) {
        LambdaQueryWrapper<KgEntity> w = new LambdaQueryWrapper<KgEntity>()
                .eq(StringUtils.hasText(countyCode), KgEntity::getCountyCode, countyCode)
                .eq(KgEntity::getStatus, 1)
                .isNotNull(KgEntity::getLng)
                .isNotNull(KgEntity::getLat);
        if (entityTypes != null && !entityTypes.isEmpty()) {
            w.in(KgEntity::getEntityType, entityTypes);
        }
        return entityMapper.selectList(w);
    }

    public KgEntity get(Long id) {
        KgEntity e = entityMapper.selectById(id);
        if (e != null) fillTags(e);
        return e;
    }

    /** 管理端分页查询 */
    public IPage<KgEntity> page(IPage<KgEntity> page, LambdaQueryWrapper<KgEntity> wrapper) {
        return entityMapper.selectPage(page, wrapper);
    }

    /** 图谱可视化：实体为点、关系为边 */
    public GraphVO graph(String countyCode) {
        String code = StringUtils.hasText(countyCode) ? countyCode : defaultCounty();
        List<KgEntity> entities = list(code, null);
        List<Long> ids = entities.stream().map(KgEntity::getId).toList();

        Map<Long, Integer> degree = new HashMap<>();
        List<GraphVO.GraphEdge> edges = new ArrayList<>();
        if (!ids.isEmpty()) {
            List<KgRelation> relations = relationMapper.selectList(
                    new LambdaQueryWrapper<KgRelation>()
                            .in(KgRelation::getSourceId, ids)
                            .in(KgRelation::getTargetId, ids)
                            .eq(KgRelation::getStatus, 1));
            Set<Long> idSet = new HashSet<>(ids);
            for (KgRelation r : relations) {
                if (!idSet.contains(r.getSourceId()) || !idSet.contains(r.getTargetId())) continue;
                GraphVO.GraphEdge e = new GraphVO.GraphEdge();
                e.setId(r.getId());
                e.setSource(r.getSourceId());
                e.setTarget(r.getTargetId());
                e.setRelationType(r.getRelationType());
                e.setWeight(r.getWeight());
                e.setWalkMinutes(r.getWalkMinutes());
                edges.add(e);
                degree.merge(r.getSourceId(), 1, Integer::sum);
                degree.merge(r.getTargetId(), 1, Integer::sum);
            }
        }

        List<GraphVO.GraphNode> nodes = entities.stream().map(e -> {
            GraphVO.GraphNode n = new GraphVO.GraphNode();
            n.setId(e.getId());
            n.setName(e.getName());
            n.setEntityType(e.getEntityType());
            n.setSummary(e.getSummary());
            n.setCoverImage(e.getCoverImage());
            n.setLng(e.getLng());
            n.setLat(e.getLat());
            n.setTags(splitTags(e.getTags()));
            n.setHeat(e.getHeat());
            n.setCrowd(e.getCrowd());
            n.setContentScore(e.getContentScore());
            n.setVisitMinutes(e.getVisitMinutes());
            n.setDegree(degree.getOrDefault(e.getId(), 0));
            return n;
        }).toList();

        GraphVO.GraphStat stat = new GraphVO.GraphStat();
        stat.setEntityCount(entities.size());
        stat.setRelationCount(edges.size());
        stat.setScenicCount((int) entities.stream().filter(e -> "SCENIC".equals(e.getEntityType())).count());
        stat.setHeritageCount((int) entities.stream().filter(e -> "HERITAGE".equals(e.getEntityType())).count());
        stat.setFoodCount((int) entities.stream().filter(e -> "FOOD".equals(e.getEntityType())).count());
        long withContent = entities.stream().filter(e -> e.getContentScore() != null && e.getContentScore() > 0).count();
        stat.setContentCoverage(entities.isEmpty() ? 0d : (double) withContent / entities.size());

        GraphVO vo = new GraphVO();
        vo.setCounties(counties());
        vo.setNodes(nodes);
        vo.setEdges(edges);
        vo.setStat(stat);
        return vo;
    }

    // ---------------- 维护 ----------------

    public KgEntity save(KgEntitySaveDTO dto) {
        KgEntity e = new KgEntity();
        if (dto.getId() != null) {
            KgEntity old = entityMapper.selectById(dto.getId());
            if (old != null) e = old;
        }
        e.setCountyCode(StringUtils.hasText(dto.getCountyCode()) ? dto.getCountyCode() : defaultCounty());
        e.setEntityType(StringUtils.hasText(dto.getEntityType()) ? dto.getEntityType() : "SCENIC");
        e.setRefId(dto.getRefId());
        e.setName(dto.getName());
        e.setSummary(dto.getSummary());
        e.setDetail(dto.getDetail());
        e.setCoverImage(dto.getCoverImage());
        e.setLng(dto.getLng());
        e.setLat(dto.getLat());
        e.setTags(dto.getTags() == null ? "" : String.join(",", dto.getTags()));
        e.setVisitMinutes(dto.getVisitMinutes() == null ? 30 : dto.getVisitMinutes());
        e.setCrowd(dto.getCrowd() == null ? 0.3 : dto.getCrowd());
        e.setHeat(dto.getHeat() == null ? 0.5 : dto.getHeat());
        e.setOpenTime(dto.getOpenTime());
        e.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (e.getId() == null) {
            entityMapper.insert(e);
        } else {
            entityMapper.updateById(e);
        }
        return e;
    }

    public void remove(Long id) {
        entityMapper.deleteById(id);
        relationMapper.delete(new LambdaQueryWrapper<KgRelation>()
                .eq(KgRelation::getSourceId, id).or().eq(KgRelation::getTargetId, id));
    }

    public KgRelation saveRelation(KgRelationSaveDTO dto) {
        KgRelation r = new KgRelation();
        r.setId(dto.getId());
        r.setSourceId(dto.getSourceId());
        r.setTargetId(dto.getTargetId());
        r.setRelationType(StringUtils.hasText(dto.getRelationType()) ? dto.getRelationType() : "NEARBY");
        r.setWeight(dto.getWeight() == null ? 1d : dto.getWeight());
        r.setWalkMinutes(dto.getWalkMinutes() == null ? 0 : dto.getWalkMinutes());
        r.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        if (r.getId() == null) {
            relationMapper.insert(r);
        } else {
            relationMapper.updateById(r);
        }
        return r;
    }

    public void removeRelation(Long id) {
        relationMapper.deleteById(id);
    }

    public List<KgRelation> relations(String countyCode) {
        List<KgEntity> entities = list(countyCode, null);
        List<Long> ids = entities.stream().map(KgEntity::getId).toList();
        if (ids.isEmpty()) return List.of();
        return relationMapper.selectList(new LambdaQueryWrapper<KgRelation>()
                .in(KgRelation::getSourceId, ids)
                .in(KgRelation::getTargetId, ids)
                .eq(KgRelation::getStatus, 1));
    }

    // ---------------- 内容完备度 g_i ----------------

    /**
     * 重算实体的内容完备度 g_i = 已覆盖模态数 / 目标模态数(4)。
     * 内容生产后调用，使式(1) 能反映"内容家底"。
     */
    public double recalcContentScore(Long entityId) {
        List<MmContent> contents = contentMapper.selectList(new LambdaQueryWrapper<MmContent>()
                .eq(MmContent::getEntityId, entityId)
                .eq(MmContent::getAuditStatus, 1));
        long modalities = contents.stream().map(MmContent::getContentType).filter(StringUtils::hasText)
                .distinct().count();
        double g = GuideFormulas.contentCompleteness((int) modalities);
        KgEntity e = entityMapper.selectById(entityId);
        if (e != null) {
            e.setContentScore(g);
            entityMapper.updateById(e);
        }
        return g;
    }

    /** 接驳时长索引：sourceId -> (targetId -> 分钟)，双向可读 */
    public Map<Long, Map<Long, Integer>> transferIndex(String countyCode) {
        Map<Long, Map<Long, Integer>> idx = new HashMap<>();
        for (KgRelation r : relations(countyCode)) {
            if (r.getWalkMinutes() == null || r.getWalkMinutes() <= 0) continue;
            idx.computeIfAbsent(r.getSourceId(), k -> new HashMap<>()).put(r.getTargetId(), r.getWalkMinutes());
            idx.computeIfAbsent(r.getTargetId(), k -> new HashMap<>()).put(r.getSourceId(), r.getWalkMinutes());
        }
        return idx;
    }

    // ---------------- 工具 ----------------

    public static List<String> splitTags(String tags) {
        if (!StringUtils.hasText(tags)) return List.of();
        return Arrays.stream(tags.split("[,，、]")).map(String::trim)
                .filter(StringUtils::hasText).distinct().collect(Collectors.toList());
    }

    public static void fillTags(KgEntity e) {
        if (e != null) e.setTagList(splitTags(e.getTags()));
    }
}
