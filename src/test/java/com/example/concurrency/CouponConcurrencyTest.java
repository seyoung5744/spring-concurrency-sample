package com.example.concurrency;

import com.example.concurrency.domain.Coupon;
import com.example.concurrency.repository.CouponRepository;
import com.example.concurrency.service.CouponService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class CouponConcurrencyTest {

    @Autowired
    CouponRepository couponRepository;
    @Autowired
    CouponService couponService;
    @Autowired
    TransactionTemplate tx;

    Long couponId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            couponRepository.deleteAll();
            Coupon c = Coupon.builder()
                    .totalQuantity(100)
                    .issuedCount(0)
                    .build();
            couponId = couponRepository.save(c).getId();
        });
    }

    @Test
    void without_lock_can_over_issue() throws Exception {
        int threads = 200;
        int attempts = 1000;

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(attempts);

        for (int i = 0; i < attempts; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    couponService.issue(couponId);
                } catch (Exception ignored) {
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();

        int issued = couponService.getIssuedCount(couponId);
        assertThat(issued).isEqualTo(100);
    }
}

