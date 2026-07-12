package com.jingxuan.referencedata.api;

import com.jingxuan.entity.SysDict;

/** v1 字典项。 */
public record V1ReferenceItem(String id, String type, String label, String value, Integer sort) {
}
