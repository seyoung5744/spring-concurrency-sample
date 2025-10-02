package com.example.concurrency.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CouponIssueResponse {
    private boolean success;
    private int issuedCount;
}