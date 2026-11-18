package com.rural.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class VoiceGuideVO {
    private Long scenicSpotId;
    private String title;
    private List<Segment> segments;

    @Data
    @AllArgsConstructor
    public static class Segment {
        private String title;
        private String text;
    }
}
