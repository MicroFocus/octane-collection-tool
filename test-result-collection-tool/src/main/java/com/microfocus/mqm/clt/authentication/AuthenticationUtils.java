package com.microfocus.mqm.clt.authentication;

public class AuthenticationUtils {

    public static byte[] mergeArrays(byte[] ...arrays) {
        int resultLength = 0;

        for (int i = 0; i < arrays.length; i++) {
            resultLength += arrays[i].length;
        }
        byte[] result = new byte[resultLength];
        int currentPos = 0;
        for (int i = 0; i < arrays.length; i++) {
            System.arraycopy(arrays[i], 0, result, currentPos, arrays[i].length);
            currentPos += arrays[i].length;
        }

        return result;
    }
}
