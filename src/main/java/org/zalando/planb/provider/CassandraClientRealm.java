package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

@Component
@Scope("prototype")
public class CassandraClientRealm implements ClientRealm {

    @Autowired
    private CassandraProperties cassandraProperties;

    @Autowired
    private Session session;

    private String realmName;

    @Override
    public void initialize(String realmName) {
        this.realmName = realmName;
    }

    @Override
    public void authenticate(String clientId, String clientSecret, String[] scopes)
            throws RealmAuthenticationException, RealmAuthorizationException {
        // TODO look up clientId in this.realmName and compare the clientSecret and scopes
    }
}
