package org.codenova.studymate.controller;

import lombok.AllArgsConstructor;
import org.codenova.studymate.model.entity.*;
import org.codenova.studymate.model.vo.PostMeta;
import org.codenova.studymate.model.vo.StudyGroupWithCreator;
import org.codenova.studymate.repository.*;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/study")
@AllArgsConstructor
public class StudyController {
    private StudyGroupRepository studyGroupRepository;
    private StudyMemberRepository studyMemberRepository;
    private UserRepository userRepository;
    private PostRepository postRepository;
    private AvatarRepository avatarRepository;
    private PostReactionRepository postReactionRepository;


    @RequestMapping("/create")
    public String createHandle(@SessionAttribute("avatar") @Nullable Avatar avatar, Model model) {

        model.addAttribute("avatar", avatar);
        return "study/create";

    }

    @Transactional
    @RequestMapping("/create/verify")
    public String createVerifyHandle(@ModelAttribute StudyGroup studyGroup,
                                     @SessionAttribute("user") User user) {

        String randomId = UUID.randomUUID().toString().substring(24);

        studyGroup.setId(randomId);
        studyGroup.setCreatorId(user.getId());
        studyGroupRepository.create(studyGroup);

        StudyMember studyMember = new StudyMember();
        studyMember.setUserId(user.getId());
        studyMember.setGroupId(studyGroup.getId());
        studyMember.setRole("리더");
        studyMemberRepository.createApproved(studyMember);

        studyGroupRepository.addMemberCountById(studyGroup.getId());

        return "redirect:/study/" + randomId;
    }

    @RequestMapping("/search")
    public String searchHandle(@RequestParam("word") Optional<String> word, Model model,
                               @SessionAttribute("avatar") @Nullable Avatar avatar) {


        if (word.isEmpty()) {
            return "redirect:/";
        }
        String wordValue = word.get();
        List<StudyGroup> result = studyGroupRepository.findByNameLikeOrGoalLike("%" + wordValue + "%");
        List<StudyGroupWithCreator> convertedResult = new ArrayList<>();

        for (StudyGroup one : result) {
            User found = userRepository.findById(one.getCreatorId());

            StudyGroupWithCreator c = StudyGroupWithCreator.builder().group(one).creator(found).build();

            // StudyGroupWithCreator c = new StudyGroupWithCreator(one, found);
/*            StudyGroupWithCreator c = new StudyGroupWithCreator();
                c.setCreator(found);
                c.setGroup(one);
*/

            convertedResult.add(c);
        }

        System.out.println("search count : " + result.size());
        model.addAttribute("count", convertedResult.size());
        model.addAttribute("result", convertedResult);

        return "study/search";
    }

    // 스터디 상세보기 핸들러
    @RequestMapping("/{id}")
    public String viewHandle(@PathVariable("id") String id, Model model,
                             @SessionAttribute("user") User user, SessionStatus sessionStatus) {
        System.out.println(id);

        StudyGroup group = studyGroupRepository.findById(id);
        if (group == null) {
            return "redirect:/";
        }
        Map<String, Object> map = new HashMap<>();
        map.put("groupId", id);
        map.put("userId", user.getId());
        StudyMember status = studyMemberRepository.findByUserIdAndGroupId(map);
        if (status == null) {
            // 아직 참여한 적 없다
            model.addAttribute("status", "NOT_JOINED");
        } else if (status.getJoinedAt() == null) {
            //승인대기중
            model.addAttribute("status", "PENDING");
        } else if (status.getRole().equals("멤버")) {
            //이미 승인받아 참여중이다
            //멤버이다
            model.addAttribute("status", "MEMBER");
        } else {
            //리더이다
            model.addAttribute("status", "LEADER");
        }


        model.addAttribute("group", group);





        List<Post> posts = postRepository.findByGroupId(id);

        List<PostMeta> postMetas = new ArrayList<>();

        PrettyTime prettyTime = new PrettyTime();

        for(Post post : posts){

            long b = Duration.between(post.getWroteAt(), LocalDateTime.now()).getSeconds();
            System.out.println(b);

            PostMeta cvt = PostMeta.builder().id(post.getId())
                    .content(post.getContent())
                    .writerName(userRepository.findById(post.getWriterId()).getName())
                    .writerAvatar(avatarRepository.findById(userRepository.findById(post.getWriterId()).getAvatarId()).getImageUrl())
                    .time(prettyTime.format(post.getWroteAt()))
                    .reactions(postReactionRepository.findByPostId(post.getId()))
                    .build();
            postMetas.add(cvt);
        }

        model.addAttribute("postMetas", postMetas);

        return "study/view";
    }

    @Transactional
    @RequestMapping("/{id}/join")
    public String joinHandle(@PathVariable("id") String id, @SessionAttribute("user") User user) {

        List<StudyMember> list = studyMemberRepository.studyGroupsByUserId(user.getId());

        boolean alreadyJoined = false;

        for (StudyMember one : list) {
            if (one.getGroupId().equals(id)) {
                alreadyJoined = true;
                break;
            }
        }

        if (!alreadyJoined) {

            StudyMember s = StudyMember.builder().userId(user.getId()).groupId(id).role("멤버").build();

         /*
        StudyMember member = new StudyMember();
        member.setUserId(user.getId());
        member.setGroupId(id);
        member.setRole("멤버");
        */


            StudyGroup sg = studyGroupRepository.findById(id);
            if (sg.getType().equals("공개")) {
                studyMemberRepository.createApproved(s);
                studyGroupRepository.addMemberCountById(id);
            } else {
                studyMemberRepository.createPending(s);
            }
        }


        return "redirect:/study/" + id;
    }

    // 탈퇴 요청 처리 핸들러

    @RequestMapping("/{groupId}/leave")
    public String leaveHandle(@PathVariable("groupId") String groupId, Model model,
                              @SessionAttribute("user") User user){

        String userId = user.getId();
        Map map = Map.of("groupId", groupId, "userId", userId);

        StudyMember found = studyMemberRepository.findByUserIdAndGroupId(map);
        studyMemberRepository.deleteById(found.getId());

        studyGroupRepository.subtractMemberCountById(groupId);

        return "redirect:/";
    }

    // 신청 철회 요청 핸들러
    @RequestMapping("/{groupId}/cancel")
    public String cancelHandle(@SessionAttribute("user") User user, @PathVariable("groupId") String groupId){

        String userId = user.getId();
        Map map = Map.of("groupId", groupId, "userId", userId);

        StudyMember found = studyMemberRepository.findByUserIdAndGroupId(map);
        if(found != null && found.getJoinedAt() == null && found.getRole().equals("멤버")) {
            studyMemberRepository.deleteById(found.getId());
        }
        return "redirect:/study/" + groupId;
    }

    // 스터디 해산
    @RequestMapping("/{groupId}/remove")
    @Transactional
    public String removeHandle(@PathVariable("groupId") String groupId,
                               @SessionAttribute("user") User user){

        StudyGroup studyGroup = studyGroupRepository.findById(groupId);

        if(studyGroup != null && studyGroup.getCreatorId().equals(user.getId()) ) {

            studyMemberRepository.deleteByGroupId(groupId);
            studyGroupRepository.deleteById(groupId);
            return "redirect:/";
        }else {
            return "redirect:/study/" +groupId;
        }
    }

    @RequestMapping("/{groupId}/approve")
    public String approveHandle(@PathVariable("groupId") String groupId,
                                @RequestParam("targetUserId") String targetUserId,
                                @SessionAttribute("user") User user){

        StudyGroup studyGroup = studyGroupRepository.findById(groupId);

        if(studyGroup != null && studyGroup.getCreatorId().equals(user.getId()) ) {

            StudyMember found = studyMemberRepository.findByUserIdAndGroupId(Map.of("userId", targetUserId,
                    "groupId", groupId));

            if (found != null) {
                studyMemberRepository.updateJoinedAtById(found.getId());
                studyGroupRepository.addMemberCountById(groupId);
            }

        }
        return "redirect:/study/" + groupId;
    }

    // 그룹 내 새글 등록
    @RequestMapping("/{groupId}/post")
    public String postHandle (@PathVariable("groupId") String groupId, @ModelAttribute Post post, @SessionAttribute("user") User user){

        /*
         모델 attribute 로 파라미터는 받았을텐데, 빠진 정보들이 있을거임. 이걸 추가로 set  .
         postRepository를 이용해서 create 메서드 작성
         */


        post.setWriterId(user.getId());
        post.setWroteAt(LocalDateTime.now());

        postRepository.create(post);


        return "redirect:/study/" + groupId;
    }

    //글에 감정 남기기 요청 처리 핸들
    @RequestMapping("/{groupId}/post/{postId}/reaction")
    public String postReactionHandle (@ModelAttribute PostReaction postReaction, @SessionAttribute("user") User user){

        /*
        Map map = new HashMap();
        map.put("postId", postReaction.getPostId());
        map.put("userId", user.getId());
        */


    //  Map map = Map.of("userId", user.getId(), "postId", postReaction.getPostId());

        PostReaction found =
        postReactionRepository.findByWriterIdAndPostId(Map.of("writerId", user.getId(), "postId", postReaction.getPostId()));


        if(found == null) {
            postReaction.setWriterId(user.getId());
            postReactionRepository.create(postReaction);
        }else {
//          postRectionRepository.deleteById(found.getId());
//          postRectionRepository.create(postReaction);

//          postRectionRepository.updateFeelingById(postReaction);
        }


        return "redirect:/study/" + postReaction.getGroupId();

    }

}
