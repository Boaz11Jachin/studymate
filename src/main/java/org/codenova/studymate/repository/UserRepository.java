package org.codenova.studymate.repository;

import lombok.AllArgsConstructor;
import org.codenova.studymate.model.User;
import org.mybatis.spring.SqlSessionTemplate;

@AllArgsConstructor
public class UserRepository {
    private SqlSessionTemplate template;

    public User findById(String id) {
        return template.selectOne("user.findById", id);
    }

    public int create(User user){
        return template.insert("user.create", user);
    }

}
