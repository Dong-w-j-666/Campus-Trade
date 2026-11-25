package com.campustrade.catalog;

import com.campustrade.auth.CurrentUser;
import com.campustrade.auth.LoginRequired;
import com.campustrade.common.ApiException;
import com.campustrade.common.PageResult;
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
import java.util.Map;

@RestController
@RequestMapping("/api/goods")
public class GoodsController {

    private final GoodsService goodsService;

    public GoodsController(GoodsService goodsService) {
        this.goodsService = goodsService;
    }

    @GetMapping
    public PageResult<GoodsCard> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String campus,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return goodsService.search(keyword, categoryId, campus, sort, page, size);
    }

    @GetMapping("/{goodsId}")
    public GoodsDetail detail(@PathVariable long goodsId) {
        return goodsService.detail(goodsId, true);
    }

    @GetMapping("/mine")
    @LoginRequired
    public List<GoodsCard> mine(HttpServletRequest request) {
        return goodsService.listMine(CurrentUser.id(request));
    }

    @PostMapping
    @LoginRequired
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Long> publish(
            @Valid @RequestBody PublishGoodsRequest goodsRequest,
            HttpServletRequest request
    ) {
        long goodsId = goodsService.publish(CurrentUser.id(request), goodsRequest);
        return Map.of("id", goodsId);
    }

    @PostMapping("/{goodsId}/off-shelf")
    @LoginRequired
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void takeOffShelf(@PathVariable long goodsId, HttpServletRequest request) {
        goodsService.takeOffShelf(goodsId, CurrentUser.id(request));
    }
}
