package org.codenova.studymate.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.codenova.studymate.model.LoginLog;
import org.codenova.studymate.model.User;
import org.codenova.studymate.repository.AvatarRepository;
import org.codenova.studymate.repository.LoginLogRepository;
import org.codenova.studymate.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/my")
@AllArgsConstructor
public class MyController {

    private AvatarRepository avatarRepository;
    private UserRepository userRepository;
    private LoginLogRepository loginLogRepository;

    @RequestMapping("/profile")
    public String profileHandle(HttpSession session, Model model){
        User user = (User)session.getAttribute("user");
        model.addAttribute("user", user);

        LoginLog latestLogin = loginLogRepository.findLatestByUserId(user.getId() );
        model.addAttribute("latestLogin", latestLogin);
        List<LoginLog> loginList = loginLogRepository.findByUserId(user.getId() );
        model.addAttribute("loginList", loginList);


        return "my/profile";
    }
}
