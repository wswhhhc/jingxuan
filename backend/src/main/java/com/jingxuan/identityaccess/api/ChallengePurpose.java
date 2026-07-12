package com.jingxuan.identityaccess.api;

import io.swagger.v3.oas.annotations.media.Schema;

/** 一次性算术 challenge 的使用场景。 */
@Schema(description = "challenge 用途")
public enum ChallengePurpose {
    LOGIN,
    GUEST_COMMENT
}
