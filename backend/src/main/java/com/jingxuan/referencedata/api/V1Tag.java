package com.jingxuan.referencedata.api;

import com.jingxuan.entity.Tag;

/** v1 作品标签。 */
public record V1Tag(String id, String name, String color, String type, Integer sort) {
    public static V1Tag from(Tag source) {
        return new V1Tag(source.getId().toString(), source.getName(), source.getColor(), source.getType(), source.getSort());
    }
}
