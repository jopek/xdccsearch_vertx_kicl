package com.lxbluem.irc;

import java.util.Random;

public class NameGenerator {
    public static String getRandomNick() {
        return getRandomNick(4);
    }

    public static String getRandomNick(int length) {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
        String dictionarySecondPart = "abcdefghijklmnopqrstuvwxyz0123456789";

        for (int i = 0; i < length; i++) {
            stringBuilder.append(
                    dictionarySecondPart.charAt(
                            random.nextInt(
                                    dictionarySecondPart.length()
                            )
                    )
            );
        }
        return stringBuilder.toString();
    }

}
