package com.example.concurrency.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "coupon_stats")
@Getter
@Setter
@NoArgsConstructor
public class CouponStat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long couponId;

    @Column(nullable = false)
    private int totalIssued;

    public void inc() {
        totalIssued++;
    }
}
