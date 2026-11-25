package com.campustrade.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {

    private final JdbcTemplate jdbcTemplate;

    public CategoryService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CategoryView> listEnabled() {
        return jdbcTemplate.query(
                """
                SELECT category_id, parent_id, category_name, sort_no
                FROM t_category
                WHERE status = 'ENABLED'
                ORDER BY sort_no, category_id
                """,
                (resultSet, rowNum) -> new CategoryView(
                        resultSet.getLong("category_id"),
                        resultSet.getObject("parent_id", Long.class),
                        resultSet.getString("category_name"),
                        resultSet.getInt("sort_no")
                )
        );
    }
}
