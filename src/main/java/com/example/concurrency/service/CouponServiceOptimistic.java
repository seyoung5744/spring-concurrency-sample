package com.example.concurrency.service;

import com.example.concurrency.domain.Coupon;
import com.example.concurrency.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class CouponServiceOptimistic {

    private final CouponRepository repo;
    private final TransactionTemplate txTemplate;

    // 단순 적용: 실패하면 예외 전파
    @Transactional
    public boolean issueOnce(Long couponId) {
        Coupon c = repo.findWithOptimisticById(couponId).orElseThrow();
        return c.issueOne(); // flush 시점에 버전 충돌 가능
    }

    public boolean issueWithRetry(Long couponId) {
        int maxRetry = 8;
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            try {
                return txTemplate.execute(status -> {
                    Coupon c = repo.findWithOptimisticById(couponId).orElseThrow();
                    boolean ok = c.issueOne();
                    repo.flush(); // 커밋 직전에 충돌 확인
                    return ok;
                });
            } catch (ObjectOptimisticLockingFailureException e) {
                if (attempt == maxRetry) throw e;
                sleepBackoff(attempt);
            }
        }
        return false;
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(10L * attempt + ThreadLocalRandom.current().nextInt(10));
        } catch (InterruptedException ignored) {
        }
    }
}
