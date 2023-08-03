package com.example.demo.repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.Role;
import com.example.demo.config.JpaAuditingConfiguration.SecurityAuditor;
import com.example.demo.model.GenderStatus;
import com.example.demo.model.UserSearchForm;
import com.example.demo.utils.CaseUtils;

@Repository
public class JdbcLoginUserRepository {
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    static final private String count_sql = """
        SELECT
            count(*)
        FROM
            login_user l1
        """;
    static final private String get_id_sql = """
        SELECT
            l1.id
        FROM
            login_user l1
        """;
    static final private String get_data_sql = """
        SELECT
            l1.id,
            l1.name,
            l1.email,
            l1.password,
            l1.gender,
            l1.genre,
            l1.created_at,
            l1.created_by,
            l1.updated_at,
            l1.updated_by,
            r1.id AS role_id,
            r1.name AS role_name,
            r1.created_at AS role_created_at,
            r1.created_by AS role_created_by,
            r1.updated_at AS role_updated_at,
            r1.updated_by AS role_updated_by
        FROM
            login_user l1
        LEFT JOIN user_role ur1 ON ur1.user_id = l1.id
        LEFT JOIN roles r1 ON r1.id = ur1.role_id
        """;

    private String getWhere(UserSearchForm form, MapSqlParameterSource params) {
        if (form == null) return "";
        List<String> stmt = new ArrayList<>();
        if (StringUtils.hasText(form.getName())) {
            stmt.add("name like :name");
            params.addValue("name", "%"+form.getName()+"%");
        }
        if (StringUtils.hasText(form.getEmail())) {
            stmt.add("email like :email");
            params.addValue("email", "%"+form.getEmail()+"%");
        }
        if (form.getRoles() != null && form.getRoles().length > 0) {
            stmt.add("""
                EXISTS (
                    SELECT
                        1
                    FROM
                        roles r1_0
                    WHERE
                        r1_0.id IN (
                            SELECT
                                r2_0.role_id
                            FROM
                                user_role r2_0
                            WHERE
                                l1.id = r2_0.user_id
                        )
                        AND
                        r1_0.name IN (:role_names)
                )
            """);
            params.addValue("role_names", Arrays.asList(form.getRoles()));
        }
        StringBuilder sb = new StringBuilder();
        for (String s: stmt) {
            if (sb.length() == 0)
                sb.append(" WHERE ").append(s);
            else
                sb.append(" AND ").append(s);

        }
        return sb.toString();
    }

    private String getOrderBy(UserSearchForm form, MapSqlParameterSource params) {
        StringBuilder sb = new StringBuilder();
        if (form.getSortBy() != null) {
            String sortBy = form.getSortBy();
            sb.append(" ORDER BY :orderBy :orderAllow");
            params.addValue("orderBy", sortBy.substring(1));
            params.addValue("orderAllow", sortBy.startsWith("+") ? "ASC" : "DESC");
        }
        return sb.toString();
    }

    private String getLimit(PageRequest pageRequest) {
        StringBuilder sb = new StringBuilder();
        if (pageRequest != null)
            sb.append(" LIMIT ").append(pageRequest.getOffset()).append(',')
                    .append(pageRequest.getPageSize());
        return sb.toString();
    }

    private List<LoginUser> queryList(String sql, MapSqlParameterSource params) {
        LinkedHashMap<Integer, LoginUser> userMap = new LinkedHashMap<>();
        jdbcTemplate.query(sql.toString(), params, (rs, rowNum) -> {
            Integer id = rs.getInt("id");
            LoginUser loginUser = userMap.get(id);
            if (loginUser == null) {
                loginUser = LoginUser.builder()
                        .id(id)
                        .name(rs.getString("name"))
                        .email(rs.getString("email"))
                        .password(rs.getString("password"))
                        .gender(GenderStatus.valueOf(rs.getString("gender")))
                        .genre(rs.getString("genre"))
                        .roleList(new ArrayList<>())
                        .createdAt(rs.getObject("created_at", LocalDateTime.class))
                        .createdBy(rs.getString("created_by"))
                        .updatedAt(rs.getObject("updated_at", LocalDateTime.class))
                        .updatedBy(rs.getString("updated_by"))
                        .build();
                userMap.put(id, loginUser);
            }
            Integer role_id = rs.getInt("role_id");
            if (role_id != null) {
                Role role = Role.builder()
                        .id(role_id)
                        .name(rs.getString("role_name"))
                        .createdAt(rs.getObject("role_created_at", LocalDateTime.class))
                        .createdBy(rs.getString("role_created_by"))
                        .updatedAt(rs.getObject("role_updated_at", LocalDateTime.class))
                        .updatedBy(rs.getString("role_updated_by"))
                        .build();
                loginUser.getRoleList().add(role);
            }
            return loginUser;
        });
        return new ArrayList<>(userMap.values());
    }

    public Page<LoginUser> findAll(PageRequest pageRequest, UserSearchForm form) {
        // レコード件数取得
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(count_sql);
        String whereStmt = getWhere(form, params);
        sql.append(whereStmt);
        Long userCount = jdbcTemplate.queryForObject(sql.toString(), params, Long.class);
        if (Optional.of(userCount).orElse(0L) == 0)
            return new PageImpl<LoginUser>(new ArrayList<LoginUser>(), pageRequest, 0L);
        // 指定ページのPK取得
        sql.delete(0, sql.length());
        sql.append(get_id_sql);
        sql.append(whereStmt);
        String orderBy = getOrderBy(form, params);
        sql.append(orderBy);
        sql.append(getLimit(pageRequest));
        List<Integer> userIdList = jdbcTemplate.queryForList(sql.toString(), params, Integer.class);
        // データ本体取得
        sql.delete(0, sql.length());
        sql.append(get_data_sql);
        sql.append(" WHERE l1.id in (:userIdList)");
        sql.append(orderBy);
        params.addValue("userIdList", userIdList);
        List<LoginUser> users = queryList(sql.toString(), params);
        Page<LoginUser> accountPage = new PageImpl<LoginUser>(users, pageRequest, Optional.of(userCount).orElse(0L));
        return accountPage;
    }

    public Page<LoginUser> findAll() {
        return findAll(null, null);
    }

    public Optional<LoginUser> findByEmail(String email) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder sql = new StringBuilder(get_data_sql);
        sql.append(" WHERE email = :email");
        params.addValue("email", email);
        List<LoginUser> userList = queryList(sql.toString(), params);
        return userList.isEmpty() ? Optional.empty() : Optional.of(userList.get(0));
    }

    public LoginUser save(LoginUser sourceUser) {
        Optional<LoginUser> optLoginUser = findByEmail(sourceUser.getEmail());
        final LoginUser targetLoginUser = optLoginUser.orElse(sourceUser);
        if (optLoginUser.isPresent()) {
            targetLoginUser.setName(sourceUser.getName());
            targetLoginUser.setPassword(sourceUser.getPassword());
            targetLoginUser.setGender(sourceUser.getGender());
            targetLoginUser.setGenre(sourceUser.getGenre().get());
        }
        boolean isNew = (targetLoginUser.getId() == null || targetLoginUser.getId() == 0);
        new SecurityAuditor().getCurrentAuditor().ifPresent(auditor -> {
            if (isNew)
                targetLoginUser.setCreatedBy(auditor);
            targetLoginUser.setUpdatedBy(auditor);
        });
        HashMap<String, Object> param = new HashMap<>();
        SqlParameterSource source = new BeanPropertySqlParameterSource(targetLoginUser);

        // インサート対象のカラム名を設定
        List<String> insertColumns = Arrays.stream(source.getParameterNames())
                .filter(parameterName -> !List.of("class", "id", "roleList", "createdAt", "updatedAt")
                        .contains(parameterName))
                .collect(Collectors.toList());
        // カラム名をキャメルケースに変換／OptionalとEnumの変換
        for (String parameterName : insertColumns) {
            Object value = source.getValue(parameterName);
            Object resolvedValue = value instanceof Optional ? ((Optional<?>) value).orElse(null) : value;
            if (resolvedValue instanceof Enum)
                resolvedValue = ((Enum<?>) resolvedValue).toString();
            String snakeName = CaseUtils.toSnakeCase(parameterName);
            param.put(snakeName, resolvedValue);
        }
        if (isNew) {
            SimpleJdbcInsert insert = new SimpleJdbcInsert((JdbcTemplate) jdbcTemplate.getJdbcOperations())
                    .withTableName("login_user")
                    .usingGeneratedKeyColumns("id")
                    .usingColumns(param.keySet().toArray(new String[0]));
            Number key = insert.executeAndReturnKey(param);
            targetLoginUser.setId(key.intValue());
        } else {
            jdbcTemplate.update(
                """
                    UPDATE login_user SET
                        name       = :name,
                        password   = :password,
                        gender     = :gender,
                        genre      = :genre,
                        updated_by = :updated_by
                     WHERE email = :email
                """, param);
            jdbcTemplate.update(
                """
                    DELETE FROM user_role ur1
                        WHERE exists (
                        SELECT 1
                            FROM login_user l1
                            WHERE l1.id = ur1.user_id
                              AND l1.email = :email
                        )
                """, param);
        }
        List<Role> roleList = targetLoginUser.getRoleList();
        if (roleList != null) {
            for (Role role: roleList) {
                param.clear();
                param.put("user_id", targetLoginUser.getId());
                param.put("role_id", role.getId());
                jdbcTemplate.update(
                    """
                        INSERT INTO user_role (
                            user_id,
                            role_id
                        ) VALUES (
                            :user_id,
                            :role_id
                        )
                    """, param);
            }
        }
        optLoginUser = findByEmail(targetLoginUser.getEmail());
        return optLoginUser.get();
    }

    public void delete(String email) {
        HashMap<String, Object> param = new HashMap<>();
        param.put("email", email);
        jdbcTemplate.update(
            """
                DELETE FROM user_role ur1
                 WHERE exists (
                    SELECT 1
                      FROM login_user l1
                     WHERE l1.id = ur1.user_id
                       AND l1.email = :email
                 )
            """, param);
        jdbcTemplate.update(
            """
                DELETE FROM login_user
                    WHERE email = :email
            """, param);
        }
}
