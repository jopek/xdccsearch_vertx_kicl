package com.lxbluem.irc;

import java.util.Random;

public class NameGenerator {
    public static String getRandomNick() {
        return getRandomNick(4);
    }

    public static String getRandomNick(int length) {
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
