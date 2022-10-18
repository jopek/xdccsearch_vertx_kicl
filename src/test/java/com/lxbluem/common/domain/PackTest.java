package com.lxbluem.common.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackTest {

    @Test
    void testEquality() {
        Pack pack1 = Pack.builder()
                .channelName("#someChannel")
                .networkName("someNetwork")
                .serverHostName("someHost")
                .nickName("someRemoteBot")
                .build();
        Pack pack2 = Pack.builder()
                .channelName("#someChannel")
                .networkName("someNetwork")
                .serverHostName("someHost")
                .nickName("someRemoteBot")
                .build();

        assertEquals(pack1, pack2);
        assertEquals(pack1.hashCode(), pack2.hashCode());
    }
}
