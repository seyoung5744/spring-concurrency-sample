package com.example.concurrency;

import com.example.concurrency.domain.Coupon;
import com.example.concurrency.repository.CouponRepository;
import com.example.concurrency.service.CouponService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@SpringBootTest
public class CouponRedisLockSimpleTest {

    @Autowired
    CouponService couponService;
    @Autowired
    CouponRepository couponRepository;

    @Test
    void concurrent_issue() throws Exception {
        Long id = couponRepository.save(
                Coupon.builder().totalQuantity(100).issuedCount(0).build()
        ).getId();

        int threads = 200;
        ExecutorService pool = Executors.newFixedThreadPool(threads);

        // 동시에 시작시키기 위한 배리어(메인 스레드 + 워커 스레드 수)
        CyclicBarrier startLine = new CyclicBarrier(threads + 1);
        CountDownLatch done = new CountDownLatch(threads);

        List<Future<Boolean>> results = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                try {
                    startLine.await();        // 모든 스레드가 모이면 동시에 출발
                    return couponService.issue(id);
                } finally {
                    done.countDown();
                }
            }));
        }

        // 모두 준비되면 스타트
        startLine.await();

        // 완료 대기 (타임아웃 부여)
        boolean finished = done.await(30, TimeUnit.SECONDS);
        Assertions.assertTrue(finished, "동시성 작업이 시간 내에 끝나지 않았습니다.");

        // 성공(=true) 횟수 집계
        long successCount = 0;
        for (Future<Boolean> f : results) {
            if (Boolean.TRUE.equals(f.get())) successCount++;
        }

        // JPA 1차 캐시 영향 없이 최종값 검증 (테스트 메서드는 트랜잭션 밖이므로 보통 문제 없음)
        Coupon c = couponRepository.findById(id).orElseThrow();

        // 두 축으로 단언
        Assertions.assertEquals(100, c.getIssuedCount(), "최종 발급 수량이 총량과 다릅니다.");
        Assertions.assertEquals(100, successCount, "성공 응답(Boolean true) 횟수가 총량과 다릅니다.");

        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);
    }
}
