package com.example.concurrency;

import com.example.concurrency.domain.Coupon;
import com.example.concurrency.domain.CouponStat;
import com.example.concurrency.repository.CouponRepository;
import com.example.concurrency.repository.CouponStatRepository;
import com.example.concurrency.service.DeadlockProneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DeadlockProneServiceSimpleTest {

    @Autowired
    DeadlockProneService service;
    @Autowired
    CouponRepository couponRepo;
    @Autowired
    CouponStatRepository statRepo;
    @Autowired
    TransactionTemplate tx;

    Long couponId;

    @BeforeEach
    void setUp() {
        tx.executeWithoutResult(s -> {
            statRepo.deleteAll();
            couponRepo.deleteAll();

            Coupon c = Coupon.builder().totalQuantity(100).issuedCount(0).build();

            couponId = couponRepo.save(c).getId();

            // 통계 행도 미리 만들어 두어 두 스레드가 같은 행에 FOR UPDATE를 걸도록
            CouponStat st = new CouponStat();
            st.setCouponId(couponId);
            st.setTotalIssued(0);
            statRepo.save(st);
        });
    }

    @Test
    void deadlock_happens_with_reverse_lock_order() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CyclicBarrier start = new CyclicBarrier(2); // 동시에 시작
        AtomicBoolean deadlockSeen = new AtomicBoolean(false);

        Future<?> f1 = pool.submit(() -> {
            await(start);
            try {
                service.flowA(couponId);
            } catch (RuntimeException e) {
                if (isDeadlock(e)) deadlockSeen.set(true);
            }
        });
        Future<?> f2 = pool.submit(() -> {
            await(start);
            try {
                service.flowB(couponId);
            } catch (RuntimeException e) {
                if (isDeadlock(e)) deadlockSeen.set(true);
            }
        });

        // 두 작업 종료 대기
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        // 데드락 또는 락 획득 실패 예외가 최소 1회는 관측되어야 함
        assertThat(deadlockSeen.get()).isTrue();
    }

    private static void await(CyclicBarrier b) {
        try {
            b.await(3, TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    private static boolean isDeadlock(Throwable t) {
        // 스프링/드라이버별 대표 예외들을 간단히 체크
        while (t != null) {
            if (t instanceof org.springframework.dao.DeadlockLoserDataAccessException) return true;
            if (t instanceof org.springframework.dao.CannotAcquireLockException) return true;
            t = t.getCause();
        }
        return false;
    }
}
