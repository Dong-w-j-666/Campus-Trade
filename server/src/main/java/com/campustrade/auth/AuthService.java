package com.campustrade.auth;

import com.campustrade.common.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final RowMapper<UserAccount> USER_MAPPER = (resultSet, rowNum) -> new UserAccount(
            resultSet.getLong("user_id"),
            resultSet.getString("student_no"),
            resultSet.getString("real_name"),
            resultSet.getString("phone"),
            resultSet.getString("password_hash"),
            resultSet.getBigDecimal("credit_score"),
            resultSet.getString("role_code"),
            resultSet.getString("status"),
            resultSet.getString("campus"),
            resultSet.getString("avatar_url")
    );

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public AuthService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserView register(RegisterRequest request) {
        String studentNo = request.studentNo().trim();
        String phone = request.phone().trim();
        String realName = request.realName().trim();
        String campus = request.campus().trim();

        Integer duplicateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM t_user WHERE student_no = ? OR phone = ?",
                Integer.class,
                studentNo,
                phone
        );
        if (duplicateCount != null && duplicateCount > 0) {
            throw ApiException.conflict("该学号或手机号已经注册");
        }

        try {
            jdbcTemplate.update(
                    """
                    INSERT INTO t_user
                      (student_no, real_name, phone, password_hash, credit_score,
                       role_code, status, certified, campus)
                    VALUES (?, ?, ?, ?, 80.00, 'BUYER', 'ACTIVE', 0, ?)
                    """,
                    studentNo,
                    realName,
                    phone,
                    passwordEncoder.encode(request.password()),
                    campus
            );
        } catch (DuplicateKeyException exception) {
            throw ApiException.conflict("该学号或手机号已经注册");
        }

        UserAccount user = findByAccount(studentNo);
        return toView(user);
    }

    public UserView authenticate(LoginRequest request) {
        UserAccount user = findByAccount(request.account().trim());
        if (!"ACTIVE".equals(user.status())) {
            throw ApiException.forbidden("账号尚未通过认证或被冻结");
        }
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "学号、手机号或密码不正确");
        }
        return toView(user);
    }

    public UserView findUser(long userId) {
        List<UserAccount> users = jdbcTemplate.query(
                """
                SELECT user_id, student_no, real_name, phone, password_hash, credit_score,
                       role_code, status, campus, avatar_url
                FROM t_user
                WHERE user_id = ?
                """,
                USER_MAPPER,
                userId
        );
        if (users.isEmpty()) {
            throw ApiException.unauthorized();
        }
        return toView(users.getFirst());
    }

    private UserAccount findByAccount(String account) {
        List<UserAccount> users = jdbcTemplate.query(
                """
                SELECT user_id, student_no, real_name, phone, password_hash, credit_score,
                       role_code, status, campus, avatar_url
                FROM t_user
                WHERE student_no = ? OR phone = ?
                LIMIT 1
                """,
                USER_MAPPER,
                account,
                account
        );
        if (users.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "学号、手机号或密码不正确");
        }
        return users.getFirst();
    }

    private UserView toView(UserAccount user) {
        return new UserView(
                user.id(),
                user.studentNo(),
                user.realName(),
                user.phone(),
                user.creditScore(),
                user.roleCode(),
                user.campus(),
                user.avatarUrl()
        );
    }

    private record UserAccount(
            long id,
            String studentNo,
            String realName,
            String phone,
            String passwordHash,
            java.math.BigDecimal creditScore,
            String roleCode,
            String status,
            String campus,
            String avatarUrl
    ) {
    }
}
