package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.SLoginUser;
import com.example.demo.SRoles;
import com.example.demo.SUserRole;
import com.example.demo.dto.UserDTO;
import com.example.demo.entity.LoginUser;
import com.example.demo.entity.QLoginUser;
import com.example.demo.entity.QRole;
import com.example.demo.entity.Role;
import com.example.demo.model.GenderStatus;
import com.example.demo.model.UserDeleteForm;
import com.example.demo.model.UserForm;
import com.example.demo.model.UserSearchForm;
import com.example.demo.model.UserUpdateForm;
import com.example.demo.repository.JdbcLoginUserRepository;
import com.example.demo.repository.LoginUserRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.tools.TestSupport;
import com.querydsl.core.Tuple;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.group.GroupExpression;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.QBean;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQueryFactory;
import com.querydsl.jpa.sql.JPASQLQuery;
import com.querydsl.sql.MySQLTemplates;
import com.querydsl.sql.SQLExpressions;
import com.querydsl.sql.SQLQuery;
import com.querydsl.sql.SQLQueryFactory;
import com.querydsl.sql.Union;

import jakarta.persistence.EntityManager;

@SpringBootTest
@Transactional
@WithMockUser("mock_user")
public class LoginUserDetailServiceTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private LoginUserDetailsService loginUserDetailService;
    @Autowired
    private SQLQueryFactory sqlQueryFactory;
    @Autowired
    private JPQLQueryFactory jpaQueryFactory;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private LoginUserRepository loginUserRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JdbcLoginUserRepository jdbcLoginUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    public void test_loadUserByUsername() {
        String email = "admin@example.com";
        UserDetails userDetails = loginUserDetailService.loadUserByUsername(email);
        assertNotNull(userDetails);
        assertEquals(userDetails.getUsername(), email);

        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () -> {
            loginUserDetailService.loadUserByUsername(null);
        });
        assertTrue(ex.getMessage().contains("email is empty"));

        final String nothing_email = "nothing@mail.com";
        ex = assertThrows(UsernameNotFoundException.class, () -> {
            loginUserDetailService.loadUserByUsername(nothing_email);
        });
        assertTrue(ex.getMessage().contains("User not found"));
    }

    @Test
    public void test_findLoginUser() {
        String email = "admin@example.com";
        Optional<LoginUser> optLoginUser = loginUserDetailService.findLoginUser(email);
        assertThat(optLoginUser).isPresent();
        assertEquals(optLoginUser.get().getEmail(), email);

        email = "nothing@mail.com";
        optLoginUser = loginUserDetailService.findLoginUser(email);
        assertThat(optLoginUser).isNotPresent();
    }

    @Test
    public void test_getAccounts() {
        Pageable pageable  = PageRequest.of(0, 2);
        UserSearchForm form = new UserSearchForm();
        form.setName("太郎");
        PageRequest     pageRequest  = loginUserDetailService.buildPageRequest(pageable, form.getSortBy());
        Page<LoginUser> accountPage  = loginUserDetailService.getAccountsByQueryDSL(pageRequest, form);
        Page<LoginUser> accountPage2 = loginUserDetailService.getAccountsByJPASpecification(pageRequest, form);
        assertThat(accountPage.getContent()).isEqualTo(accountPage2.getContent());
        assertThat(accountPage).isEqualTo(accountPage2);
        Page<LoginUser> accountPage3 = loginUserDetailService.getAccountsByJDBC(pageRequest, form);
        assertThat(accountPage2.getContent()).isEqualTo(accountPage3.getContent());
        assertThat(accountPage2).isEqualTo(accountPage3);
    }

    private void create_loginUser() {
        // 登録データ
        UserForm form = TestSupport.getUserForm();
        // 登録前に登録データが存在しないことを確認
        Optional<LoginUser> optLoginUser = loginUserDetailService.findLoginUser(form.getEmail());
        assertThat(optLoginUser).isEmpty();

        // データ登録
        loginUserDetailService.create(form);

        // 正しく登録されたことを確認
        optLoginUser = loginUserDetailService.findLoginUser(form.getEmail());
        assertThat(optLoginUser).isPresent();
        LoginUser loginUser = optLoginUser.get();
        assertEquals(loginUser.getEmail(), form.getEmail());
        assertEquals(loginUser.getName(), form.getName());
        assertTrue(passwordEncoder.matches(form.getPassword(), loginUser.getPassword()));
        assertEquals(loginUser.getGender(), form.getGender());
        assertThat(loginUser.getGenre()).isPresent();
        assertEquals(loginUser.getGenre().get(), form.getGenreString());
    }

    @Test
    public void test_create() {
        create_loginUser();
    }

    @Test
    public void test_update() {
        // 更新データ
        UserUpdateForm form = TestSupport.getUserUpdateForm();
        // 更新前に登録データが存在しないことを確認
        Optional<LoginUser> optLoginUser = loginUserDetailService.findLoginUser(form.getEmail());
        assertThat(optLoginUser).isEmpty();

        // データが無いのにデータ更新
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class, () -> {
            loginUserDetailService.update(form);
        });
        assertTrue(ex.getMessage().contains("User not found"));

        // テストデータ登録
        create_loginUser();

        // データ更新
        loginUserDetailService.update(form);

        // 正しく更新されたことを確認
        optLoginUser = loginUserDetailService.findLoginUser(form.getEmail());
        assertThat(optLoginUser).isPresent();
        LoginUser loginUser = optLoginUser.get();
        assertEquals(loginUser.getEmail(), form.getEmail());
        assertEquals(loginUser.getName(), form.getName());
        assertTrue(passwordEncoder.matches(form.getNewPassword(), loginUser.getPassword()));
        assertEquals(loginUser.getGender(), form.getGender());
        assertThat(loginUser.getGenre()).isPresent();
        assertEquals(loginUser.getGenre().get(), form.getGenreString());
    }

    @Test
    public void test_delete() {
        // 削除データ
        UserDeleteForm form = TestSupport.getUserDeleteForm();
        // 削除前に登録データが存在しないことを確認
        Optional<LoginUser> optLoginUser = loginUserDetailService.findLoginUser(form.getEmail());
        assertThat(optLoginUser).isEmpty();

        // データが無いのにデータ削除しても例外は発生しない
        assertDoesNotThrow(() -> {
            loginUserDetailService.delete(form);
        });

        // テストデータ登録
        create_loginUser();

        // データ削除
        loginUserDetailService.delete(form);
        
        // 削除されたことを確認
        optLoginUser = loginUserDetailService.findLoginUser(form.getEmail());
        assertThat(optLoginUser).isEmpty();
    }

    @Test
    public void test_queryDSL() {
        // -----------------------------------------
        // JPAQueryFactory
        // -----------------------------------------
        final QLoginUser l  = new QLoginUser("l1");
        QRole r = QRole.role;
	    // BooleanExpression whereClause = JPAExpressions.selectOne()
	    //     .from(l2)
	    //     .leftJoin(l2.roleList, r)
        //     .where(
        //         l.id.eq(l2.id).andAnyOf(
        //             r.name.eq("ROLE_ADMIN"),
        //             r.name.eq("ROLE_ADMIN1")
		//         )
        //     ).exists();
        // LeftJoinを使わない分、こちらの方がパフォーマンスが高くなりそう
        BooleanExpression whereClause = JPAExpressions.selectOne()
            .from(r)
            .where(
                l.roleList.contains(r).andAnyOf(
                    r.name.eq("ROLE_ADMIN"),
                    r.name.eq("ROLE_ADMIN1")
                )
            ).exists();
        if (true) {
            BooleanExpression be0 = l.name.contains("太郎");
            List<String> genres = List.of("SPORTS", "LIFE");
            if (!genres.isEmpty()) {
                List<BooleanExpression> expressions = new ArrayList<>();
                for(String s: genres) {
                    expressions.add(l.genre.contains(s));
                }
                BooleanExpression[] bea = expressions.toArray(new BooleanExpression[0]);
                be0 = be0.andAnyOf(bea);
            }
            whereClause = whereClause.or(be0);
        }
        // 検索結果をNot EntitiyなDTOオブジェクトへセット
        List<UserDTO> userDTOs = jpaQueryFactory
                .select(Projections.fields(UserDTO.class, l.id, l.name))
                .from(l)
                .fetch();
        logger.debug("userDTOs="+userDTOs);

        // 検索結果をNot EntitiyなDTOオブジェクトへセット（子テーブルのリストに対応）
        userDTOs = jpaQueryFactory
                // Projections.constructor(UserDTO.class, l.id, l.name, l.roleList))
                .from(l)
                .leftJoin(l.roleList, r)
                .where(whereClause)
                // .transform(GroupBy.groupBy(l.id).as( // Map型
                .transform(GroupBy.groupBy(l.id).list(
                        Projections.constructor(
                                UserDTO.class, l.id, l.name,
                                GroupBy.list(
                                        Projections.constructor(Role.class, r.id, r.name)))));
        logger.debug("userDTOs="+userDTOs);

        // -----------------------------------------
        // SqlQueryFactory
        // -----------------------------------------
        SLoginUser sLoginUser = SLoginUser.loginUser;
        SUserRole  sUserRole  = SUserRole.userRole;
        SRoles     sRoles     = SRoles.roles;
        SLoginUser sLoginUser2 = new SLoginUser("l2");
	    BooleanExpression whereClause2 = JPAExpressions.selectOne()
	        .from(sLoginUser2)
            .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser2.id))
            .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
            .where(
                sLoginUser.id.eq(sLoginUser2.id).andAnyOf(
                    sRoles.name.eq("ROLE_ADMIN"),
                    sRoles.name.eq("ROLE_ADMIN1")
		        )
            ).exists();
        if (true) {
            BooleanExpression be0 = sLoginUser.name.contains("太郎");
            List<String> genres = List.of("SPORTS", "LIFE");
            if (!genres.isEmpty()) {
                List<BooleanExpression> expressions = new ArrayList<>();
                for(String s: genres) {
                    expressions.add(sLoginUser.genre.contains(s));
                }
                BooleanExpression[] bea = expressions.toArray(new BooleanExpression[0]);
                be0 = be0.andAnyOf(bea);
            }
            whereClause2 = whereClause2.or(be0);
        }

        var varUsers = sqlQueryFactory.select(sLoginUser.all())
            .from(sLoginUser)
            .fetch();
        logger.debug("varUsers="+varUsers);
        if (!varUsers.isEmpty())
            logger.debug("varUsers.get(0).get(sLoginUser.email)=" +
                    varUsers.get(0).get(sLoginUser.email));

        userDTOs = sqlQueryFactory
            .select(Projections.fields(UserDTO.class, sLoginUser.id, sLoginUser.name))
            .from(sLoginUser)
            .fetch();
        logger.debug("userDTOs="+userDTOs);

        // エンティテイクラスで受け取ることもできるが、親クラスのフィールドも含めて初期化するコンストラクタを用意する必要がある
        ConstructorExpression<UserDTO> userDTOProjection2 = Projections.constructor(
            UserDTO.class,
            sLoginUser.id,
            sLoginUser.name,
            GroupBy.list(
                    Projections.constructor(Role.class, sRoles.id, sRoles.name)));
        userDTOs = sqlQueryFactory
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .transform(GroupBy.groupBy(sLoginUser.id).list(userDTOProjection2));
        logger.debug("userDTOs=" + userDTOs);

        // エンティテイクラスで受け取る（Projections.fieldsを使ってセッターで親クラスのフィールドも埋める）
        // QクラスとSクラスでフィールドの型が異なっているフィールドはIllegalArgumentExceptionで弾かれてしまう
        // QBean#initFieldsから呼ばれるQBean#isAssignableFromでミスマッチ判定
        QBean<LoginUser> userDTOProjection = Projections.fields(
            LoginUser.class,
                sLoginUser.createdBy,
                // sLoginUser.createdAt, // java.lang.IllegalArgumentException: java.sql.Timestamp is not compatible with java.time.LocalDateTime
                sLoginUser.updatedBy,
                // sLoginUser.updatedAt,
                sLoginUser.id,
                sLoginUser.name,
                sLoginUser.email,
                sLoginUser.password,
                // sLoginUser.gender, // java.lang.IllegalArgumentException: java.lang.String is not compatible with com.example.demo.model.GenderStatus
                sLoginUser.genre,
                GroupBy.list(
                        Projections.constructor(Role.class, sRoles.id, sRoles.name))
                        .as(l.roleList.getMetadata().getName()));
        List<LoginUser> users = sqlQueryFactory
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .transform(GroupBy.groupBy(sLoginUser.id).list(userDTOProjection));                                        
        logger.debug("users="+users);

        // 【NG】ハッシュマップTupleで受け取ってからエンティティに詰め替える
        // Projections.fieldsでは成功するのにProjections.tupleだとRoleリストが1件のみ抽出される
        // AbstractGroupByTransformerの中で受け取りがTupleだとGOneで処理されてしまいグループ毎の
        // １件目のデータしか返さない仕様になっている
        Expression<Tuple> hash = Projections.tuple(
                sLoginUser.createdBy,
                sLoginUser.createdAt,
                sLoginUser.updatedBy,
                sLoginUser.updatedAt,
                sLoginUser.id,
                sLoginUser.name,
                sLoginUser.email,
                sLoginUser.password,
                sLoginUser.gender,
                sLoginUser.genre,
                GroupBy.list(
                    Projections.constructor(Role.class, sRoles.id, sRoles.name)
                ));
        List<Tuple> tupleList = sqlQueryFactory
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .transform(GroupBy.groupBy(sLoginUser.id).list(hash));                                        
        logger.debug("tupleList="+tupleList);
        List<HashMap<String, Object>> hashList = new ArrayList<>();
        for (Tuple tuple : tupleList) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("name", tuple.get(sLoginUser.name));
            hashMap.put("roleList", tuple.get(10, List.class));
            hashList.add(hashMap);
        }
        logger.debug("hashList="+hashList);

        // グループ集計用のハッシュGroupだと正しく集計できる
        // QueryDSLがTimestamp->LocalDateTimeの変換やString->Enum変換に対応していないので
        // 自分で変換してエンティティクラスを生成する
        GroupExpression<Tuple, List<Tuple>> roleListExpression = GroupBy.list(
                Projections.tuple(
                        sRoles.createdBy,
                        sRoles.createdAt,
                        sRoles.updatedBy,
                        sRoles.updatedAt,
                        sRoles.id,
                        sRoles.name));
        users = sqlQueryFactory
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .transform(GroupBy.groupBy(sLoginUser.id).as(
                        sLoginUser.createdBy,
                        sLoginUser.createdAt,
                        sLoginUser.updatedBy,
                        sLoginUser.updatedAt,
                        sLoginUser.id,
                        sLoginUser.name,
                        sLoginUser.email,
                        sLoginUser.password,
                        sLoginUser.gender,
                        sLoginUser.genre,
                        roleListExpression))
                .values()
                .stream() // Map<Integer, Group>のstream
                .map(group -> {
                    List<Role> roleList = group.getList(roleListExpression.getExpression())
                            .stream() // List<Tuple>のstream
                            .map(tuple -> Role.builder()
                                    .createdBy(tuple.get(sRoles.createdBy))
                                    .createdAt(tuple.get(sRoles.createdAt).toLocalDateTime())
                                    .updatedBy(tuple.get(sRoles.updatedBy))
                                    .updatedAt(tuple.get(sRoles.updatedAt).toLocalDateTime())
                                    .id(tuple.get(sRoles.id))
                                    .name(tuple.get(sRoles.name))
                                    .build())
                            .collect(Collectors.toList());
                    LoginUser loginUser = LoginUser.builder()
                            .createdBy(group.getOne(sLoginUser.createdBy))
                            .createdAt(group.getOne(sLoginUser.createdAt).toLocalDateTime())
                            .updatedBy(group.getOne(sLoginUser.updatedBy))
                            .updatedAt(group.getOne(sLoginUser.updatedAt).toLocalDateTime())
                            .id(group.getOne(sLoginUser.id))
                            .name(group.getOne(sLoginUser.name))
                            .email(group.getOne(sLoginUser.email))
                            .password(group.getOne(sLoginUser.password))
                            .gender(GenderStatus.valueOf(group.getOne(sLoginUser.gender)))
                            .genre(group.getOne(sLoginUser.genre))
                            .roleList(roleList)
                            .build();
                    return loginUser;
                }).collect(Collectors.toList());
        logger.debug("users=" + users);

        // union test1
        SQLQuery<Tuple> query1 = sqlQueryFactory
                .select(sLoginUser.id, sLoginUser.name,
                        sRoles.id.min().longValue())
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        SQLQuery<Tuple> query2 = sqlQueryFactory
                .select(sLoginUser.id, sLoginUser.name,
                        sRoles.id.max().longValue())
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        List<Tuple> unionList = sqlQueryFactory.query().unionAll(
                Arrays.asList(query1, query2)).fetch();
        logger.info("union SQL: "+unionList.toString());

        // union test2
        Union<Tuple> union = sqlQueryFactory.query().unionAll(
                Arrays.asList(query1, query2));
        unionList = sqlQueryFactory.query().unionAll(
                Arrays.asList(union, query2)).fetch();
        logger.info("union SQL2: " + unionList.toString());

        // union test3
        PathBuilder<Tuple>   unionPath = new PathBuilder<>(Tuple.class  , "t");
        PathBuilder<Integer> idPath    = new PathBuilder<>(Integer.class, "id");
        PathBuilder<String>  namePath  = new PathBuilder<>(String.class , "name");
        PathBuilder<Long>    countPath = new PathBuilder<>(Long.class   , "count");
        SubQueryExpression<Tuple> query3 = SQLExpressions
                .select(sLoginUser.id, sLoginUser.name, sRoles.id.min().longValue().as(countPath))
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        SubQueryExpression<Tuple> query4 = SQLExpressions
                .select(sLoginUser.id, sLoginUser.name, sRoles.id.max().longValue().as(countPath))
                .from(sLoginUser)
                .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
                .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .where(whereClause2)
                .groupBy(sLoginUser.id, sLoginUser.name);
        Union<Tuple> union2 = SQLExpressions.unionAll(Arrays.asList(query3, query4));
        SQLQuery<?> query = sqlQueryFactory.query();
        tupleList = query.select(idPath, namePath, countPath)
                .from(union2, unionPath)
                .where(unionPath.get(namePath).eq("管理太郎"))
                .fetch();
        logger.info("union SQL3: \n" + query.getSQL().getSQL()+"\n"+tupleList.toString());

        // Query in SQL, but project as entity:
        JPASQLQuery<?> jpaSqlQuery = new JPASQLQuery<>(entityManager, MySQLTemplates.builder().build());
        varUsers = jpaSqlQuery.select(sLoginUser.all())
            .from(sLoginUser)
            .leftJoin(sUserRole).on(sUserRole.userId.eq(sLoginUser.id))
            .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
            .orderBy(sLoginUser.id.asc()).fetch();
        logger.debug("jpaSqlQuery="+varUsers);

        // fromにRelationalPathBaseを指定したSQL文を実行した後に、
        // fromにEntityPathBaseを指定したSQL文を実行すると直前のSQL文のゴミが混入されてしまう（逆でも一緒。多分バグ）
        // 不本意だがJPASQLQueryのインスタンスを毎回生成する
        jpaSqlQuery = new JPASQLQuery<>(entityManager, MySQLTemplates.builder().build());
        SLoginUser sl = new SLoginUser(l.getMetadata().getName());
        // SQLクエリを実行してエンティティクラスで受け取れるが、INNER JOINしても関連テーブルは取得できない(LAZYのまま)
        users = jpaSqlQuery.select(l)
                .from(sl, l)
                .innerJoin(sUserRole).on(sUserRole.userId.eq(l.id))
                .innerJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
                .orderBy(l.id.asc()).fetch();
        // 【NG】transformでオブジェクトを生成しようとすると下記の例外が発生してしまう
        // JPASQLQuery java.lang.NoSuchMethodError: 'java.lang.Object org.hibernate.ScrollableResults.get(int)'
        // JPAQueryFactoryの時はバグで回避策として下記の対象方法があったが
        //   new JPAQueryFactory(JPQLTemplates.DEFAULT, entityManager);
        // JPASQLQueryの対処法は見当たらなかった。（JPAQueryFactoryとは別の問題かもしれないが）
        // 結論として、Projections.constructorを使う手間が同じなので
        // SQLでやりたい場合はsqlQueryFactoryを使う方向で進める
        // 
        // users = jpaSqlQuery.select(l)
        //         .from(l)
        //         .leftJoin(sUserRole).on(sUserRole.userId.eq(l.id))
        //         .leftJoin(sRoles).on(sRoles.id.eq(sUserRole.roleId))
        //         .orderBy(l.id.asc())
        //         .transform(GroupBy.groupBy(l.id).list(
        //                 Projections.constructor(LoginUser.class,
        //                         l.id, l.name, l.email, l.password, l.gender, l.genre,
        //                         GroupBy.list(
        //                                 Projections.constructor(Role.class, sRoles.id, sRoles.name)))));
        logger.debug("users="+users);
    }

    @Test
    public void testMixTransaction() {
        // JDBCでINSERT
        Optional<Role> optRole = roleRepository.findOptionalByName("ROLE_GENERAL");
        List<Role> roleList = new ArrayList<>();
        roleList.add(optRole.get());
        LoginUser loginUser = LoginUser.builder()
                .name("name")
                .email("test@email.com")
                .password(passwordEncoder.encode("password"))
                .gender(GenderStatus.OTHER)
                .genre("NEWS|SPORTS")
                .roleList(roleList)
                .build();
        loginUser = jdbcLoginUserRepository.save(loginUser);

        // QueryDSLでUPDATE
        loginUser.setName("updateName");
        QLoginUser qLoginUser = QLoginUser.loginUser;
        jpaQueryFactory.update(qLoginUser)
                .set(qLoginUser.name, loginUser.getName())
                .set(qLoginUser.genre, "LIFE|MUSIC")
                .where(qLoginUser.email.eq(loginUser.getEmail()))
                .execute();

        // Spring Data JPAでSELECT
        Optional<LoginUser> optLoginUser = loginUserDetailService.findLoginUser(loginUser.getEmail());
        if (optLoginUser.isPresent()) {
            LoginUser jpaLoginUser = optLoginUser.get();
            logger.debug("JPA findLoginUser="+jpaLoginUser);
            // Spring Data JPAでUPDATE
            jpaLoginUser.setName("JpaName");
            // FlushしないとJDBCで検索した時に更新結果が反映されていない
            // QueryDSLは内部的にJPAを呼び出しているので検索実行前に自動でFlushされるので整合性が担保されている
            // loginUserRepository.save(jpaLoginUser);
            // JDBCと併用する場合はsaveAndFlushが必須
            loginUserRepository.saveAndFlush(jpaLoginUser);
            // JDBCでSELECT
            Optional<LoginUser> optJdbcLoginUser = jdbcLoginUserRepository.findByEmail(jpaLoginUser.getEmail());
            assertThat(optJdbcLoginUser).isPresent();
            // JPAは@LastModifiedDateを付けたカラムにナノ秒付きの現在日時をセットしている
            // DBのDATETIME型にはナノ秒は保存されないがJPAのキャッシュにエンティテイが保持
            // されているのでJDBC経由で取得したエンティテイと比較するとナノ秒の差異で不一致になる
            // これを解決するにはJPAのキャッシュをクリアしてJPAで再取得したエンティテイと比較する
            // 必要がある
            //     entityManager.clear();
            // LoginUser jdbcLoginUser = optJdbcLoginUser.get();
            // assertThat(jdbcLoginUser).isEqualTo(jpaLoginUser);

            // QueryDSLでSELECT
            LoginUser queryDslLoginUser = jpaQueryFactory
                    .selectFrom(qLoginUser)
                    .leftJoin(qLoginUser.roleList)
                    .where(qLoginUser.email.eq(jpaLoginUser.getEmail()))
                    .fetchJoin()
                    .fetchOne();
            assertNotNull(queryDslLoginUser);
            assertThat(queryDslLoginUser).isEqualTo(jpaLoginUser);
        }
        // QueryDSLでDELETE（関連テーブルuser_roleも削除される）
        // ※関連テーブルの自動削除は@ManyToMany @JoinTableが指定されている場合のみ
        jpaQueryFactory.delete(qLoginUser)
                .where(qLoginUser.email.eq(loginUser.getEmail()))
                .execute();
    }
}

