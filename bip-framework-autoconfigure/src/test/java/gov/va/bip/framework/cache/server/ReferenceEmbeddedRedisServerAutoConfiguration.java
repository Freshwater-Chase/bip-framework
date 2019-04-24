package gov.va.bip.framework.cache.server;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import gov.va.bip.framework.cache.autoconfigure.BipRedisCacheProperties;
import gov.va.bip.framework.cache.autoconfigure.server.BipEmbeddedRedisServer;

/**
 *
 * @author rthota
 *
 */
@Configuration
public class ReferenceEmbeddedRedisServerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public BipEmbeddedRedisServer bipEmbeddedRedisServer() {
		return new BipEmbeddedRedisServer();
	}

	@Bean
	@ConditionalOnMissingBean
	public RedisProperties redisProperties() {
		return new RedisProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public BipRedisCacheProperties bipRedisCacheProperties() {
		return new BipRedisCacheProperties();
	}
}