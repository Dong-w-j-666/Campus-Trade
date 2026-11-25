package com.campustrade.message;

import java.time.LocalDateTime;

public record ConversationView(
        long orderId,
        String orderNo,
        long goodsId,
        String goodsTitle,
        String goodsImage,
        long counterpartId,
        String counterpartName,
        String orderStatus,
        String lastMessage,
        Long lastSenderId,
        LocalDateTime lastMessageAt,
        long unreadCount
) {
}
