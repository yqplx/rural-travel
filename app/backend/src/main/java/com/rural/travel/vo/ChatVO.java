package com.rural.travel.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ChatVO {
    private String answer;
    private String matchedQuestion;
    private List<String> suggestions;
    private Long relatedSpotId;
    private String relatedSpotName;
}
