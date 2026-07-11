package com.jingxuan.referencedata.api;

import java.util.List;

/** 删除共享根前返回的引用影响清单。 */
public record V1DeletionImpact(String resourceType, String resourceId, long referenceCount, List<String> references) {
}
