package com.lms.tenant;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 커넥션을 내줄 때마다 PostgreSQL 세션 변수 app.current_tenant 를 설정한다.
 * RLS 정책이 이 값으로 행을 필터링한다.
 *
 * <p>핵심: HikariCP는 커넥션을 재사용하므로, 이전 요청이 남긴 값이 새 요청에
 * 새어 들어가지 않도록 <b>매 획득 시 항상 덮어쓴다.</b> 테넌트가 없으면(미인증 요청)
 * 어떤 실제 행과도 매칭되지 않는 sentinel(0 UUID)로 설정해 fail-closed 한다.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    /** 미인증/테넌트 없음 → 어떤 행도 보이지 않도록 하는 sentinel */
    private static final String NO_TENANT = "00000000-0000-0000-0000-000000000000";

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return prepare(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return prepare(super.getConnection(username, password));
    }

    private Connection prepare(Connection connection) throws SQLException {
        String tenant = TenantContext.get().map(Object::toString).orElse(NO_TENANT);
        // set_config(키, 값, is_local=false) — 세션 수준. 파라미터 바인딩으로 주입 안전.
        try (PreparedStatement ps = connection.prepareStatement("SELECT set_config('app.current_tenant', ?, false)")) {
            ps.setString(1, tenant);
            ps.execute();
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
