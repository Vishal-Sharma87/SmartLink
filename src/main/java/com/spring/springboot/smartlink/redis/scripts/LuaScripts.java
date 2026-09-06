package com.spring.springboot.smartlink.redis.scripts;

import org.springframework.data.redis.core.script.RedisScript;

public class LuaScripts {

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
