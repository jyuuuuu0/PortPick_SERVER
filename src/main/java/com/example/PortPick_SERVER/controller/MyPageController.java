package com.example.PortPick_SERVER.controller;

import com.example.PortPick_SERVER.dto.MyPageResponse;
import com.example.PortPick_SERVER.service.MyPageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mypage")
public class MyPageController {

    private final MyPageService myPageService;

    public MyPageController(MyPageService myPageService) {
        this.myPageService = myPageService;
    }

    @GetMapping
    public ResponseEntity<MyPageResponse> getMyPage(Authentication authentication) {
        return ResponseEntity.ok(myPageService.getMyPage(authentication.getName()));
    }
}
