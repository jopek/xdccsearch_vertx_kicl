package com.lxbluem.irc.usecase.requestmodel;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class BotDccInitQuery implements Serializable {
    private String message;
    private boolean passive;
    private String ip;
    private int port;
    private long size;
    private String filename;
    private int token;
    private String bot;

    public static BotDccInitQuery from(DccCtcpQuery query, String botNickName) {
        return BotDccInitQuery.builder()
                .bot(botNickName)
                .filename(query.getFilename())
                .ip(query.getParsedIp())
                .port(query.getPort())
                .passive(query.getTransferType().equals(DccCtcpQuery.TransferType.PASSIVE))
                .token(query.getToken())
                .size(query.getSize())
                .build();
    }
}
