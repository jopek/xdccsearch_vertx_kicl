package com.lxbluem.irc.domain;

import java.util.HashSet;
import java.util.Set;

public class ChannelExtractor {

    public static Set<String> getMentionedChannels(String topic) {
        Set<String> channels = new HashSet<>();

        for (String rawTokens : topic.toLowerCase().split("[\\[\\], ]")) {
            String token = rawTokens.replaceAll("[^a-zA-Z#_-]", "");

            if (token.isEmpty()) {
                continue;
            }

            if (!token.startsWith("#")) {
                continue;
            }

            if (token.length() == 1) {
                continue;
            }

            if (token.contains("help")) {
                continue;
            }

            channels.add(token);
        }

        return channels;
    }

}
