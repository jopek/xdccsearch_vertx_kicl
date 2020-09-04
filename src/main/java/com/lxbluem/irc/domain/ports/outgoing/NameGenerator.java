package com.lxbluem.irc.domain.ports.outgoing;

import java.util.Random;

public interface NameGenerator {
    String getNick();

    static NameGenerator getDefault() {
        return new RandomNameGenerator();
    }

    class RandomNameGenerator implements NameGenerator {
        @Override
        public String getNick() {
            return getNick(4);
        }

        public String getNick(int length) {
            StringBuilder stringBuilder = new StringBuilder();
            Random random = new Random();
            String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789";

            for (int i = 0; i < length; i++) {
                final int dictionaryIndex = i == 0 ? dictionary.length() - 10 : dictionary.length();
                stringBuilder.append(
                        dictionary.charAt(
                                random.nextInt(
                                        dictionaryIndex
                                )
                        )
                );
            }
            return stringBuilder.toString();
        }

    }
}
