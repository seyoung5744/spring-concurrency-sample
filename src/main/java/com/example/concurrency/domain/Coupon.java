package com.example.concurrency.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Coupon {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 총 발급 가능 수량(예: 100장)
    @Column(nullable = false)
    private int totalQuantity;

    // 현재까지 발급된 수량
    @Column(nullable = false)
    private int issuedCount;

    // 단순 로직: issuedCount < totalQuantity 일 때만 1장 발급
    public boolean issueOne() {
        if (issuedCount >= totalQuantity) return false;
        issuedCount += 1;
        return true;
    }
}

