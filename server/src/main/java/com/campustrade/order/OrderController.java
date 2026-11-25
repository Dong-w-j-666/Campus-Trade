package com.campustrade.order;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@LoginRequired
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView create(
            @Valid @RequestBody CreateOrderRequest request,
            HttpServletRequest servletRequest
    ) {
        return orderService.create(CurrentUser.id(servletRequest), request);
    }

    @GetMapping
    public List<OrderView> list(
            @RequestParam(defaultValue = "buyer") String role,
            HttpServletRequest servletRequest
    ) {
        return orderService.list(CurrentUser.id(servletRequest), role);
    }

    @GetMapping("/{orderId}")
    public OrderView detail(@PathVariable long orderId, HttpServletRequest servletRequest) {
        return orderService.findAccessible(orderId, CurrentUser.id(servletRequest));
    }

    @PostMapping("/{orderId}/pay")
    public OrderView pay(@PathVariable long orderId, HttpServletRequest request) {
        return orderService.pay(orderId, CurrentUser.id(request));
    }

    @PostMapping("/{orderId}/cancel")
    public OrderView cancel(@PathVariable long orderId, HttpServletRequest request) {
        return orderService.cancel(orderId, CurrentUser.id(request));
    }

    @PostMapping("/{orderId}/refund")
    public OrderView refund(@PathVariable long orderId, HttpServletRequest request) {
        return orderService.refund(orderId, CurrentUser.id(request));
    }

    @PostMapping("/{orderId}/complete")
    public OrderView complete(@PathVariable long orderId, HttpServletRequest request) {
        return orderService.complete(orderId, CurrentUser.id(request));
    }
}
