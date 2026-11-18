package com.rural.travel.guide;

import com.rural.travel.common.Result;
import com.rural.travel.guide.dto.*;
import com.rural.travel.guide.entity.*;
import com.rural.travel.guide.vo.GraphVO;
import com.rural.travel.guide.vo.GuideAskVO;
import com.rural.travel.guide.vo.GuideTriggerVO;
import com.rural.travel.guide.vo.RoutePlanVO;
import com.rural.travel.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 方案三 游客端接口：知识图谱(S1) / 多模态内容(S2) / 画像(S3) / 路线规划(S4) / 智能导览(S5) / 反馈闭环(S6)
 * 游客可访问的接口已在 SecurityConfig 放行，未登录时 userId 为 null，走默认画像、路线不落库。
 */
@RestController
@RequestMapping("/api/guide")
@RequiredArgsConstructor
public class GuideController {

    private final KgService kgService;
    private final VisitorProfileService profileService;
    private final RouteService routeService;
    private final MultimodalContentService contentService;
    private final GuideService guideService;
    private final FeedbackService feedbackService;

    // ---------------- S1 知识图谱 ----------------

    /** 县域列表 */
    @GetMapping("/counties")
    public Result<List<KgCounty>> counties() {
        return Result.ok(kgService.counties());
    }

    /** 知识图谱（节点+关系+统计） */
    @GetMapping("/graph")
    public Result<GraphVO> graph(@RequestParam(required = false) String countyCode) {
        return Result.ok(kgService.graph(countyCode));
    }

    /** 实体列表 */
    @GetMapping("/entities")
    public Result<List<KgEntity>> entities(@RequestParam(required = false) String countyCode,
                                           @RequestParam(required = false) String entityType) {
        return Result.ok(kgService.list(countyCode, entityType));
    }

    /** 实体详情 + 已生成的多模态内容 */
    @GetMapping("/entities/{id}")
    public Result<Map<String, Object>> entityDetail(@PathVariable Long id) {
        KgEntity e = kgService.get(id);
        if (e == null) return Result.fail(404, "资源不存在");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("entity", e);
        m.put("contents", contentService.ofEntity(id));
        m.put("typeName", MultimodalContentService.typeName(e.getEntityType()));
        return Result.ok(m);
    }

    // ---------------- S3 游客画像 ----------------

    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        VisitorProfile p = profileService.getOrCreate(SecurityUtils.currentUserId());
        Map<String, Object> m = profileService.summary(p);
        m.put("userId", p.getUserId());
        return Result.ok(m);
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> saveProfile(@RequestBody VisitorProfileDTO dto) {
        VisitorProfile p = profileService.save(SecurityUtils.currentUserId(), dto);
        return Result.ok(profileService.summary(p));
    }

    /** 依据浏览/收藏/评分行为推断兴趣 */
    @PostMapping("/profile/infer")
    public Result<Map<String, Object>> infer() {
        VisitorProfile p = profileService.infer(SecurityUtils.currentUserId());
        return Result.ok(profileService.summary(p));
    }

    // ---------------- S4 路线规划 ----------------

    @PostMapping("/route/plan")
    public Result<RoutePlanVO> plan(@RequestBody(required = false) RoutePlanDTO dto) {
        RoutePlanDTO d = dto == null ? new RoutePlanDTO() : dto;
        return Result.ok(routeService.plan(SecurityUtils.currentUserIdOrNull(), d));
    }

    @GetMapping("/routes")
    public Result<List<GuideRoute>> myRoutes() {
        return Result.ok(routeService.myRoutes(SecurityUtils.currentUserId()));
    }

    @GetMapping("/routes/{id}")
    public Result<RoutePlanVO> routeDetail(@PathVariable Long id) {
        RoutePlanVO vo = routeService.routeDetail(id);
        return vo == null ? Result.fail(404, "路线不存在") : Result.ok(vo);
    }

    // ---------------- S5 智能导览 ----------------

    /** 位置触发自适应讲解 */
    @PostMapping("/trigger")
    public Result<GuideTriggerVO> trigger(@RequestBody GuideTriggerDTO dto) {
        return Result.ok(guideService.trigger(SecurityUtils.currentUserIdOrNull(), dto));
    }

    /** 语音问答 */
    @PostMapping("/ask")
    public Result<GuideAskVO> ask(@RequestBody GuideAskDTO dto) {
        return Result.ok(guideService.ask(SecurityUtils.currentUserIdOrNull(), dto));
    }

    /** 回传停留时长，驱动式(3) 的 r_u */
    @PostMapping("/session/{id}/touch")
    public Result<Boolean> touch(@PathVariable Long id, @RequestParam Integer dwellSeconds) {
        guideService.touch(id, dwellSeconds);
        return Result.ok(true);
    }

    @GetMapping("/sessions")
    public Result<List<GuideSession>> mySessions(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(guideService.mySessions(SecurityUtils.currentUserId(), limit));
    }

    // ---------------- S6 反馈闭环 ----------------

    @PostMapping("/feedback")
    public Result<Map<String, Object>> feedback(@RequestBody GuideFeedbackDTO dto) {
        return Result.ok(feedbackService.submit(SecurityUtils.currentUserId(), dto));
    }

    @GetMapping("/feedback/my")
    public Result<List<GuideFeedback>> myFeedback() {
        return Result.ok(feedbackService.myFeedback(SecurityUtils.currentUserId()));
    }
}
