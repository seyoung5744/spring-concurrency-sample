package com.example.concurrency.repository;

import com.example.concurrency.domain.CouponStat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponStatRepository extends JpaRepository<CouponStat, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CouponStat s where s.couponId = :couponId")
    Optional<CouponStat> findByCouponIdForUpdate(@Param("couponId") Long couponId);
}
