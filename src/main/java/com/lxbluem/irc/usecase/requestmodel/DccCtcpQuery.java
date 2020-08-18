package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@lombok.AllArgsConstructor
@Builder
@Getter
public class DccCtcpQuery implements Serializable {

    private final String filename;
    private final long ip;
    private final String parsedIp;
    private final int port;
    private final long size;
    private final int token;
    private final boolean isValid;
    private final TransferType transferType;

    public enum TransferType {ACTIVE, PASSIVE}

    public static DccCtcpQuery fromQueryString(String incomingCtcpQuery) {
        Pattern pattern = Pattern.compile("DCC (SEND|ACCEPT) (\\S+) (\\d+) (\\d+) (\\d+)( \\d+)?");
        Matcher matcher = pattern.matcher(incomingCtcpQuery);

        if (!matcher.find()) {
            return DccCtcpQuery.builder()
                    .filename("")
                    .transferType(TransferType.ACTIVE)
                    .isValid(false)
                    .parsedIp("0.0.0.0")
                    .build();
        }

        int port = Integer.parseInt(matcher.group(4));

        TransferType transferType = TransferType.ACTIVE;
        int token = 0;

        if (port == 0) {
            token = Objects.nonNull(matcher.group(6)) ? Integer.parseInt(matcher.group(6).trim()) : 0;
            transferType = TransferType.PASSIVE;
        }

        return DccCtcpQuery.builder()
                .filename(matcher.group(2))
                .ip(Long.parseLong(matcher.group(3)))
                .parsedIp(transformLongToIpString(Long.parseLong(matcher.group(3))))
                .port(port)
                .size(Long.parseLong(matcher.group(5)))
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
