package com.example.demo.controller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.example.demo.entity.LoginUser;
import com.example.demo.exception.UserNotFoundException;
import com.example.demo.model.UserDeleteForm;
import com.example.demo.model.UserDeleteValidator;
import com.example.demo.model.UserForm;
import com.example.demo.model.UserSearchForm;
import com.example.demo.model.UserUpdateForm;
import com.example.demo.model.UserUpdateValidator;
import com.example.demo.service.LoginUserDetailsService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class AuthController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private LoginUserDetailsService loginUserDetailsService;
    @Autowired
    private UserUpdateValidator userUpdateValidator;
    @Autowired
    private UserDeleteValidator userDeleteValidator;

    final static Map<String, String> GENRE_ITEMS = 
        Collections.unmodifiableMap(new LinkedHashMap<String, String>() {
        {
            put("ニュース", "NEWS");
            put("金融"   , "FINANCE");
            put("スポーツ", "SPORTS");
            put("音楽"   , "MUSIC");
            put("暮らし"  , "LIFE");
        }
    });

    final static Map<String, String> ROLE_ITEMS = 
        Collections.unmodifiableMap(new LinkedHashMap<String, String>() {
        {
            put("管理者", "ROLE_ADMIN");
            put("一般"  , "ROLE_GENERAL");
        }
    });

    public AuthController() {
    }

    @InitBinder("userUpdateForm") // 注意: @ModelAttributeでフォーム名は先頭小文字で登録される
    public void validatorBinder(WebDataBinder binder) {
        binder.addValidators(userUpdateValidator);
    }

    @InitBinder("userDeleteForm") // 注意: @ModelAttributeでフォーム名は先頭小文字で登録される
    public void validatorBinder2(WebDataBinder binder) {
        binder.addValidators(userDeleteValidator);
    }

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/general")
    public String general() {
        return "general";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @RequestMapping(value = "/admin", method = {RequestMethod.GET, RequestMethod.POST})
    public String admin(@PageableDefault(page = 0, size = 2) Pageable pageable,
            @ModelAttribute @Validated UserSearchForm form, BindingResult result,
             Model model) {
        if (!result.hasErrors()) {
            Page<LoginUser> accountPage = loginUserDetailsService.getAccounts(pageable, form);
            model.addAttribute("page", accountPage);
            model.addAttribute("accounts", accountPage.getContent());
        }
        model.addAttribute("userSearchForm", form);
        model.addAttribute("roleItems", ROLE_ITEMS);
        return "admin";
    }

    private boolean isAdmin(UserDetails userDetails) {
        return userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    /** 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK） */
    private boolean checkAccessUser(UserDetails userDetails, String username) {
        if (!isAdmin(userDetails)) {
            if (!userDetails.getUsername().equals(username)) {
                logger.error(String.format("不正アクセス検出!!! 検出ユーザ:%s, アクセス先ユーザ:%s",
                userDetails.getUsername(), username));
                return false;
            }
        }
        return true;
    }

    @GetMapping("/user/{id}")
    public String displayView(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, Model model)
            throws UserNotFoundException {
        // 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK）
        if (!checkAccessUser(userDetails, id))
            return "redirect:/";
        Optional<LoginUser> optLoginUser = loginUserDetailsService.findLoginUser(id);
        if (optLoginUser.isEmpty())
            throw new UserNotFoundException(id);
        LoginUser loginUser = optLoginUser.get();
        UserForm form = UserForm.builder()
                .name(loginUser.getName())
                .gender(loginUser.getGender())
                .email(loginUser.getEmail())
                .build();
        form.setSplitGenre(loginUser.getGenre());
        model.addAttribute("userForm", form);
        model.addAttribute("genreItems", GENRE_ITEMS);
        return "user/view";
    }

    @GetMapping("/user/{id}/edit")
    public String displayEdit(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, Model model)
            throws UserNotFoundException {
        // 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK）
        if (!checkAccessUser(userDetails, id))
            return "redirect:/";
        Optional<LoginUser> optLoginUser = loginUserDetailsService.findLoginUser(id);
        if (optLoginUser.isEmpty())
            throw new UserNotFoundException(id);
        LoginUser loginUser = optLoginUser.get();
        UserUpdateForm form = UserUpdateForm.builder()
                .name(loginUser.getName())
                .gender(loginUser.getGender())
                .email(loginUser.getEmail())
                .build();
        form.setSplitGenre(loginUser.getGenre());
        model.addAttribute("userUpdateForm", form);
        model.addAttribute("genreItems", GENRE_ITEMS);
        return "user/edit";
    }

    @PostMapping(value = "/user/update")
    public String update(@ModelAttribute @Validated UserUpdateForm form, BindingResult result, Model model,
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        // 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK）
        if (!checkAccessUser(userDetails, form.getEmail()))
            return "redirect:/";
        if (result.hasErrors()) {
            model.addAttribute("genreItems", GENRE_ITEMS);
            return "user/edit";
        }
        // ユーザ情報の更新
        loginUserDetailsService.update(form);
        // 本人がパスワードを変更した場合（＝管理者が一般ユーザのパスワードを変更するケース以外）
        // メモリ上の認証情報に含まれるパスワードも更新したいので再ログインする
        if (StringUtils.hasText(form.getNewPassword()) && userDetails.getUsername().equals(form.getEmail())) {
            try {
                SecurityContextHolder.clearContext();
                request.login(userDetails.getUsername(), form.getNewPassword());
            } catch (ServletException e) {
                logger.error("login failed userId="+userDetails.getUsername(), e);
            }
        }
        return String.format("redirect:/user/%s", form.getEmail());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping(value = "/user/add")
    public String displayAdd(Model model) {
        model.addAttribute("genreItems", GENRE_ITEMS);
        UserForm form = UserForm.builder().build();
        model.addAttribute("userForm", form);
        return "user/add";
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping(value = "/user/create")
    public String create(@Validated @ModelAttribute UserForm form, BindingResult result, Model model) {
        model.addAttribute("genreItems", GENRE_ITEMS);
        if (result.hasErrors())
            return "user/add";
        try {
            // ユーザー情報の登録
            loginUserDetailsService.create(form);
        } catch(Exception e) {
            if (e.getMessage().contains("Duplicate entry")) {
                FieldError fieldError = new FieldError(result.getObjectName(), "email", "既に登録されているメールアドレスです");
                result.addError(fieldError);
                return "user/add";
            }
            logger.error("user create failed userId="+form.getEmail(), e);
            throw e;
        }
        return "redirect:/admin";
    }

    @GetMapping("/user/{id}/delete")
    public String displayDelete(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id, Model model)
            throws UserNotFoundException {
        // 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK）
        if (!checkAccessUser(userDetails, id))
            return "redirect:/";
        Optional<LoginUser> optLoginUser = loginUserDetailsService.findLoginUser(id);
        if (optLoginUser.isEmpty())
            throw new UserNotFoundException(id);
        LoginUser loginUser = optLoginUser.get();
        UserDeleteForm form = UserDeleteForm.builder()
                .name(loginUser.getName())
                .email(loginUser.getEmail())
                .build();
        model.addAttribute("userDeleteForm", form);
        return "user/delete";
    }

    @PostMapping(value = "/user/delete")
    public String delete(@ModelAttribute @Validated UserDeleteForm form, BindingResult result, Model model,
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest request) {
        // 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK）
        if (!checkAccessUser(userDetails, form.getEmail()))
            return "redirect:/";
        if (result.hasErrors())
            return "user/delete";
        // ユーザ情報の削除
        loginUserDetailsService.delete(form);

        if (isAdmin(userDetails))
            return "redirect:/admin";
        else {
            try {
                request.logout();
            } catch (ServletException e) {
                logger.error("logout failed userId="+form.getEmail(), e);
            }
            return "redirect:/";
        }
    }

    // ***************************************
    // ここから @RestAPI ↓↓
    // ***************************************
    @GetMapping("/api/V1/user/{id}")
    public ResponseEntity<Object> getUser(@AuthenticationPrincipal UserDetails userDetails, @PathVariable String id) {
        // 他人のユーザ情報を参照しようとしていないかチェック（管理者はOK）
        if (!checkAccessUser(userDetails, id))
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        Optional<LoginUser> optLoginUser = loginUserDetailsService.findLoginUser(id);
        if (optLoginUser.isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(optLoginUser.get());
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/api/V1/user/create")
    public ResponseEntity<Object> createUser(@Validated @RequestBody UserForm form, BindingResult result) {
        if (result.hasErrors())
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
        try {
            // ユーザー情報の登録
            loginUserDetailsService.create(form);
        } catch(Exception e) {
            if (e.getMessage().contains("Duplicate entry")) {
                FieldError fieldError = new FieldError(result.getObjectName(), "email", "既に登録されているメールアドレスです");
                result.addError(fieldError);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result.getAllErrors());
            }
            logger.error("user create failed userId="+form.getEmail(), e);
            return ResponseEntity.internalServerError().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}