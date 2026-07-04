package com.lms.config;

import com.lms.tenant.TenantAwareDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;

/**
 * 애플리케이션 데이터소스 조립.
 *
 * <pre>
 *   LazyConnectionDataSourceProxy   ← 트랜잭션이 실제 쿼리를 칠 때만 물리 커넥션 획득
 *     └─ TenantAwareDataSource      ← 커넥션 획득 시 app.current_tenant 설정
 *          └─ HikariDataSource      ← lms_app 롤로 접속 (RLS 대상)
 * </pre>
 *
 * Flyway는 이 데이터소스를 쓰지 않는다. spring.flyway.* 설정으로 소유자(lms_owner)
 * 전용 데이터소스를 따로 만들어 마이그레이션/DDL을 수행한다.
 */
@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties appDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProperties appDataSourceProperties) {
        DataSource hikari = appDataSourceProperties
                .initializeDataSourceBuilder()
                .build();
        return new LazyConnectionDataSourceProxy(new TenantAwareDataSource(hikari));
    }
}
