package config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.listener.RedisMessageListenerContainer
import org.springframework.data.redis.serializer.StringRedisSerializer
import java.util.concurrent.Executors

@Configuration
class RedisConfig {

    @Bean
    fun distributedLockConfig(): ObjectMapper {
        return ObjectMapper().apply {
            // Jackson 설정을 여기에 추가할 수 있습니다.
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }


    @Bean
    fun redisTemplateConfig(connectionFactory: RedisConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            afterPropertiesSet() // 초기화 메서드 호출
        }
    }

    @Bean
    fun redisMessageListenerContainer(
        connectionFactory: RedisConnectionFactory
    ): RedisMessageListenerContainer {
        RedisMessageListenerContainer().apply {
            setConnectionFactory(connectionFactory)
            setTaskExecutor(Executors.newCachedThreadPool{
                runnable ->
                Thread(runnable).apply {
                    name = "redis-message-listener-container-${System.currentTimeMillis()}" // 스레드 이름 설정
                    isDaemon = true // 데몬 스레드로 설정
                }
            }) // 스레드 풀 설정
            setErrorHandler { t->
                // 에러 핸들러 설정
                println("Redis message listener error: ${t.message}")
                t.printStackTrace()
            }
        }
    }

}
