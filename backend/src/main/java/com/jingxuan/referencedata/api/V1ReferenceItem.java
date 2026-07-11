package com.jingxuan.referencedata.api;

import com.jingxuan.entity.SysDict;

/** v1 字典项。 */
public record V1ReferenceItem(String id, String type, String label, String value, Integer sort) {
    public static V1ReferenceItem from(SysDict source) {
        return new V1ReferenceItem(source.getId().toString(), source.getDictType(), source.getDictLabel(),
                source.getDictValue(), source.getSort());
    }
}
