package org.codenova.studymate.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.codenova.studymate.model.entity.Avatar;
import org.codenova.studymate.model.entity.User;
import org.codenova.studymate.model.query.UserWithAvatar;
import org.codenova.studymate.repository.AvatarRepository;
import org.codenova.studymate.repository.LoginLogRepository;
import org.codenova.studymate.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {

    private AvatarRepository avatarRepository;
    private UserRepository userRepository;
    private LoginLogRepository loginLogRepository;

    @RequestMapping("/signup")
    public String signupHandle(Model model) {
        model.addAttribute("avatars", avatarRepository.findAll());

        return "auth/signup";
    }

    @RequestMapping("/signup/verify")
    public String signupVerifyHandle(@ModelAttribute @Valid User user, BindingResult result, Model model) {
        if (result.hasErrors()) {
            // 에러처리를 하고,
            return "auth/signup/verify-failed";
        }
        if (userRepository.findById(user.getId()) != null) {
            // 에러 처리하고
            return "auth/signup/verify-failed";
        }
        userRepository.create(user);
        // 밴드 기준으로 가입이 성공하면 로그인 처리되고, 모임 개설로 리다이렉트를 시키는 걸로 확인함.
        return "redirect:/index";
    }

    @RequestMapping("/login")
    public String loginHandle(Model model) {
        return "auth/login";
    }

    @Transactional
    @RequestMapping("/login/verify")
    public String loginVerifyHandle(@ModelAttribute User user, HttpSession session) {

        UserWithAvatar found = userRepository.findWithAvatarById(user.getId());
        if (found == null || !found.getPassword().equals(user.getPassword())) {
            return "auth/login/verify-failed";
        } else {
            //else 없어도 같은 효과
            userRepository.updateLoginCountByUserId(user.getId());
            loginLogRepository.create(user.getId());
            session.setAttribute("user", found);

            Avatar avatar = avatarRepository.findById(found.getAvatarId());
            session.setAttribute("avatar",avatar);




            return "redirect:/index";
        }
    }

    @RequestMapping("/logout")
    public String logoutHandle(HttpSession session){
        // session.removeAttribute("user");
        session.invalidate();
        return "redirect:/index";
    }

}
