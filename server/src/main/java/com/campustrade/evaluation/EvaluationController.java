package com.campustrade.evaluation;

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
@RequestMapping("/api/evaluations")
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @PostMapping
    @LoginRequired
    @ResponseStatus(HttpStatus.CREATED)
    public EvaluationView submit(
            @Valid @RequestBody EvaluationRequest request,
            HttpServletRequest servletRequest
    ) {
        return evaluationService.submit(CurrentUser.id(servletRequest), request);
    }

    @GetMapping("/orders/{orderId}")
    @LoginRequired
    public List<EvaluationView> listForOrder(
            @PathVariable long orderId,
            HttpServletRequest servletRequest
    ) {
        return evaluationService.listForOrder(orderId, CurrentUser.id(servletRequest));
    }

    @GetMapping("/users/{userId}")
    public ReputationView reputation(@PathVariable long userId) {
        return evaluationService.reputation(userId);
    }
}
