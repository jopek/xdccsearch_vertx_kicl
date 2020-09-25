package com.lxbluem.irc.domain.model.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.util.StringJoiner;

@AllArgsConstructor
@Builder
@Getter
@ToString
public class CtcpDccSend implements Serializable {

    private final String filename;
    private final long ip;
    private final String parsedIp;
    private final int port;
    private final long size;
    private final int token;
    private final boolean isValid;
    private final TransferType transferType;

    public enum TransferType {ACTIVE, PASSIVE}

    //       0   1       2          3          4        5        6
    //      DCC SEND   <filename> <sender-ip> <port> <filesize> <token>
    //      DCC ACCEPT <filename> 0 <position> <token>
//(ACCEPT) (\S+) (\d+) (\d+)( \d+)?)

    public static CtcpDccSend fromQueryString(String incomingCtcpQuery) {
        String[] ctcp = incomingCtcpQuery.trim().split("\\s+");

        if (!(ctcp.length == 6 || ctcp.length == 7) || !ctcp[1].equalsIgnoreCase("send")) {
            return CtcpDccSend.builder()
                    .filename("")
                    .transferType(TransferType.ACTIVE)
                    .isValid(false)
                    .parsedIp("0.0.0.0")
                    .build();
        }

        int port = Integer.parseInt(ctcp[4]);

        TransferType transferType = TransferType.ACTIVE;
        int token = 0;

        if (port == 0) {
            token = ctcp.length == 7 ? Integer.parseInt(ctcp[6]) : 0;
            transferType = TransferType.PASSIVE;
        }

        return CtcpDccSend.builder()
                .filename(ctcp[2])
                .ip(Long.parseLong(ctcp[3]))
                .parsedIp(transformLongToIpString(Long.parseLong(ctcp[3])))
                .port(port)
                .size(Long.parseLong(ctcp[5].trim()))
                .transferType(transferType)
                .token(token)
                .isValid(true)
                .build();
    }

    static String transformLongToIpString(long ip) {
        StringJoiner joiner = new StringJoiner(".");
        for (int i = 3; i >= 0; i--) {
            joiner.add(String.valueOf(ip >> 8 * i & 0xff));
        }
        return joiner.toString();
    }

}
