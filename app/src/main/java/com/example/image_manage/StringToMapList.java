package com.example.image_manage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class StringToMapConverter {
    public static Map<String, String> convertStringToMap(String input) {
        Map<String, String> map = new HashMap<>();

        // Remove leading and trailing '#' if any
        if (input.startsWith("#")) {
            input = input.substring(1);
        }
        if (input.endsWith("#")) {
            input = input.substring(0, input.length() - 1);
        }

        // Split the string by '#'
        String[] parts = input.split("#");

        // Ensure the number of parts is even
        if (parts.length % 2 != 0) {
            throw new IllegalArgumentException("Input string does not have an even number of elements.");
        }

        // Iterate through the parts and add to map
        for (int i = 0; i < parts.length; i += 2) {
            map.put(parts[i], parts[i + 1]);
        }

        return map;
    }
}
