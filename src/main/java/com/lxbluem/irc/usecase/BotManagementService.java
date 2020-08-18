package com.lxbluem.irc.usecase;

import com.lxbluem.domain.Pack;
import com.lxbluem.irc.NameGenerator;
import com.lxbluem.irc.usecase.ports.BotManagementPort;
import com.lxbluem.irc.usecase.ports.BotPort;
import com.lxbluem.irc.usecase.ports.BotStorage;
import com.lxbluem.irc.usecase.requestmodel.BotConnectionDetails;

public class BotManagementService implements BotManagementPort {

    private final BotStorage bots;
    private final BotFactory botFactory;
    private final BotService botService;

    public BotManagementService(
            BotStorage bots,
            BotFactory botFactory,
            BotService botService) {
        this.bots = bots;
        this.botFactory = botFactory;
        this.botService = botService;
    }

    @Override
    public String startTransferOf(Pack pack) {
        String botNick = NameGenerator.getRandomNick();

        BotPort bot = botFactory.createNewInstance(botService);
        BotConnectionDetails botConnectionDetails = connectionDetailsFromPack(pack, botNick);
        bot.connect(botConnectionDetails);
        bot.joinChannel(pack.getChannelName());

        bots.save(botNick, bot);
        botService.init(botNick, pack);

        return botNick;
    }

    private BotConnectionDetails connectionDetailsFromPack(Pack pack, String botNick) {
        return BotConnectionDetails.builder()
                .botNick(botNick)
                .name("name_" + botNick)
                .user("user_" + botNick)
                .realName("realname_" + botNick)
                .serverHostName(pack.getServerHostName())
                .serverPort(pack.getServerPort())
                .build();
    }

}
