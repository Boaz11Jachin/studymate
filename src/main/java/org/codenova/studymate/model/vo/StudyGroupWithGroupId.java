package org.codenova.studymate.model.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class StudyGroupWithGroupId {
    private String groupId;
    private String groupName;
}
