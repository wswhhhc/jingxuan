package com.jingxuan.identityaccess.api;

/** 可由登录和游客评论复用的一次性 challenge 公共用例。 */
public interface ChallengeService {

    IssuedChallenge issue(ChallengePurpose purpose);

    /**
     * 原子消费并校验 challenge。无论答案或用途是否匹配，只要 challenge 存在就会被消费。
     */
    boolean verifyAndConsume(String challengeId, ChallengePurpose purpose, Integer answer);

    record IssuedChallenge(String challengeId, String question, long expiresIn) {
    }
}
