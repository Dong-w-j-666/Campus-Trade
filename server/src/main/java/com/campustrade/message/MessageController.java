package com.campustrade.message;

import com.campustrade.auth.CurrentUser;
import com.campustrade.auth.LoginRequired;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@LoginRequired
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/conversations")
    public List<ConversationView> conversations(HttpServletRequest request) {
        return messageService.conversations(CurrentUser.id(request));
    }

    @GetMapping("/orders/{orderId}")
    public List<MessageView> listForOrder(
            @PathVariable long orderId,
            HttpServletRequest request
    ) {
        return messageService.listForOrder(orderId, CurrentUser.id(request));
    }

    @PostMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.CREATED)
    public MessageView send(
            @PathVariable long orderId,
            @Valid @RequestBody SendMessageRequest body,
            HttpServletRequest request
    ) {
        return messageService.send(orderId, CurrentUser.id(request), body);
    }

    @PostMapping("/orders/{orderId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markOrderRead(@PathVariable long orderId, HttpServletRequest request) {
        messageService.markOrderRead(orderId, CurrentUser.id(request));
    }

    @GetMapping("/notifications")
    public List<MessageView> notifications(HttpServletRequest request) {
        return messageService.notifications(CurrentUser.id(request));
    }

    @PostMapping("/notifications/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markNotificationsRead(HttpServletRequest request) {
        messageService.markNotificationsRead(CurrentUser.id(request));
    }

    @GetMapping("/unread-count")
    public UnreadCountView unreadCount(HttpServletRequest request) {
        return messageService.unreadCount(CurrentUser.id(request));
    }
}
