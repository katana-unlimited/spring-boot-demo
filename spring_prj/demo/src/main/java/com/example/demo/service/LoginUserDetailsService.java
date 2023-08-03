package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.demo.entity.LoginUser;
import com.example.demo.entity.QLoginUser;
import com.example.demo.entity.QRole;
import com.example.demo.entity.Role;
import com.example.demo.repository.JdbcLoginUserRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.model.UserDeleteForm;
import com.example.demo.model.UserForm;
import com.example.demo.model.UserSearchForm;
import com.example.demo.model.UserUpdateForm;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.EntityPathBase;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQueryFactory;

@Service
public class LoginUserDetailsService implements UserDetailsService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
	private PasswordEncoder     passwordEncoder;
    private LoginUserRepository loginUserRepository;
    private RoleRepository      roleRepository;
    private JdbcLoginUserRepository jdbcLoginUserRepository;
    private JPQLQueryFactory jpaQueryFactory;

    public LoginUserDetailsService(PasswordEncoder passwordEncoder,
            LoginUserRepository loginUserRepository,
            RoleRepository roleRepository,
            LoginUserSpecification loginUserSpecification,
            JdbcLoginUserRepository jdbcLoginUserRepository,
            JPQLQueryFactory jpaQueryFactory) {
        this.passwordEncoder = passwordEncoder;
        this.loginUserRepository = loginUserRepository;
        this.roleRepository = roleRepository;
        this.jdbcLoginUserRepository = jdbcLoginUserRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || "".equals(email)) {
            throw new UsernameNotFoundException("email is empty");
        }

        Optional<LoginUser> loginUserOptional = loginUserRepository.findOptionalByEmail(email);
        return loginUserOptional.map(loginUser -> new LoginUserDetails(loginUser))
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
    }

    private <T> OrderSpecifier<?>[] createOrderSpecifiers(PageRequest pageRequest, EntityPathBase<T> entityPathBase) {
        PathBuilder<T> entityPath = new PathBuilder<>(entityPathBase.getType(), entityPathBase.getMetadata().getName());

        return pageRequest.getSort()
                .stream()
                .map(x -> {
                    String property = x.getProperty();
                    return x.isAscending() ? entityPath.getString(property).asc()
                            : entityPath.getString(property).desc();
                })
                .toArray(OrderSpecifier[]::new);
    }

    Page<LoginUser> getAccountsByQueryDSL(PageRequest pageRequest, UserSearchForm form) {
        final QLoginUser l  = new QLoginUser("l1");
        QRole r = QRole.role;
        // 検索条件の組み立て
        BooleanExpression whereClause = l.id.gt(0);
        if (StringUtils.hasText(form.getName()))
            whereClause = whereClause.and(l.name.contains(form.getName()));
        if (StringUtils.hasText(form.getEmail()))
            whereClause = whereClause.and(l.email.contains(form.getEmail()));
        if (form.getRoles() != null && form.getRoles().length > 0) {
            whereClause = whereClause.and(JPAExpressions.selectOne()
                .from(r)
                .where(
                    l.roleList.contains(r).and(r.name.in(form.getRoles())))
                .exists());
        }
        if (true) {
            List<String> genres = List.of();
            // List<String> genres = List.of("NEWS", "FINANCE", "SPORTS", "MUSIC", "LIFE");
            if (!genres.isEmpty()) {
                List<BooleanExpression> expressions = new ArrayList<>();
                for(String s: genres) {
                    expressions.add(l.genre.contains(s));
                }
                BooleanExpression[] bea = expressions.toArray(new BooleanExpression[0]);
                whereClause = whereClause.andAnyOf(bea);
            }
        }
        // PageRequestからソート条件を生成
        OrderSpecifier<?>[] orderSpecifier = createOrderSpecifiers(pageRequest, l);

        // レコード件数取得
        long userCount = jpaQueryFactory
                .selectFrom(l)
                .where(whereClause)
                .fetchCount();
        if (userCount == 0)
            return new PageImpl<LoginUser>(new ArrayList<LoginUser>(), pageRequest, 0L);
        // 指定ページのPK取得
        List<Integer> userIds = jpaQueryFactory
                .select(l.id)
                .from(l)
                .where(whereClause)
                .limit(pageRequest.getPageSize())
                .offset(pageRequest.getOffset())
                .orderBy(orderSpecifier)
                .fetch();
        // データ本体取得
        List<LoginUser> users = jpaQueryFactory
                                .selectFrom(l)
                                .leftJoin(l.roleList, r)
                                .where(l.id.in(userIds))
                                .orderBy(orderSpecifier)
                                .fetchJoin()
                                .fetch();
        logger.debug("jpaQueryFactory="+users);
        Page<LoginUser> accountPage = new PageImpl<LoginUser>(users, pageRequest, userCount);
        return accountPage;
    }

    Page<LoginUser> getAccountsByJPASpecification(PageRequest pageRequest, UserSearchForm form) {
        int id = StringUtils.hasText(form.getId()) ? Integer.parseInt(form.getId()) : 0;
        return loginUserRepository.findAll(
                Specification.where(LoginUserSpecification.equalsID(id))
                // .and(Specification.where(LoginUserSpecification.containsName(form.getName()))
                //      .or(LoginUserSpecification.containsEmail(form.getEmail())))
                .and(LoginUserSpecification.containsName(form.getName()))
                .and(LoginUserSpecification.containsEmail(form.getEmail()))
                .and(LoginUserSpecification.existsRole(form.getRoles())),
                pageRequest);
    }

    PageRequest buildPageRequest(Pageable pageable, String sortBy) {
        Direction direction = sortBy.startsWith("+") ? Direction.ASC : Direction.DESC;
        String sortKey = sortBy.substring(1);
        Sort sort = Sort.by(direction, sortKey);
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    Page<LoginUser> getAccountsByJDBC(PageRequest pageRequest, UserSearchForm form) {
        return jdbcLoginUserRepository.findAll(pageRequest, form);
    }

    @Transactional
    public Page<LoginUser> getAccounts(Pageable pageable, UserSearchForm form) {
        PageRequest     pageRequest  = buildPageRequest(pageable, form.getSortBy());
        Page<LoginUser> accountPage  = getAccountsByQueryDSL(pageRequest, form);
        // @SuppressWarnings("unused")
        // Page<LoginUser> accountPage2 = getAccountsByJPASpecification(pageRequest, form);
        // @SuppressWarnings("unused")
        // Page<LoginUser> accountPage3 = getAccountsByJDBC(pageRequest, form);
        return accountPage;
    }

    public Optional<LoginUser> findLoginUser(String email) throws UsernameNotFoundException {
        if (email == null || "".equals(email)) {
            throw new UsernameNotFoundException("email is empty");
        }
        return loginUserRepository.findOptionalByEmail(email);
    }

    @Transactional
     public void update(UserUpdateForm form) {
        Optional<LoginUser> optLoginUser = loginUserRepository.findOptionalByEmail(form.getEmail());
        if (optLoginUser.isEmpty())
            throw new UsernameNotFoundException("User not found: " + form.getEmail());
        LoginUser loginUser = optLoginUser.get();
        loginUser.setName(form.getName());
        loginUser.setGender(form.getGender());
        loginUser.setGenre(form.getGenreString());
        if (StringUtils.hasText(form.getNewPassword()))
            loginUser.setPassword(passwordEncoder.encode(form.getNewPassword()));
        loginUserRepository.save(loginUser);
    }

    @Transactional
    public void create(UserForm form) {
        Optional<Role> optRole = roleRepository.findOptionalByName("ROLE_GENERAL");
        List<Role> roleList = new ArrayList<>();
        roleList.add(optRole.get());
        LoginUser loginUser = LoginUser.builder()
                .name(form.getName())
                .email(form.getEmail())
                .password(passwordEncoder.encode(form.getPassword()))
                .gender(form.getGender())
                .genre(form.getGenreString())
                .roleList(roleList)
                .build();
        loginUserRepository.save(loginUser);
    }

    @Transactional
    public void delete(UserDeleteForm form) {
        loginUserRepository.deleteByEmail(form.getEmail());
   }
}