package com.jingxuan.api;

import java.util.List;

/** v1 列表资源统一响应。 */
public record V1Page<T>(List<T> items, V1PageInfo pageInfo) {
}
