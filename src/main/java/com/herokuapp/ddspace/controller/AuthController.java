package com.herokuapp.ddspace.controller;

import com.herokuapp.ddspace.dto.AccessTokenDTO;
import com.herokuapp.ddspace.dto.GithubUser;
import com.herokuapp.ddspace.mapper.UserMapper;
import com.herokuapp.ddspace.model.User;
import com.herokuapp.ddspace.provider.GithubProvider;
import com.herokuapp.ddspace.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Controller
@AllArgsConstructor
public class AuthController {

    private GithubProvider githubProvider;
    private UserService userService;

    @GetMapping("/callback")
    public String callback(@RequestParam(name="code") String code,
                           @RequestParam(name="state") String state,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        AccessTokenDTO accessTokenDTO = new AccessTokenDTO();
        accessTokenDTO.setClient_id(System.getenv("GITHUB_CLIENT_ID"));
        accessTokenDTO.setClient_secret(System.getenv("GITHUB_CLIENT_SECRET"));
        accessTokenDTO.setCode(code);
        accessTokenDTO.setRedirect_uri(System.getenv("GITHUB_CALLBACK_URL"));
        accessTokenDTO.setState(state);
        String accessToken = githubProvider.getAccessToken(accessTokenDTO);
        GithubUser githubUser = githubProvider.getUser(accessToken);
        if (githubUser != null) {
            // 登录成功，写 cookie 和 session
            User user = new User();
            String token = UUID.randomUUID().toString();
            user.setAccountId(String.valueOf(githubUser.getId()));
            user.setToken(token);
            user.setName(githubUser.getName());
            user.setAvatarUrl(githubUser.getAvatarUrl());
            user.setBio(githubUser.getBio());
            userService.createOrUpdate(user);
            response.addCookie(new Cookie("token", token));
        }
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request,
            HttpServletResponse response) {
        request.getSession().removeAttribute("user");
        Cookie cookie = new Cookie("token", null);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return "redirect:/";
    }
}
