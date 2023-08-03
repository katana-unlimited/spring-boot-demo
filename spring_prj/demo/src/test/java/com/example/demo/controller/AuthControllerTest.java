package com.example.demo.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.example.demo.entity.LoginUser;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.UserDeleteForm;
import com.example.demo.model.UserForm;
import com.example.demo.model.UserSearchForm;
import com.example.demo.model.UserUpdateForm;
import com.example.demo.service.LoginUserDetails;
import com.example.demo.service.LoginUserDetailsService;
import com.example.demo.tools.TestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest // 処理が重たくなるがSecurityFilterChainを読み込ませるためしょうがない
@AutoConfigureMockMvc
@Import(AuthControllerTest. TestControllerAdvice.class)
// @WebMvcTest(AuthController.class) // @PreAuthorizeのテストがなければこちらの2セットがベター
// @ContextConfiguration
// @DataJpaTest // JPA関連のテスト用の設定を有効化
public class AuthControllerTest {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // @Autowired
    // private WebApplicationContext applicationContext;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoginUserDetailsService loginUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @ControllerAdvice
    public static class TestControllerAdvice {
        // テスト用の例外ハンドリングロジックなどを定義
        // 例外そのものをキャッチしたいので主に再スローする
        @ExceptionHandler(UserNotFoundException.class)
        public String handleUserNotFoundException(UserNotFoundException ex, Model model) throws UserNotFoundException {
            throw ex;
        }
    }

    @Before(value="setUp")
    public void setUp() {
    }

    @BeforeEach
    public void beforeEachTest() {
        // mockMvc.perform実行後も認証情報をクリアしないおまじない
        // （常に同じセキュリティコンテキストを参照する）
        // しかし、コントローラー側でrequest.loginにて新しいセキュリティコンテキストを作成しても
        // その値を参照できないので設定する意味が無いのでコメントアウト
        // mockMvc = MockMvcBuilders
        //         .webAppContextSetup(applicationContext)
        //         .apply(SecurityMockMvcConfigurers.springSecurity())
        //         .alwaysDo(result -> SecurityContextHolder.setContext(TestSecurityContextHolder.getContext()))
        //         .build();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("auth: " + (auth != null ? auth.toString() : "auth is null"));
    }

    @AfterEach
    public void afterEachTest() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("auth: " + (auth != null ? auth.toString() : "auth is null"));
    }


    private void printAuthentication(String testMethodName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String message =
                "[" + testMethodName + "]\n" +
                "class = " + auth.getClass() + "\n" +
                "name = " + auth.getName() + "\n" +
                "credentials = " + auth.getCredentials() + "\n" +
                "authorities = " + auth.getAuthorities() + "\n" +
                "principal = " + auth.getPrincipal() + "\n" +
                "loginUser = " + ((LoginUserDetails)auth.getPrincipal()).getLoginUser() + "\n" +
                "details = " + auth.getDetails();
        logger.info(message);
    }

    /**
     * トップページを表示
     */
    @Test
    @WithAnonymousUser
    public void test_index() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/");
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("index"));
    }

    /**
     * ログイン画面を表示
     */
    @Test
    @WithAnonymousUser
    public void test_login() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/login");
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("login"));
    }

    /**
     * 一般ユーザのトップ画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_general() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/general");
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("general"));
    }

    /**
     * 未ログインで一般ユーザのトップ画面を表示
     */
    @Test
    @WithAnonymousUser
    public void test_general_NG() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = get("/general");
        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * 管理者が管理者ページを表示
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_admin() throws Exception {
        printAuthentication(Thread.currentThread().getStackTrace()[1].getMethodName());
        // モックのデータを設定
        Page<LoginUser> page = new PageImpl<>(List.of(TestSupport.getGeneralUser(), TestSupport.getAdminUser()));

        // モックのサービスの振る舞いを定義
        when(loginUserDetailsService.getAccounts(any(Pageable.class), any(UserSearchForm.class))).thenReturn(page);

        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/admin");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("admin"))
            .andExpect(model().attributeExists("page", "accounts", "userSearchForm", "roleItems"));

        // モックのサービスのメソッドが特定の引数で呼び出されたことを検証
        verify(loginUserDetailsService).getAccounts(any(Pageable.class), any(UserSearchForm.class));
    }

    /**
     * 一般ユーザが管理者ページを表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_admin_roleNG() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/admin");
        mockMvc.perform(requestBuilder)
            .andExpect(status().is4xxClientError());
    }

    /**
     * 未ログインで管理者ページを表示
     */
    @Test
    @WithAnonymousUser
    public void test_admin_anonymous() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/admin");
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    /**
     * 一般ユーザが自身のユーザ情報を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayView_OK() throws Exception {
        LoginUser findUser = TestSupport.getGeneralUser();

        // モックのサービスの振る舞いを定義
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.of(findUser));

        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/" + findUser.getEmail());

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("user/view"))
            .andExpect(model().attributeExists("userForm", "genreItems"));

        // モックのサービスのメソッドが特定の引数で呼び出されたことを検証
        verify(loginUserDetailsService).findLoginUser(findUser.getEmail());
    }

    /**
     * 一般ユーザが管理者のユーザ情報を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayView_NG() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/admin@example.com");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    private void test_displayEdit() throws Exception {
        LoginUser findUser = TestSupport.getGeneralUser();

        // モックのサービスの振る舞いを定義
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.of(findUser));

        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/" + findUser.getEmail() + "/edit");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("user/edit"))
            .andExpect(model().attributeExists("userUpdateForm", "genreItems"));

        // モックのサービスのメソッドが特定の引数で呼び出されたことを検証
        verify(loginUserDetailsService).findLoginUser(findUser.getEmail());
    }

    /**
     * 一般ユーザが自身のユーザ編集画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayEdit_general() throws Exception {
        test_displayEdit();
    }

    /**
     * 管理者が他者のユーザ編集画面を表示
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_displayEdit_admin() throws Exception {
        test_displayEdit();
    }

    /**
     * 一般ユーザが管理者のユーザ編集画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayEdit_RoleNG() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/admin@example.com/edit");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    /**
     * 管理者がユーザ情報変更（成功）
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_update_admin() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserUpdateForm form = TestSupport.getUserUpdateForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).update(any(UserUpdateForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/update")
        .param("name", form.getName())
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("newPassword", form.getNewPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .param("genre", form.getGenre())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/"+form.getEmail()));

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).update(form);
    }

    /**
     * 一般ユーザが自身のユーザ情報変更（成功）
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_update_general() throws Exception {
        // セキュリティコンテキストから認証情報を取得
        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // String oldPass = (String)auth.getCredentials();

        // モックの引数で期待されるオブジェクト
        UserUpdateForm form = TestSupport.getUserUpdateForm();
        form.setEmail("general@example.com");
        LoginUser loginUser = TestSupport.getGeneralUser();
        loginUser.setPassword(passwordEncoder.encode(form.getNewPassword()));
        UserDetails userDetails = new LoginUserDetails(loginUser);

        // モックの定義
        doNothing().when(loginUserDetailsService).update(any(UserUpdateForm.class));
        when(loginUserDetailsService.loadUserByUsername(any(String.class))).thenReturn(userDetails);

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/update")
        .param("name", form.getName())
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("newPassword", form.getNewPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .param("genre", form.getGenre())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/user/"+form.getEmail()));

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).update(form);
        verify(loginUserDetailsService).loadUserByUsername(form.getEmail());

        // セキュリティコンテキストから認証情報を取得
        // auth = SecurityContextHolder.getContext().getAuthentication();
        // String newPass = (String)auth.getCredentials();

        // 本当はここで oldPass != newPass パスワードが変更されたことを比較検証したいのだが
        // コントローラー側でrequest.loginによって新たに作成されたセキュリティコンテキストを
        // 取得する術がないのでデバッガによる確認に留める
    }

    /**
     * 管理者がユーザ情報変更（入力エラー）
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_update_invalid() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserUpdateForm form = TestSupport.getUserUpdateForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).update(any(UserUpdateForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/update")
        .param("name", "") // empty error
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("newPassword", form.getNewPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .param("genre", form.getGenre())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
        .andExpect(status().isOk())
        .andExpect(view().name("user/edit"))
        .andExpect(model().attributeExists("userUpdateForm", "genreItems"));

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).update(form);
    }

    /**
     * 一般ユーザが他者のユーザ情報変更
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_update_NG() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserUpdateForm form = TestSupport.getUserUpdateForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).update(any(UserUpdateForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/update")
        .param("name", form.getName())
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("newPassword", form.getNewPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .param("genre", form.getGenre())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).update(form);
    }

    /**
     * 管理者がユーザ登録画面を表示
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_displayAdd_OK() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/add");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("user/add"))
            .andExpect(model().attributeExists("userForm", "genreItems"));
    }

    /**
     * 一般ユーザがユーザ登録画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayAdd_NG() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/add");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is4xxClientError());
    }

    /**
     * 管理者がユーザ登録（成功）
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_create_OK() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserForm form = TestSupport.getUserForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).create(any(UserForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/create")
        .param("name", form.getName())
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .param("genre", form.getGenre())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin"));

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).create(form);
    }

    /**
     * 管理者がユーザ登録（入力エラー）
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_create_invalid() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserForm form = TestSupport.getUserForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).create(any(UserForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/create")
        .param("name", "") // empty error
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("user/add"))
            .andExpect(model().attributeExists("userForm", "genreItems"));

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).create(form);
    }

    /**
     * 管理者がユーザ登録（PK重複エラー）
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_create_duplicate() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserForm form = TestSupport.getUserForm();

        // 例外を作成
        RuntimeException exception = new RuntimeException("例外が発生しました(Duplicate entry)");
        // モックの定義
        doThrow(exception).when(loginUserDetailsService).create(any(UserForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/create")
        .param("name", form.getName())
        .param("gender", form.getGender().toString())
        .param("email", form.getEmail())
        .param("password", form.getPassword())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .param("genre", form.getGenre())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
                .andExpect(view().name("user/add"))
                .andExpect(model().attributeExists("userForm", "genreItems"));

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).create(form);
    }

    /**
     * 一般ユーザが自身のユーザ削除画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayDelete_OK() throws Exception {
        LoginUser findUser = TestSupport.getGeneralUser();

        // モックのサービスの振る舞いを定義
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.of(findUser));

        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/" + findUser.getEmail() + "/delete");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("user/delete"))
            .andExpect(model().attributeExists("userDeleteForm"));

        // モックのサービスのメソッドが特定の引数で呼び出されたことを検証
        verify(loginUserDetailsService).findLoginUser(findUser.getEmail());
    }

    /**
     * 一般ユーザが管理者のユーザ削除画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayDelete_NG() throws Exception {
        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/admin@example.com/delete");

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));
    }

    /**
     * 一般ユーザが自身のユーザ削除画面を表示
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_displayDelete_notFound() throws Exception {
        LoginUser findUser = TestSupport.getGeneralUser();

        // モックのサービスの振る舞いを定義
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.empty());// ユーザなし

        // GETリクエスト
        MockHttpServletRequestBuilder requestBuilder = get("/user/" + findUser.getEmail() + "/delete");

        // リクエストを送信
        Exception exception = assertThrows(Exception.class, () -> {
          mockMvc.perform(requestBuilder)
            .andExpect(status().isOk());
         });

         // ServletException -> IllegalStateException -> 自作の例外クラス
         assertTrue(exception.getCause().getCause() instanceof UserNotFoundException);

        // モックのサービスのメソッドが特定の引数で呼び出されたことを検証
        verify(loginUserDetailsService).findLoginUser(findUser.getEmail());
    }

    /**
     * 管理者がユーザ削除（成功）
     */
    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_delete_admin() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserDeleteForm form = TestSupport.getUserDeleteForm();
        form.setPasswordConfirmation(""); // 管理者はパスワード不要

        // モックの定義
        doNothing().when(loginUserDetailsService).delete(any(UserDeleteForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/delete")
        .param("name", form.getName())
        .param("email", form.getEmail())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin"));

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).delete(form);
    }

    /**
     * 一般ユーザが自身のユーザ削除（成功）
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_delete_general() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserDeleteForm form = TestSupport.getUserDeleteForm();
        form.setEmail("general@example.com");

        // モックの定義
        doNothing().when(loginUserDetailsService).delete(any(UserDeleteForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/delete")
        .param("name", form.getName())
        .param("email", form.getEmail())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).delete(form);
    }

    /**
     * 一般ユーザが自身のユーザ削除（失敗）
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_delete_invalid() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserDeleteForm form = TestSupport.getUserDeleteForm();
        form.setEmail("general@example.com");
        form.setPasswordConfirmation("BAD password");// 不一致エラー
        // form.setPasswordConfirmation("");// 未入力エラー

        // モックの定義
        doNothing().when(loginUserDetailsService).delete(any(UserDeleteForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/delete")
        .param("name", form.getName())
        .param("email", form.getEmail())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andExpect(view().name("user/delete"))
            .andExpect(model().attributeExists("userDeleteForm"));

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).delete(form);
    }

    /**
     * 一般ユーザが他人のユーザ削除（失敗）
     */
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_delete_NG() throws Exception {
        // モックの引数で期待されるオブジェクト
        UserDeleteForm form = TestSupport.getUserDeleteForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).delete(any(UserDeleteForm.class));

        // POSTリクエストのパラメータを設定
        MockHttpServletRequestBuilder requestBuilder = post("/user/delete")
        .param("name", form.getName())
        .param("email", form.getEmail())
        .param("passwordConfirmation", form.getPasswordConfirmation())
        .with(csrf()); // CSRFトークンを含める

        // リクエストを送信
        mockMvc.perform(requestBuilder)
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/"));

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).delete(form);
    }

    // ***************************************
    // ここから @RestAPI ↓↓
    // ***************************************
    @Test
    @WithMockCustomUser(name = "general@example.com", username = "テスター", role = "GENERAL")
    public void test_getUser() throws Exception {
        LoginUser findUser = TestSupport.getGeneralUser();
        // ---------------------------------
        // 検索成功テスト
        // ---------------------------------
        // モックのサービスの振る舞いを定義
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.of(findUser));

        mockMvc.perform(get("/api/V1/user/" + findUser.getEmail()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(findUser.getId()))
                .andExpect(jsonPath("$.name").value(findUser.getName()))
                .andExpect(jsonPath("$.email").value(findUser.getEmail()))
                .andExpect(jsonPath("$.gender").value(findUser.getGender().toString()))
                .andExpect(jsonPath("$.genre").value(findUser.getGenre().get()))
                .andExpect(jsonPath("$.roleList[0].id").value(findUser.getRoleList().get(0).getId()))
                .andExpect(jsonPath("$.roleList[0].name").value(findUser.getRoleList().get(0).getName()))
                .andDo(print());

        // モックのサービスのメソッドが特定の引数で呼び出されたことを検証
        verify(loginUserDetailsService).findLoginUser(findUser.getEmail());

        // ---------------------------------
        // データなしテスト
        // ---------------------------------
        // モックのサービスの振る舞いを定義（empty）
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/V1/user/" + findUser.getEmail()))
                .andExpect(status().isNotFound())
                .andDo(print());

        // ---------------------------------
        // 一般ユーザが他人のユーザ情報を参照テスト
        // ---------------------------------
        findUser = TestSupport.getAdminUser();
        mockMvc.perform(get("/api/V1/user/" + findUser.getEmail()))
                .andExpect(status().isBadRequest())
                .andDo(print());

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).findLoginUser(findUser.getEmail());
    }

    @Test
    @WithMockUser(username = "general@example.com", roles = "GENERAL")
    public void testGetUserWithValidUser() throws Exception {
        // Arrange
        LoginUser loginUser = TestSupport.getGeneralUser();
        when(loginUserDetailsService.findLoginUser(any(String.class))).thenReturn(Optional.of(loginUser));

        // Act
        MvcResult result = mockMvc.perform(get("/api/V1/user/{id}", loginUser.getEmail()))
                .andExpect(status().isOk())
                .andReturn();

        // Assert
        String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        LoginUser responseUser = objectMapper.readValue(content, LoginUser.class);
        assertEquals(loginUser.getEmail(), responseUser.getEmail());
        assertEquals(loginUser.getName(), responseUser.getName());
    }

    @Test
    @WithMockCustomUser(name = "admin@example.com", username = "テスター", role = "ADMIN")
    public void test_createUser() throws Exception {
        // ---------------------------------
        // 未入力テスト
        // ---------------------------------
        UserForm form = UserForm.builder().build();

        // POSTリクエストのパラメータを設定
        mockMvc.perform(post("/api/V1//user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))
                .with(csrf())) // CSRFトークンを含める
                .andExpect(status().isBadRequest())
                .andDo(print());

        // メソッドが実行されていないことを検証
        verify(loginUserDetailsService, times(0)).create(form);

        // ---------------------------------
        // 登録成功テスト
        // ---------------------------------
        // モックの引数で期待されるオブジェクト
        form = TestSupport.getUserForm();

        // モックの定義
        doNothing().when(loginUserDetailsService).create(any(UserForm.class));

        // POSTリクエストのパラメータを設定
        mockMvc.perform(post("/api/V1//user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))
                .with(csrf())) // CSRFトークンを含める
                .andExpect(status().isCreated())
                .andDo(print());

        // メソッドが実行されたことを検証
        verify(loginUserDetailsService).create(form);

        // ---------------------------------
        // 重複キーテスト
        // ---------------------------------
        RuntimeException exception = new RuntimeException("例外が発生しました(Duplicate entry)");

        doThrow(exception).when(loginUserDetailsService).create(any(UserForm.class));

        mockMvc.perform(post("/api/V1//user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))
                .with(csrf()))
                .andExpect(status().isBadRequest())
                .andDo(print());

        // ---------------------------------
        // 予期せぬ例外テスト
        // ---------------------------------
        exception = new RuntimeException("例外が発生しました(Unknown error)");

        doThrow(exception).when(loginUserDetailsService).create(any(UserForm.class));

        mockMvc.perform(post("/api/V1//user/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(form))
                .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andDo(print());
    }
}
