package com.example.concurrency.service;

import com.example.concurrency.domain.Coupon;
import com.example.concurrency.domain.CouponStat;
import com.example.concurrency.repository.CouponRepository;
import com.example.concurrency.repository.CouponStatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeadlockProneService {

    private final CouponRepository couponRepository;
    private final CouponStatRepository couponStatRepository;

    // T1: 쿠폰 -> 통계 순서
    @Transactional
    public void flowA(Long couponId) {
        Coupon c = couponRepository.findByIdForUpdate(couponId).orElseThrow();
        c.issueOne();

        CouponStat s = couponStatRepository.findByCouponIdForUpdate(couponId).orElseGet(() -> {
            CouponStat ns = new CouponStat();
            return couponStatRepository.save(ns);
        });

        s.inc();
    }

    // T2: 통계 -> 쿠폰 순서 (역순) ==> T1과 교착 위험
    @Transactional
    public void flowB(Long couponId) {
        CouponStat s = couponStatRepository.findByCouponIdForUpdate(couponId).orElseThrow();
        s.inc();

        Coupon c = couponRepository.findByIdForUpdate(couponId).orElseThrow();
        c.issueOne();
    }
}
