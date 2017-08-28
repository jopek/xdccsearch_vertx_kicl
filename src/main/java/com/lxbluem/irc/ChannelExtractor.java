package com.lxbluem.irc;

import java.util.HashSet;
import java.util.Set;

class ChannelExtractor {

    Set<String> getMentionedChannels(String topic) {
        Set<String> channels = new HashSet<>();

        for (String rawTokens : topic.toLowerCase().split("[\\[\\], ]")) {
            String token = rawTokens.replaceAll("[^a-zA-Z#_-]", "");

            if (token.isEmpty()) {
                continue;
            }

            if (!token.startsWith("#")) {
                continue;
            }

            if (token.contains("help")) {
                continue;
            }

            if (!channels.contains(token)) {
                channels.add(token);
            }
        }

        return channels;
    }

}
