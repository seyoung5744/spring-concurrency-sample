package com.example.concurrency.service;

import com.example.concurrency.aop.DistributedLock;
import com.example.concurrency.domain.Coupon;
import com.example.concurrency.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    @DistributedLock(key = "#couponId")
    public boolean issue(Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("coupon not found"));
        return coupon.issueOne();
    }

    @Transactional(readOnly = true)
    public int getIssuedCount(Long couponId) {
        return couponRepository.findById(couponId)
                .map(Coupon::getIssuedCount)
                .orElseThrow(() -> new IllegalArgumentException("coupon not found"));
    }
}

