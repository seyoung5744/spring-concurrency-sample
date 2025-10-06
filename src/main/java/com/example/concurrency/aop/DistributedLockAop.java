package com.example.concurrency.aop;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockAop {

    private final RedissonClient redissonClient;

//    @Around("@annotation(distributedLock)")
    public Object lockV0(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = "LOCK:" + distributedLock.key();
        RLock rLock = redissonClient.getLock(key);

        try {
            if (!rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit())) {
                throw new IllegalStateException("락 획득 실패: " + key);
            }
            // (중요) 여기서 바로 joinPoint.proceed() 실행 → 트랜잭션과 해제 순서 꼬일 수 있음
            return joinPoint.proceed();
        } finally {
            rLock.unlock(); // 트랜잭션 종료 전 해제될 수 있음
        }
    }

    @Around("@annotation(distributedLock)")
    public Object lock(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String key = "LOCK:" + distributedLock.key();
        RLock rLock = redissonClient.getLock(key);

        boolean locked = false;
        try {
            // leaseTime <= 0 이면 watchdog을 사용(자동 연장)해서 커밋 전 자동해제 방지
            if (distributedLock.leaseTime() <= 0) {
                // tryLock(waitTime, unit) 오버로드 → watchdog ON
                locked = rLock.tryLock(distributedLock.waitTime(), distributedLock.timeUnit());
            } else {
                // leaseTime 명시 → watchdog OFF (실습/비교용)
                locked = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(), distributedLock.timeUnit());
            }

            if (!locked) {
                throw new IllegalStateException("락 획득 실패: " + key);
            }

            // 트랜잭션 활성화 여부에 따라 unlock 시점 결정
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                final RLock lockRef = rLock;
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        // 커밋 완료 후 해제 → "락 해제 ≤ 커밋" 순서 보장
                        safeUnlock(lockRef);
                    }
                    @Override
                    public void afterCompletion(int status) {
                        // 롤백 시에도 반드시 해제 (누수 방지)
                        if (status != STATUS_COMMITTED) {
                            safeUnlock(lockRef);
                        }
                    }
                });
                // 트랜잭션이 있으면 여기서는 unlock하지 않음
                return joinPoint.proceed();
            } else {
                // 트랜잭션이 없다면 일반 finally에서 해제
                return joinPoint.proceed();
            }

        } finally {
            // 트랜잭션이 없을 때만 여기서 해제
            if (!TransactionSynchronizationManager.isSynchronizationActive() && locked) {
                safeUnlock(rLock);
            }
        }
    }

    private void safeUnlock(RLock lock) {
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        } catch (IllegalMonitorStateException ignored) {
            // 이미 해제되었거나 보유 스레드가 아닐 때 대비
        }
    }
}
