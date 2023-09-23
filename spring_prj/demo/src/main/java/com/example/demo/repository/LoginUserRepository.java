package com.example.demo.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.demo.entity.LoginUser;

public interface LoginUserRepository extends JpaRepository<LoginUser, String>,
        JpaSpecificationExecutor<LoginUser> {

    @EntityGraph(attributePaths = { "roleList" })
    public Optional<LoginUser> findOptionalByEmail(String email);

    Page<LoginUser> findAll(Specification<LoginUser> spec, Pageable pageable);

    /**
     * @EntityGraphとSpecificationがそれぞれ別々のJOIN句を生成するため重複レコードを除去するために
     * distinctを後から付け加える為のメソッド。
     * （Specificationの方をexists句に変更したので現在未使用）
     */
    default Page<LoginUser> findAllDistinct(Specification<LoginUser> spec, Pageable pageable) {
        Specification<LoginUser> distinctSpec = Specification.where(spec).and(
                (root, query, criteriaBuilder) -> {
                    query.distinct(true);
                    return criteriaBuilder.conjunction(); // 無害だが「1 = 1」が条件に追加される（試してないけどnullで返せば良さそう）
                });
        return findAll(distinctSpec, pageable);
    }

    public void deleteByEmail(String email);
}
