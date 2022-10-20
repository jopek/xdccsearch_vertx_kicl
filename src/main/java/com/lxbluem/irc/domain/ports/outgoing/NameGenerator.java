package com.lxbluem.irc.domain.ports.outgoing;

import java.util.Random;

public interface NameGenerator {
    String getNick();

    static NameGenerator getDefault() {
        return new RandomNameGenerator();
    }

    class RandomNameGenerator implements NameGenerator {
        private final Random random = new Random();

        @Override
        public String getNick() {
            return getNick(4);
        }

        public String getNick(int length) {
            StringBuilder stringBuilder = new StringBuilder();
            String dictionary = "abcdefghijklmnopqrstuvwxyz0123456789";

            for (int i = 0; i < length; i++) {
                final int dictionaryBound = i == 0 ? dictionary.length() - 10 : dictionary.length();
                stringBuilder.append(
                        dictionary.charAt(
                                random.nextInt(
                                        dictionaryBound
                                )
                        )
                );
            }
            return stringBuilder.toString();
        }

    }
}
