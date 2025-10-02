package com.example.concurrency.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponIssueRequest {
    private Long couponId;
    private Long userId;
}
