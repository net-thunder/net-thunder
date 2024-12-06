package io.jaspercloud.sdwan.tranport;

import io.jaspercloud.sdwan.support.ClientVersion;
import org.junit.jupiter.api.Test;

public class ClientVersionTest {

    @Test
    public void test() {
        long version = ClientVersion.parseLong(ClientVersion.encode(145, 10, 32));
        System.out.println();
    }
}
