package com.campustrade.catalog;

import com.campustrade.common.ApiException;
import com.campustrade.common.PageResult;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class GoodsService {

    private static final RowMapper<GoodsCard> CARD_MAPPER = (resultSet, rowNum) -> new GoodsCard(
            resultSet.getLong("goods_id"),
            resultSet.getString("title"),
            resultSet.getBigDecimal("price"),
            resultSet.getString("condition_level"),
            resultSet.getString("campus"),
            resultSet.getString("status"),
            resultSet.getInt("view_count"),
            resultSet.getString("image_url"),
            resultSet.getLong("seller_id"),
            resultSet.getString("seller_name"),
            resultSet.getString("category_name"),
            resultSet.getTimestamp("created_at").toLocalDateTime()
    );

    private static final String CARD_SELECT = """
            SELECT g.goods_id, g.title, g.price, g.condition_level, g.campus, g.status,
                   g.view_count, g.created_at, g.seller_id,
                   u.real_name AS seller_name,
                   c.category_name,
                   (SELECT gi.image_url
                    FROM t_goods_image gi
                    WHERE gi.goods_id = g.goods_id
                    ORDER BY gi.sort_no
                    LIMIT 1) AS image_url
            FROM t_goods g
            JOIN t_user u ON u.user_id = g.seller_id
            JOIN t_category c ON c.category_id = g.category_id
            """;

    private final JdbcTemplate jdbcTemplate;

    public GoodsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public PageResult<GoodsCard> search(
            String keyword,
            Long categoryId,
            String campus,
            String sort,
            int page,
            int size
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 48);
        StringBuilder where = new StringBuilder(" WHERE g.status = 'ON_SALE'");
        List<Object> parameters = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (g.title LIKE ? OR g.description LIKE ?)");
            String query = "%" + keyword.trim() + "%";
            parameters.add(query);
            parameters.add(query);
        }
        if (categoryId != null) {
            where.append(" AND g.category_id = ?");
            parameters.add(categoryId);
        }
        if (campus != null && !campus.isBlank()) {
            where.append(" AND g.campus = ?");
            parameters.add(campus.trim());
        }

        String normalizedSort = sort == null ? "latest" : sort.toLowerCase(Locale.ROOT);
        String orderBy = switch (normalizedSort) {
            case "price-asc" -> " ORDER BY g.price ASC, g.goods_id DESC";
            case "price-desc" -> " ORDER BY g.price DESC, g.goods_id DESC";
            case "popular" -> " ORDER BY g.view_count DESC, g.goods_id DESC";
            default -> " ORDER BY g.created_at DESC, g.goods_id DESC";
        };

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_goods g" + where,
                Long.class,
                parameters.toArray()
        );
        long totalItems = total == null ? 0 : total;

        List<Object> pageParameters = new ArrayList<>(parameters);
        pageParameters.add(safeSize);
        pageParameters.add(safePage * safeSize);
        List<GoodsCard> items = jdbcTemplate.query(
                CARD_SELECT + where + orderBy + " LIMIT ? OFFSET ?",
                CARD_MAPPER,
                pageParameters.toArray()
        );
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        return new PageResult<>(items, safePage, safeSize, totalItems, totalPages);
    }

    @Transactional
    public GoodsDetail detail(long goodsId, boolean increaseView) {
        if (increaseView) {
            jdbcTemplate.update(
                    "UPDATE t_goods SET view_count = view_count + 1 WHERE goods_id = ? AND status = 'ON_SALE'",
                    goodsId
            );
        }
        GoodsDetail detail;
        try {
            detail = jdbcTemplate.queryForObject(
                    """
                    SELECT g.goods_id, g.title, g.description, g.price, g.condition_level,
                           g.category_id, c.category_name, g.campus, g.status, g.view_count,
                           g.created_at, u.user_id AS seller_id, u.real_name AS seller_name,
                           u.credit_score, u.campus AS seller_campus, u.avatar_url
                    FROM t_goods g
                    JOIN t_category c ON c.category_id = g.category_id
                    JOIN t_user u ON u.user_id = g.seller_id
                    WHERE g.goods_id = ?
                    """,
                    (resultSet, rowNum) -> new GoodsDetail(
                            resultSet.getLong("goods_id"),
                            resultSet.getString("title"),
                            resultSet.getString("description"),
                            resultSet.getBigDecimal("price"),
                            resultSet.getString("condition_level"),
                            resultSet.getLong("category_id"),
                            resultSet.getString("category_name"),
                            resultSet.getString("campus"),
                            resultSet.getString("status"),
                            resultSet.getInt("view_count"),
                            List.of(),
                            new GoodsDetail.SellerView(
                                    resultSet.getLong("seller_id"),
                                    resultSet.getString("seller_name"),
                                    resultSet.getBigDecimal("credit_score"),
                                    resultSet.getString("seller_campus"),
                                    resultSet.getString("avatar_url")
                            ),
                            resultSet.getTimestamp("created_at").toLocalDateTime()
                    ),
                    goodsId
            );
        } catch (EmptyResultDataAccessException exception) {
            throw ApiException.notFound("商品不存在或已被删除");
        }

        List<String> images = jdbcTemplate.query(
                "SELECT image_url FROM t_goods_image WHERE goods_id = ? ORDER BY sort_no",
                (resultSet, rowNum) -> resultSet.getString("image_url"),
                goodsId
        );
        return new GoodsDetail(
                detail.id(),
                detail.title(),
                detail.description(),
                detail.price(),
                detail.conditionLevel(),
                detail.categoryId(),
                detail.categoryName(),
                detail.campus(),
                detail.status(),
                detail.viewCount(),
                images,
                detail.seller(),
                detail.createdAt()
        );
    }

    @Transactional
    public long publish(long sellerId, PublishGoodsRequest request) {
        Integer categoryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_category WHERE category_id = ? AND status = 'ENABLED'",
                Integer.class,
                request.categoryId()
        );
        if (categoryCount == null || categoryCount == 0) {
            throw ApiException.badRequest("所选分类不可用");
        }

        List<String> imageUrls = normalizeImages(request.imageUrls());
        jdbcTemplate.update(
                """
                INSERT INTO t_goods
                  (title, description, price, condition_level, category_id, seller_id,
                   campus, status, view_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'ON_SALE', 0)
                """,
                request.title().trim(),
                trimToNull(request.description()),
                request.price(),
                request.conditionLevel().trim(),
                request.categoryId(),
                sellerId,
                request.campus().trim()
        );
        Long goodsId = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (goodsId == null) {
            throw new IllegalStateException("Unable to read generated goods id");
        }
        for (int index = 0; index < imageUrls.size(); index++) {
            jdbcTemplate.update(
                    "INSERT INTO t_goods_image (goods_id, image_url, sort_no) VALUES (?, ?, ?)",
                    goodsId,
                    imageUrls.get(index),
                    index + 1
            );
        }
        return goodsId;
    }

    public List<GoodsCard> listMine(long sellerId) {
        return jdbcTemplate.query(
                CARD_SELECT + " WHERE g.seller_id = ? ORDER BY g.created_at DESC, g.goods_id DESC",
                CARD_MAPPER,
                sellerId
        );
    }

    public void takeOffShelf(long goodsId, long sellerId) {
        Integer updated = jdbcTemplate.update(
                """
                UPDATE t_goods
                SET status = 'OFF_SHELF'
                WHERE goods_id = ? AND seller_id = ? AND status = 'ON_SALE'
                """,
                goodsId,
                sellerId
        );
        if (updated == 0) {
            throw ApiException.conflict("商品当前不可下架");
        }
    }

    private List<String> normalizeImages(List<String> imageUrls) {
        if (imageUrls == null) {
            return List.of();
        }
        return imageUrls.stream()
                .filter(url -> url != null && !url.isBlank())
                .map(String::trim)
                .peek(this::validateImageUrl)
                .toList();
    }

    private void validateImageUrl(String url) {
        boolean allowed = url.startsWith("https://")
                || url.startsWith("http://")
                || url.startsWith("/images/")
                || url.startsWith("/uploads/");
        if (!allowed) {
            throw ApiException.badRequest("图片地址格式不受支持");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
