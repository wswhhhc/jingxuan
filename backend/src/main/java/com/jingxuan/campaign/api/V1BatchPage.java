package com.jingxuan.campaign.api;

import com.jingxuan.api.V1PageInfo;

import java.util.List;

/** v1 批次分页列表响应。 */
public record V1BatchPage(V1PageInfo page, List<V1BatchDetail> items) {
}
