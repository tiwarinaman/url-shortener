package com.naman.urlshortner.encoder;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Base64 {

    public static String encode(Long id) {
        return java.util.Base64.getUrlEncoder().encodeToString(String.valueOf(id).getBytes());
    }

    public static Long decode(String base64Id) {
        return Long.parseLong(new String(java.util.Base64.getUrlDecoder().decode(base64Id)));
    }

}
