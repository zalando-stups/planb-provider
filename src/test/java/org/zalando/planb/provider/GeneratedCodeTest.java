package org.zalando.planb.provider;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.zalando.planb.provider.api.User;

import com.google.common.collect.Lists;

public class GeneratedCodeTest {

    @Test
    public void toStringLines() {
        User user = new User();
        user.setPasswordHashes(Lists.newArrayList("one", "two"));
        user.setScopes("ScOPE");
        String st = user.toString();
        System.out.println(st);
        Assertions.assertThat(st).doesNotContain("\n");
    }

}
