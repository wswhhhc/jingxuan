package com.jingxuan.identityaccess.internal.infrastructure;

import com.jingxuan.identityaccess.api.ChallengePurpose;
import com.jingxuan.identityaccess.api.ChallengeService;
import com.jingxuan.identityaccess.api.ChallengeUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/** Redis 一次性算术 challenge 适配器。 */
@Service
public class RedisChallengeService implements ChallengeService {

    static final String KEY_PREFIX = "jingxuan:v2:challenge:";
    private static final int ID_BYTES = 16;
    private static final int EXPIRES_IN_SECONDS = 300;
    private static final Duration TTL = Duration.ofSeconds(EXPIRES_IN_SECONDS);
    private static final Pattern ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{22}");
    private static final Logger log = LoggerFactory.getLogger(RedisChallengeService.class);
    private static final RedisScript<String> CONSUME_SCRIPT = RedisScript.of("""
            local value = redis.call('GET', KEYS[1])
            if value then
                redis.call('DEL', KEYS[1])
            end
            return value
            """, String.class);

    private final StringRedisTemplate redis;
    private final SecureRandom secureRandom;

    @Autowired
    public RedisChallengeService(StringRedisTemplate redis) {
        this(redis, new SecureRandom());
    }

    RedisChallengeService(StringRedisTemplate redis, SecureRandom secureRandom) {
        this.redis = Objects.requireNonNull(redis, "redis");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom");
    }

    @Override
    public IssuedChallenge issue(ChallengePurpose purpose) {
        if (purpose == null) {
            throw new IllegalArgumentException("challenge 用途不能为空");
        }

        ArithmeticQuestion question = newQuestion();
        String challengeId = newId();
        try {
            redis.opsForValue().set(key(challengeId), encode(purpose, question.answer()), TTL);
        } catch (RuntimeException exception) {
            log.error("challenge Redis issue 失败 ({})", exception.getClass().getSimpleName());
            throw unavailableStore();
        }
        return new IssuedChallenge(challengeId, question.text(), EXPIRES_IN_SECONDS);
    }

    @Override
    public boolean verifyAndConsume(String challengeId, ChallengePurpose purpose, Integer answer) {
        if (!isValidId(challengeId) || purpose == null || answer == null) {
            return false;
        }

        String stored;
        try {
            stored = redis.execute(CONSUME_SCRIPT, List.of(key(challengeId)));
        } catch (RuntimeException exception) {
            log.error("challenge Redis consume 失败 ({})", exception.getClass().getSimpleName());
            throw unavailableStore();
        }
        if (stored == null) {
            return false;
        }

        StoredAnswer expected = decode(stored);
        return expected != null && expected.purpose() == purpose && expected.answer() == answer;
    }

    private ArithmeticQuestion newQuestion() {
        int first = secureRandom.nextInt(20) + 1;
        int second = secureRandom.nextInt(20) + 1;
        if (secureRandom.nextInt(2) == 0) {
            return new ArithmeticQuestion(first + " + " + second + " = ?", first + second);
        }
        int left = Math.max(first, second);
        int right = Math.min(first, second);
        return new ArithmeticQuestion(left + " - " + right + " = ?", left - right);
    }

    private String newId() {
        byte[] bytes = new byte[ID_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean isValidId(String challengeId) {
        return challengeId != null && ID_PATTERN.matcher(challengeId).matches();
    }

    private static String encode(ChallengePurpose purpose, int answer) {
        return purpose.name() + ":" + answer;
    }

    private static StoredAnswer decode(String stored) {
        int separator = stored.indexOf(':');
        if (separator <= 0 || separator != stored.lastIndexOf(':') || separator == stored.length() - 1) {
            return null;
        }
        try {
            return new StoredAnswer(ChallengePurpose.valueOf(stored.substring(0, separator)),
                    Integer.parseInt(stored.substring(separator + 1)));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String key(String challengeId) {
        return KEY_PREFIX + challengeId;
    }

    private static ChallengeUnavailableException unavailableStore() {
        return new ChallengeUnavailableException();
    }

    private record ArithmeticQuestion(String text, int answer) {
    }

    private record StoredAnswer(ChallengePurpose purpose, int answer) {
    }
}
