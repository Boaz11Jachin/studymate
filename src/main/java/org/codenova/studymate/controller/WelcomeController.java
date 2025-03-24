package org.codenova.studymate.controller;

import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.codenova.studymate.model.entity.Avatar;
import org.codenova.studymate.model.entity.StudyGroup;
import org.codenova.studymate.model.entity.StudyMember;
import org.codenova.studymate.model.entity.User;
import org.codenova.studymate.repository.AvatarRepository;
import org.codenova.studymate.repository.StudyGroupRepository;
import org.codenova.studymate.repository.StudyMemberRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

@Controller
@AllArgsConstructor
public class WelcomeController {
    private AvatarRepository avatarRepository;
    private StudyMemberRepository studyMemberRepository;
    private StudyGroupRepository studyGroupRepository;

    @RequestMapping({"/", "/index"})
    public String indexHandle(HttpSession session, Model model) {
        if (session.getAttribute("user") == null) {
            return "index";
        } else {
            User user = (User)session.getAttribute("user");
            model.addAttribute("user", user);
            // int avatarId = user.getAvatarId();

            Avatar userAvatar = avatarRepository.findById(user.getAvatarId());
            model.addAttribute("userAvatar", userAvatar);

        List<StudyMember> studyList = studyMemberRepository.studyGroupsByUserId(user.getId());
        model.addAttribute("studyList", studyList);

        for(StudyMember one : studyList){
            StudyMember sm = studyMemberRepository.findByUserIdAndGroupId(Map.of("userId", one.getUserId(), "groupId", one.getGroupId()));
            StudyGroup sg = studyGroupRepository.findById(sm.getGroupId());

            model.addAttribute(sg);
        }


            return "index-authenticated";
        }


    }
}
