package org.codenova.studymate.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.codenova.studymate.model.entity.Avatar;
import org.codenova.studymate.model.entity.LoginLog;
import org.codenova.studymate.model.entity.User;
import org.codenova.studymate.repository.AvatarRepository;
import org.codenova.studymate.repository.LoginLogRepository;
import org.codenova.studymate.repository.UserRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.SessionAttribute;

import java.util.List;

@Controller
@RequestMapping("/my")
@AllArgsConstructor
public class MyController {

    private AvatarRepository avatarRepository;
    private UserRepository userRepository;
    private LoginLogRepository loginLogRepository;

    @RequestMapping("/profile")
    public String profileHandle(HttpSession session, Model model,
                                @SessionAttribute("user") @Nullable User user){

        // User user = (User)session.getAttribute("user");
       if(user == null){
           return "redirect:/auth/login";
       }

        model.addAttribute("user", user);

        LoginLog latestLogin = loginLogRepository.findLatestByUserId(user.getId() );
        model.addAttribute("latestLogin", latestLogin);
        List<LoginLog> loginList = loginLogRepository.findByUserId(user.getId() );
        model.addAttribute("loginList", loginList);

        Avatar avatar = (Avatar)session.getAttribute("avatar");
        model.addAttribute("avatar", avatar);

        return "my/profile";
    }
}
