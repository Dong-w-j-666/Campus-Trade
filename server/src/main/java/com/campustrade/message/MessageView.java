package com.campustrade.message;

import java.time.LocalDateTime;

public record MessageView(
        long id,
        Long orderId,
        long senderId,
        String senderName,
        long receiverId,
        String receiverName,
        String type,
        String content,
        boolean read,
        LocalDateTime createdAt,
        boolean mine
) {
}
