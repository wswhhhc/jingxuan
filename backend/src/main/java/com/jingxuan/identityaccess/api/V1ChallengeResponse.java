package com.jingxuan.identityaccess.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "一次性算术 challenge；答案仅保存在服务端")
public record V1ChallengeResponse(
        @Schema(description = "128-bit URL-safe challenge ID", example = "AAAAAAAAAAAAAAAAAAAAAA")
        String id,
        @Schema(description = "简单加减法题目", example = "3 + 4 = ?")
        String question,
        @Schema(description = "剩余有效时间（秒）", example = "300")
        long expiresIn
) {

    public static V1ChallengeResponse from(ChallengeService.IssuedChallenge challenge) {
        return new V1ChallengeResponse(challenge.challengeId(), challenge.question(), challenge.expiresIn());
    }
}
