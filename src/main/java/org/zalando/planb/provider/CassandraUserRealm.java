package org.zalando.planb.provider;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.datastax.driver.core.querybuilder.QueryBuilder.*;

@Component
public class CassandraUserRealm implements UserRealm {

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
    public Map<String, Object> authenticate(final String username, final String password, final String[] scopes)
            throws RealmAuthenticationException {

        // selectUser to figure out password


        final ResultSet result = session.execute(selectUser.bind().setString("username", username));
        final Row row = result.one();
        final Set<String> passwordHashes = row.getSet("password_hashes", String.class);

        // TODO put password_hash to the query and find

        return new HashMap<String, Object>() {{
            put("uid", username);
        }};
    }
}
