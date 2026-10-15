package springproject.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // 자동 설정에서 Builder를 제공하지 않는 환경에 HTTP 클라이언트 생성기를 등록한다.
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
