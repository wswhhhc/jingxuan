package com.jingxuan.identityaccess.api;

import java.util.List;

/** 用户物理删除前的关联数据影响清单。 */
public record V1UserDeletionImpact(String resourceType, String resourceId, long referenceCount,
                                   List<String> references, boolean deletionBlocked) {
}
