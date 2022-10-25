package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class BotNoticeEvent extends BotEvent {
    private String message;
    private String remoteNick;

    @Builder
    public BotNoticeEvent(String botNickName, String remoteNick, String noticeMessage) {
        super.bot = botNickName;
        this.remoteNick = remoteNick;
        this.message = noticeMessage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotNoticeEvent that)) return false;
        if (!super.equals(o)) return false;

        if (!getMessage().equals(that.getMessage())) return false;
        return getRemoteNick().equals(that.getRemoteNick());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getMessage().hashCode();
        result = 31 * result + getRemoteNick().hashCode();
        return result;
    }
}
