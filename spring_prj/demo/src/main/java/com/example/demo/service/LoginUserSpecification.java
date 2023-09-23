package com.example.demo.service;

import java.util.Arrays;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.LoginUser_;
import com.example.demo.entity.Role;
import com.example.demo.entity.Role_;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

@Component
public class LoginUserSpecification {

    static public Specification<LoginUser> join() {
        return (root, query, cb) -> {
            // ページング処理と組み合わせると、カウント用SQLで関連テーブルを使用していないエラーが出るので
            // 戻り値の型が Long.class 以外の場合に結合するようにした。
            // https://hepokon365.hatenablog.com/entry/2021/12/31/160502
            // https://stackoverflow.com/questions/29348742/spring-data-jpa-creating-specification-query-fetch-joins
            // ここでは Long.class 以外に long.class とも比較しているが、 Long.class だけで動いている。
            if (Long.class != query.getResultType()) {
                root.fetch(LoginUser_.roleList, JoinType.LEFT);
            }
            return null;
        };
    }

    static public Specification<LoginUser> equalsID(Integer id) {
        return id == 0 ? null
                : (root, query, cb) -> cb.equal(root.get(LoginUser_.id), id);
    }

    static public Specification<LoginUser> containsName(String name) {
        return !StringUtils.hasText(name) ? null
                : (root, query, cb) -> cb.like(root.get(LoginUser_.name), "%" + name + "%");
    }

    static public Specification<LoginUser> containsEmail(String email) {
        return !StringUtils.hasText(email) ? null
                : (root, query, cb) -> cb.like(root.get(LoginUser_.email), "%" + email + "%");
    }

    /**
     * @EntityGraphとSpecificationがそれぞれ別々のJOIN句を生成するため、
     * distinctを付けないと結果を正しく取得できないので
     * #existsRoleメソッドを利用するように変更しました
     */
    @Deprecated
    static public Specification<LoginUser> inRole(String[] roles) {
        return roles == null || roles.length == 0 ? null
                : (root, query, cb) -> cb.in(root.join(LoginUser_.roleList, JoinType.INNER)
                    .get(Role_.NAME)).value(Arrays.asList(roles));
    }

    static public Specification<LoginUser> existsRole(String[] roles) {
        return roles == null || roles.length == 0 ? null
                : (root, query, cb) -> {
                    Subquery<Integer> subquery = query.subquery(Integer.class);
                    Root<LoginUser> subRoot = subquery.from(LoginUser.class);
                    ListJoin<LoginUser, Role> subRole = subRoot.join(LoginUser_.roleList, JoinType.INNER);
                    return cb.exists(subquery.select(cb.literal(1))
                    .where(
                        cb.and(
                            cb.equal(root.get(LoginUser_.id), subRoot.get(LoginUser_.id)),
                            cb.in(subRole.get(Role_.NAME)).value(Arrays.asList(roles)))
                    ));
                };
    }
}