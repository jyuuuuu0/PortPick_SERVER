package com.example.PortPick_SERVER.controller;

import com.example.PortPick_SERVER.dto.ProfileResponse;
import com.example.PortPick_SERVER.dto.ProfileUpsertRequest;
import com.example.PortPick_SERVER.service.ProfileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(Authentication authentication) {
        return ResponseEntity.ok(profileService.getMyProfile(authentication.getName()));
    }

    @PostMapping(value = "/signup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> completeSignup(
            Authentication authentication,
            @ModelAttribute ProfileUpsertRequest request,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        return ResponseEntity.ok(profileService.completeSignup(authentication.getName(), request, profileImage));
    }

    @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateMyProfile(
            Authentication authentication,
            @ModelAttribute ProfileUpsertRequest request,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage
    ) {
        return ResponseEntity.ok(profileService.updateMyProfile(authentication.getName(), request, profileImage));
    }
}
