package com.lxbluem.irc;

import java.util.Random;

class NameGenerator {
    static String getRandomNick() {
        StringBuilder stringBuilder = new StringBuilder();
        Random random = new Random();
//        String dictionaryFirstPart = "abcdefghijklmnopqrstuvwxyz";
        String dictionarySecondPart = "abcdefghijklmnopqrstuvwxyz0123456789";

//        stringBuilder.append(
//                dictionaryFirstPart.charAt(
//                        random.nextInt(
//                                dictionaryFirstPart.length()
//                        )
//                )
//        );

        for (int i = 0; i < 4; i++) {
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
