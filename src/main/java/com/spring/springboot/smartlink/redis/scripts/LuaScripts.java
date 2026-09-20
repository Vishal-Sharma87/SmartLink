package com.spring.springboot.smartlink.redis.scripts;

import org.springframework.data.redis.core.script.RedisScript;

public class LuaScripts {

    private static final String REVOKE_THEN_SET_REFRESH_TOKEN_SCRIPT_CONTENT = """
            local email = ARGV[1]
            local refreshToken = ARGV[2]
            local emailToTokenKeyPrefix = KEYS[1]
            local tokenToEmailKeyPrefix = KEYS[2]
            
            local refreshTokenHashKey = KEYS[3]
            local emailHashKey = KEYS[4]
            
            local emailToTokenKey = emailToTokenKeyPrefix..email
            
            local previousRefreshToken = redis.call('HGET', emailToTokenKey, refreshTokenHashKey)
            
            if previousRefreshToken ~= false then
                redis.call('DEL', tokenToEmailKeyPrefix..previousRefreshToken)
            end
            
            redis.call('HSET', emailToTokenKey, refreshTokenHashKey, refreshToken)
            redis.call('HSET', tokenToEmailKeyPrefix..refreshToken, emailHashKey, email)
            """;

    /**
     * Atomically revokes the previously stored refresh token for an email
     * and associates the new refresh token with that email.
     *
     * <p>The script performs the following operations:
     * <ol>
     *     <li>Fetches the previously stored refresh token for the email.</li>
     *     <li>Deletes the reverse mapping of the previous refresh token, if present.</li>
     *     <li>Stores the new refresh token against the email.</li>
     *     <li>Creates the reverse mapping from the new refresh token to the email.</li>
     * </ol>
     *
     * <p>Keys:
     * <ul>
     *     <li>{@code KEYS[1]} - Prefix for the email-to-token Redis hash.</li>
     *     <li>{@code KEYS[2]} - Prefix for the token-to-email Redis hash.</li>
     *     <li>{@code KEYS[3]} - Field name used to store the refresh token
     *         in the email-to-token hash.</li>
     *     <li>{@code KEYS[4]} - Field name used to store the email
     *         in the token-to-email hash.</li>
     * </ul>
     *
     * <p>Arguments:
     * <ul>
     *     <li>{@code ARGV[1]} - User email.</li>
     *     <li>{@code ARGV[2]} - New refresh token.</li>
     * </ul>
     *
     * <p>The script returns no value.
     */
    public static final RedisScript<Void> REVOKE_OTHER_THEN_SET_CURRENT_REFRESH_TOKEN_SCRIPT =
            RedisScript.of(REVOKE_THEN_SET_REFRESH_TOKEN_SCRIPT_CONTENT, Void.class);


    private static final String VALIDATE_OTP_SCRIPT_CONTENT = """
            local key = KEYS[1]
            local passedOtp = ARGV[1]

            local storedHashedOtp = redis.call('GET', key)
            if storedHashedOtp == false or storedHashedOtp ~= passedOtp then
                return 0
            end
            redis.call('DEL', key)
            return 1
            """;

    public static final RedisScript<Long> VALIDATE_OTP_SCRIPT = RedisScript.of(VALIDATE_OTP_SCRIPT_CONTENT, Long.class);

}
