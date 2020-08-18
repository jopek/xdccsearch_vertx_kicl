package com.lxbluem.irc.usecase.requestmodel;

import org.junit.Test;

import static org.junit.Assert.*;

public class DccCtcpQueryTest {
    @Test
    public void createValidDccCtcpQuery() {
        String incomingDccCtcpQuery = "DCC SEND test1.bin 3232260964 50000 6";

        DccCtcpQuery dccCtcpQuery = DccCtcpQuery.fromQueryString(incomingDccCtcpQuery);

        assertNotNull(dccCtcpQuery);
        assertEquals("test1.bin", dccCtcpQuery.getFilename());
        assertEquals(3232260964L, dccCtcpQuery.getIp());
        assertEquals("192.168.99.100", dccCtcpQuery.getParsedIp());
        assertEquals(50000L, dccCtcpQuery.getPort());
        assertEquals(6L, dccCtcpQuery.getSize());
        assertEquals(DccCtcpQuery.TransferType.ACTIVE, dccCtcpQuery.getTransferType());
        assertEquals(0, dccCtcpQuery.getToken());
        assertTrue(dccCtcpQuery.isValid());
    }

    @Test
    public void createValidDccCtcpQuery_from_invalid_input() {
        String incomingDccCtcpQuery = "asd";

        DccCtcpQuery dccCtcpQuery = DccCtcpQuery.fromQueryString(incomingDccCtcpQuery);

        assertNotNull(dccCtcpQuery);
        assertEquals("", dccCtcpQuery.getFilename());
        assertEquals(0, dccCtcpQuery.getIp());
        assertEquals("0.0.0.0", dccCtcpQuery.getParsedIp());
        assertEquals(0, dccCtcpQuery.getPort());
        assertEquals(0, dccCtcpQuery.getSize());
        assertEquals(DccCtcpQuery.TransferType.ACTIVE, dccCtcpQuery.getTransferType());
        assertEquals(0, dccCtcpQuery.getToken());
        assertFalse(dccCtcpQuery.isValid());
    }

    @Test
    public void parseIp() {
        String ipString = DccCtcpQuery.transformLongToIpString(3232260865L);
        System.out.println(ipString);
    }
}