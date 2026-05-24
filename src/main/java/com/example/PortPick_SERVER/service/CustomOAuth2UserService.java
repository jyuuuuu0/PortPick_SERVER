package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserRepository userRepository;
    @Value("${app.profile.default-image-url:/images/default-profile.svg}")
    private String defaultProfileImageUrl;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        if (oAuth2User == null) {
            throw new OAuth2AuthenticationException("Failed to load user information from Google.");
        }

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google account email is missing.");
        }

        if (name == null || name.isBlank()) {
            name = email;
        }

        saveOrUpdate(email, name, registrationId);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName
        );
    }

    private User saveOrUpdate(String email, String name, String provider) {
        User user = userRepository.findByEmail(email)
                .map(entity -> entity.isSignupCompleted() ? entity : entity.updateOAuthName(name))
                .orElse(User.builder()
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .signupCompleted(false)
                        .profileImageUrl(defaultProfileImageUrl)
                        .customProfileImage(false)
                        .build());

        return userRepository.save(user);
    }
}
