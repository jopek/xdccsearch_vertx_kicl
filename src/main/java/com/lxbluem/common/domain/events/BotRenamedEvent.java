package com.lxbluem.common.domain.events;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

//FIXME: renameto --> newBotName
@Getter
@NoArgsConstructor
public class BotRenamedEvent extends BotEvent {
    private String renameto;
    private String message;

    @Builder
    public BotRenamedEvent(String attemptedBotName, String renameto, String serverMessages) {
        super.bot = attemptedBotName;
        this.renameto = renameto;
        this.message = serverMessages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BotRenamedEvent)) return false;
        if (!super.equals(o)) return false;

        BotRenamedEvent that = (BotRenamedEvent) o;

        if (!getRenameto().equals(that.getRenameto())) return false;
        return getMessage().equals(that.getMessage());
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getRenameto().hashCode();
        result = 31 * result + getMessage().hashCode();
        return result;
    }
}
