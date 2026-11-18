package com.rural.travel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ContentGenerateDTO {
    /** grass=种草文案 guide=游玩攻略 slogan=宣传标语 */
    @NotBlank(message = "请选择内容类型") private String type;
    private Long scenicSpotId;
    /** fresh=清新 lively=活泼 literary=文艺 */
    private String style;
    /** short/medium/long */
    private String length;
}
