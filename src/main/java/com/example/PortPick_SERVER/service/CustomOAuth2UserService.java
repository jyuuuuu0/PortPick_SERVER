package com.example.PortPick_SERVER.service;

import com.example.PortPick_SERVER.model.User;
import com.example.PortPick_SERVER.repository.UserRepository;
import lombok.RequiredArgsConstructor;
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

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = delegate.loadUser(userRequest);

        if (oAuth2User == null) {
            throw new OAuth2AuthenticationException("구글에서 유저 정보를 불러오지 못했습니다.");
        }
        // 1. 구글 로그인인지 카카오 로그인인지 구분하는 ID (여기선 "google"이 들어옴)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        // 2. OAuth2 로그인 진행 시 키가 되는 필드값 (구글은 기본적으로 "sub"라는 키를 씁니다)
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 3. 구글 서버가 던져준 유저 정보 유저 맵(Map) 데이터
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 4. 구글 데이터에서 필요한 값(이메일, 이름)만 쏙쏙 추출
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        // 5. DB에 저장하거나 이미 있으면 업데이트
        User user = saveOrUpdate(email, name, registrationId);

        // 6. 시큐리티 세션에 유저 정보를 담아서 리턴
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                userNameAttributeName
        );
    }

    // 이미 가입된 유저면 이름만 업데이트하고, 신규 유저면 회원가입(Save) 시키는 함수
    private User saveOrUpdate(String email, String name, String provider) {
        User user = userRepository.findByEmail(email)
                .map(entity -> entity.update(name))
                .orElse(User.builder()
                        .email(email)
                        .name(name)
                        .provider(provider)
                        .build());

        return userRepository.save(user);
    }
}