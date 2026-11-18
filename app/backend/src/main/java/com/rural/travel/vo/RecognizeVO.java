package com.rural.travel.vo;

import com.rural.travel.entity.ScenicSpot;
import lombok.Data;

import java.util.List;

@Data
public class RecognizeVO {
    private boolean matched;
    private String message;
    private Double confidence;
    private ScenicSpot spot;
    private List<ScenicSpot> alternatives;
}
