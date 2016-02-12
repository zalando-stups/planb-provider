package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Clause;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.cassandra.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

@Component
public class CassandraRealm implements Realm {

    @Autowired
    private CassandraProperties cassandraProperties;

    @Autowired
    private Session session;

    private PreparedStatement selectUser;

    @PostConstruct
    void prepareStatements() {
        selectUser = session.prepare(
                select("password_hashes")
                        .from(cassandraProperties.getKeyspace(), "user")
                        .where(eq("username", bindMarker("username"))));
    }

    @Override
    public Map<String, Object> authenticate(final String user, final String password, final String[] scopes)
            throws RealmAuthenticationFailedException {

        // selectUser to figure out password

        return new HashMap<String, Object>() {{
            put("uid", user);
        }};
    }
}
