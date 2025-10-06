package com.example.concurrency;

import com.example.concurrency.domain.Coupon;
import com.example.concurrency.repository.CouponRepository;
import com.example.concurrency.service.CouponServiceOptimistic;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CouponOptimisticLockTest {

    @Autowired
    CouponRepository couponRepository;
    @Autowired
    CouponServiceOptimistic svc;
    @Autowired
    TransactionTemplate tx;

    Long couponId;

    @BeforeEach
    void init() {
        tx.executeWithoutResult(s -> {
            couponRepository.deleteAll();
            couponId = couponRepository.save(
                    Coupon.builder()
                            .totalQuantity(100)
                            .issuedCount(0)
                            .build()
            ).getId();
        });
    }

    @Test
    void optimistic_lock_without_retry() throws Exception {
        int threads = 20, attempts = 100;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(attempts);

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    // 재시도 없는 케이스: 충돌 시 예외 → 요청 실패
                    svc.issueOnce(couponId);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        int issued = couponRepository.findById(couponId).orElseThrow().getIssuedCount();
        assertThat(issued).isEqualTo(100); // 초과발급은 없음
    }

    @Test
    void optimistic_lock_with_retry() throws Exception {
        int threads = 200, attempts = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1), done = new CountDownLatch(attempts);

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    svc.issueWithRetry(couponId);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        int issued = couponRepository.findById(couponId).orElseThrow().getIssuedCount();
        assertThat(issued).isEqualTo(100); // 동일
    }
}
