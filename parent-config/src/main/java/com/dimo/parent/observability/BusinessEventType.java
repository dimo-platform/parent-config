package com.dimo.parent.observability;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/// Business events exposed as Prometheus counters. Names are stable dashboard and alert contracts.
@Getter
@RequiredArgsConstructor
public enum BusinessEventType {

    SIGNUP("dimo.signups", "Accounts created"),
    OTP("dimo.otp", "One-time codes delivered (success) or failed to deliver (failure)"),
    LOGIN("dimo.logins", "Login attempts by method"),
    CHAT_STARTED("dimo.chats.started", "Direct chats that became ready"),
    MESSAGE_SENT("dimo.messages.sent", "Messages sent"),
    MEDIA_UPLOADED("dimo.media.uploaded", "Objects uploaded to object storage"),
    VIP_PURCHASE("dimo.vip.purchases", "VIP plans activated"),
    PAYMENT_FAILURE("dimo.payment.failures", "Payments that need review or were closed unpaid");

    private final String meterName;
    private final String description;
}
