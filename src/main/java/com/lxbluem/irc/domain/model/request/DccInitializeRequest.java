package com.lxbluem.irc.domain.model.request;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class DccInitializeRequest implements Serializable {
    private String message;
    private boolean passive;
    private String ip;
    private int port;
    private long size;
    private long continueFromPosition;
    private String filename;
    private int token;
    private String bot;

    public static DccInitializeRequest from(
            CtcpDccSend query,
            String botNickName,
            String resolvedFilename,
            long continueFromPosition) {
        return DccInitializeRequest.builder()
                .bot(botNickName)
                .filename(resolvedFilename)
                .ip(query.getParsedIp())
                .port(query.getPort())
                .passive(query.getTransferType().equals(CtcpDccSend.TransferType.PASSIVE))
                .token(query.getToken())
                .size(query.getSize())
                .continueFromPosition(continueFromPosition)
                .build();
    }
}
