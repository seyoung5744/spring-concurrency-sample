package com.example.concurrency.controller;

import com.example.concurrency.dto.*;
import com.example.concurrency.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    public CouponIssueResponse issue(@RequestBody CouponIssueRequest req) {
        boolean success = couponService.issue(req.getCouponId());
        int issued = couponService.getIssuedCount(req.getCouponId());
        return new CouponIssueResponse(success, issued);
    }
}

