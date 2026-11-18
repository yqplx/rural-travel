package com.rural.travel.controller;

import com.rural.travel.ai.AiChatService;
import com.rural.travel.ai.ImageRecognitionService;
import com.rural.travel.common.Result;
import com.rural.travel.vo.ChatVO;
import com.rural.travel.vo.RecognizeVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** AI 游客端:智能问答 + 拍照识景(免登录) */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiChatService aiChatService;
    private final ImageRecognitionService imageRecognitionService;

    public AiController(AiChatService aiChatService, ImageRecognitionService imageRecognitionService) {
        this.aiChatService = aiChatService;
        this.imageRecognitionService = imageRecognitionService;
    }

    public record ChatDTO(String message) {}

    @PostMapping("/chat")
    public Result<ChatVO> chat(@RequestBody ChatDTO dto) {
        return Result.ok(aiChatService.chat(dto.message()));
    }

    @PostMapping("/recognize")
    public Result<RecognizeVO> recognize(@RequestParam("file") MultipartFile file) {
        return Result.ok(imageRecognitionService.recognize(file));
    }
}
