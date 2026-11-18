package com.rural.travel.ai;

import com.rural.travel.entity.ScenicSpot;
import com.rural.travel.service.ScenicSpotService;
import com.rural.travel.vo.VoiceGuideVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/** 语音导游:按景区内容生成结构化分段讲解稿,前端用 Web Speech API 播报 */
@Service
public class VoiceGuideService {

    private final ScenicSpotService scenicSpotService;

    public VoiceGuideService(ScenicSpotService scenicSpotService) {
        this.scenicSpotService = scenicSpotService;
    }

    public VoiceGuideVO guide(Long spotId) {
        ScenicSpot s = scenicSpotService.getById(spotId);
        scenicSpotService.enrich(List.of(s));
        List<VoiceGuideVO.Segment> segments = new ArrayList<>();

        String location = (s.getProvince() == null ? "" : s.getProvince())
                + (s.getCity() == null ? "" : s.getCity());
        segments.add(new VoiceGuideVO.Segment("开场白",
                "欢迎来到" + s.getName() + "。这里位于" + location + ","
                        + "是一处以" + (s.getCategoryName() == null ? "田园风光" : s.getCategoryName())
                        + "闻名的乡村好去处。接下来,请跟着我一起慢慢逛。"));

        String[] featureArr = StringUtils.hasText(s.getFeatures())
                ? s.getFeatures().split("[,、,]") : new String[0];
        String[] sentences = StringUtils.hasText(s.getDescription())
                ? s.getDescription().split("。") : new String[0];
        int groupSize = Math.max(1, (int) Math.ceil(sentences.length / 3.0));
        for (int i = 0, gi = 0; i < sentences.length; i += groupSize, gi++) {
            StringBuilder text = new StringBuilder();
            for (int j = i; j < Math.min(i + groupSize, sentences.length); j++) {
                if (!sentences[j].isBlank()) text.append(sentences[j].trim()).append("。");
            }
            String title = gi < featureArr.length ? "亮点 · " + featureArr[gi].trim() : "景点讲解";
            if (text.length() > 0) segments.add(new VoiceGuideVO.Segment(title, text.toString()));
        }

        StringBuilder tip = new StringBuilder();
        if (StringUtils.hasText(s.getTips())) tip.append(s.getTips());
        if (StringUtils.hasText(s.getOpenTime())) tip.append("开放时间:").append(s.getOpenTime()).append("。");
        if (s.getTicketPrice() != null) {
            tip.append(s.getTicketPrice().doubleValue() <= 0 ? "景区免费开放。"
                    : "门票参考价" + s.getTicketPrice().stripTrailingZeros().toPlainString() + "元。");
        }
        tip.append("祝您旅途愉快。");
        segments.add(new VoiceGuideVO.Segment("温馨提示", tip.toString()));

        return new VoiceGuideVO(s.getId(), s.getName() + " · 语音讲解", segments);
    }
}
