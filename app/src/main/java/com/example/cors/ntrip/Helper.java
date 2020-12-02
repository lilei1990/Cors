package com.example.cors.ntrip;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Helper {

    /**
     * 字节数组转十六进制字符串
     */
    public static String byte2HexStr(byte[] b) {
        String stmp = "";
        StringBuilder sb = new StringBuilder("");

        for(int n = 0; n < b.length; ++n) {
            stmp = Integer.toHexString(b[n] & 255);
            sb.append(stmp.length() == 1?"0" + stmp:stmp);
            sb.append(" ");
        }

        return sb.toString().toUpperCase().trim();
    }

    /**
     * byte[]数组转换List<Byte>
     */
    public static List<Byte> toList(byte[] array) {
        return Optional.ofNullable(array).map(arr -> {

            List<Byte> result = new ArrayList<>();
            for (byte temp : arr) {
                result.add(temp);
            }
            return result;
        }).orElse(null);
    }

    /**
     * Byte列表转换为byte数组
     */
    public static byte[] toByteArray(List<Byte> list) {
        return Optional.ofNullable(list).map(li -> {
            int length = list.size();
            byte[] result = new byte[length];
            for (int i = 0; i < length; i++) {
                result[i] = list.get(i);
            }
            return result;
        }).orElse(null);
    }

    /**
     * 字符串转为double，优先进行无区域化限制转换
     * @param num 待转字符串
     * @return 转换后的实数值
     */
    public static double toDouble(String num) {
        try {
            return Optional.ofNullable(num).map(n -> Double.valueOf(n)).orElse(0.);
        } catch (NumberFormatException e) {
            DecimalFormat decimalFormat = new DecimalFormat();
            try {
                return decimalFormat.parse(num).doubleValue();
            } catch (Exception ex) {
                return 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }
}