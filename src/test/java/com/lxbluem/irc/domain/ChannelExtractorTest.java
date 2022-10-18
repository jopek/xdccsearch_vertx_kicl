package com.lxbluem.irc.domain;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelExtractorTest {

    @Test
    void regular_topic() {
        String topic = "@notAChannel #thisIsAChannel -> #thisToo: '#ddd'";
        Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(topic);

        HashSet<String> expected = new HashSet<>(Arrays.asList("#thisisachannel", "#thistoo", "#ddd"));
        assertEquals(expected, mentionedChannels);
    }

    @Test
    void skip_help_channels() {
        String topic = "#lala #help";
        Set<String> mentionedChannels = ChannelExtractor.getMentionedChannels(topic);

        HashSet<String> expected = new HashSet<>(Arrays.asList("#lala"));
        assertEquals(expected, mentionedChannels);
    }
}
