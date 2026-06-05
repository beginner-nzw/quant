package com.quant.aiorchestrator.config;

import com.quant.common.security.AuthProperties;
import com.quant.common.security.InMemoryUserProfileSource;
import com.quant.common.security.RoleAccessAuthority;
import com.quant.common.security.ServiceActorContextFilter;
import com.quant.common.security.UserProfileSource;
import com.quant.common.security.UserContextFilter;
import com.quant.common.web.TraceIdFilter;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommonInfraConfig {

    @Bean
    public UserProfileSource userProfileSource() {
        return new InMemoryUserProfileSource();
    }

    @Bean
    public RoleAccessAuthority roleAccessAuthority(Environment environment) {
        return new RoleAccessAuthority(environment.getProperty(
                "quant.ai.role-access-config",
                "../../../ai-config/role-access-configs.json"
        ));
    }

    @Bean
    public UserContextFilter userContextFilter(Environment environment, UserProfileSource userProfileSource) {
        return new UserContextFilter(authProperties(environment), userProfileSource);
    }

    @Bean
    public ServiceActorContextFilter serviceActorContextFilter(Environment environment) {
        return new ServiceActorContextFilter(
                environment.getProperty("quant.security.service-actor.secret"),
                environment.getProperty("quant.security.service-actor.clock-skew-seconds", Long.class, 60L)
        );
    }

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    private AuthProperties authProperties(Environment environment) {
        AuthProperties properties = new AuthProperties();
        properties.setMode(environment.getProperty("quant.security.auth.mode", "JWT"));
        properties.setJwtSecret(environment.getProperty("quant.security.jwt.secret"));
        properties.setIssuer(environment.getProperty("quant.security.jwt.issuer"));
        properties.setAudience(environment.getProperty("quant.security.jwt.audience"));
        properties.setUserIdClaim(environment.getProperty("quant.security.jwt.user-id-claim", "sub"));
        properties.setRoleClaim(environment.getProperty("quant.security.jwt.role-claim", "role"));
        properties.setClockSkewSeconds(environment.getProperty("quant.security.jwt.clock-skew-seconds", Long.class, 60L));
        return properties;
    }
}
