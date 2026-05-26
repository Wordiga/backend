package com.wordiga.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @Column(length = 50)
    private String nickname;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    private Member(String email, String nickname, String password, String provider, String providerId) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
    }

    public static Member create(String email, String nickname, String password) {
        return new Member(email, nickname, password, "LOCAL", null);
    }

    public static Member ofOAuth(String email, String nickname, String provider, String providerId) {
        return new Member(email, nickname, null, provider, providerId);
    }
}