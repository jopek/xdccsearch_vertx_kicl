package com.lxbluem.irc.domain.model.request;

import org.junit.jupiter.api.Test;

import static com.lxbluem.irc.domain.model.request.CtcpDccSend.TransferType.PASSIVE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CtcpDccSendTest {
    @Test
    void createValidDccSendCtcpQuery() {
        //                             DCC SEND <filename> <sender-ip> 0 <filesize> <token>
        String incomingDccCtcpQuery = "DCC SEND test1.bin 3232260964 50000 6";

        CtcpDccSend ctcpDccSend = CtcpDccSend.fromQueryString(incomingDccCtcpQuery);

        assertNotNull(ctcpDccSend);
        assertEquals("test1.bin", ctcpDccSend.getFilename());
        assertEquals(3232260964L, ctcpDccSend.getIp());
        assertEquals("192.168.99.100", ctcpDccSend.getParsedIp());
        assertEquals(50000L, ctcpDccSend.getPort());
        assertEquals(6L, ctcpDccSend.getSize());
        assertEquals(CtcpDccSend.TransferType.ACTIVE, ctcpDccSend.getTransferType());
        assertEquals(0, ctcpDccSend.getToken());
        assertTrue(ctcpDccSend.isValid());
    }

    @Test
    void createValidReverseDccSendCtcpQuery() {
        //                             DCC SEND <filename> <sender-ip> 0 <filesize> <token>
        String incomingDccCtcpQuery = "DCC SEND test1.bin 3232260964 0 6 111";

        CtcpDccSend ctcpDccSend = CtcpDccSend.fromQueryString(incomingDccCtcpQuery);

        assertNotNull(ctcpDccSend);
        assertEquals("test1.bin", ctcpDccSend.getFilename());
        assertEquals(3232260964L, ctcpDccSend.getIp());
        assertEquals("192.168.99.100", ctcpDccSend.getParsedIp());
        assertEquals(0, ctcpDccSend.getPort());
        assertEquals(6L, ctcpDccSend.getSize());
        assertEquals(PASSIVE, ctcpDccSend.getTransferType());
        assertEquals(111, ctcpDccSend.getToken());
        assertTrue(ctcpDccSend.isValid());
    }

    private boolean isSend(String message) {
        String[] split = message.trim().split("\\s+");
        return split[1].equals("SEND");
    }

    private boolean isAccept(String message) {
        String[] split = message.trim().split("\\s+");
        return split[1].equals("ACCEPT");
    }

    @Test
    void name() {
        assertTrue(isAccept("DCC ACCEPT test1.bin 0 6 1"));
        assertFalse(isSend("DCC ACCEPT test1.bin 0 6 1"));

        assertFalse(isAccept("DCC SEND test1.bin 3232260964 50000 6"));
        assertTrue(isSend("DCC SEND test1.bin 3232260964 50000 6"));

    }

    @Test
    void createValidDccCtcpQuery_from_invalid_input() {
        String incomingDccCtcpQuery = "asd";

        CtcpDccSend ctcpDccSend = CtcpDccSend.fromQueryString(incomingDccCtcpQuery);

        assertNotNull(ctcpDccSend);
        assertEquals("", ctcpDccSend.getFilename());
        assertEquals(0, ctcpDccSend.getIp());
        assertEquals("0.0.0.0", ctcpDccSend.getParsedIp());
        assertEquals(0, ctcpDccSend.getPort());
        assertEquals(0, ctcpDccSend.getSize());
        assertEquals(CtcpDccSend.TransferType.ACTIVE, ctcpDccSend.getTransferType());
        assertEquals(0, ctcpDccSend.getToken());
        assertFalse(ctcpDccSend.isValid());
    }

    @Test
    void parseIp() {
        String ipString = CtcpDccSend.transformLongToIpString(3232260865L);
        System.out.println(ipString);

        assertNotNull(ipString);
    }
}
