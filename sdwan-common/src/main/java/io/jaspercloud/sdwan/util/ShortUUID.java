package io.jaspercloud.sdwan.util;

import java.util.UUID;

public class ShortUUID {

    private static String[] chars = new String[]{
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"
    };

    public static String gen() {
        StringBuilder builder = new StringBuilder();
        String uuid = UUID.randomUUID().toString().replaceAll("\\-", "");
        for (int i = 0; i < 8; i++) {
            String str = uuid.substring(i * 4, i * 4 + 4);
            int strInteger = Integer.parseInt(str, 16);
            builder.append(chars[strInteger % 0x3E]);
        }
        return builder.toString();
    }
}
