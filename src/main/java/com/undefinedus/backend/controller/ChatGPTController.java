package com.undefinedus.backend.controller;

import com.undefinedus.backend.dto.MemberSecurityDTO;
import com.undefinedus.backend.dto.response.ApiResponseDTO;
import com.undefinedus.backend.dto.response.aladinAPI.AladinApiResponseDTO;
import com.undefinedus.backend.dto.response.discussion.DiscussionGPTResponseDTO;
import com.undefinedus.backend.service.ChatGPTService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatGpt")
public class ChatGPTController {

    private final ChatGPTService chatGPTService;

    // 회원이 읽은 책들 기반 gpt 추천 도서 목록(gpt가 추천 책의 isbn13을 출력하지 않음)
    @GetMapping("/recommended")
    public ResponseEntity<ApiResponseDTO<List<AladinApiResponseDTO>>> getGPTRecommendedBookLIst(
        @AuthenticationPrincipal MemberSecurityDTO memberSecurityDTO) {

        Long memberId = memberSecurityDTO.getId();

        System.out.println("memberId : " + memberId);

        List<AladinApiResponseDTO> gptRecommendedBookList = chatGPTService.getGPTRecommendedBookList(
            memberId);

        return ResponseEntity.ok(ApiResponseDTO.success(gptRecommendedBookList));
    }

    // 토론 게시판의 정보들을 가지고 gpt가 결론낸것 보기
    @GetMapping("/discussion")
    public ResponseEntity<ApiResponseDTO<DiscussionGPTResponseDTO>> getDiscussionGTP(
        Long discussionId) {

        DiscussionGPTResponseDTO discussionGPTResult = chatGPTService.getDiscussionGPTResult(
            discussionId);

        return ResponseEntity.ok(ApiResponseDTO.success(discussionGPTResult));
    }


}
