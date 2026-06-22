package com.example.bank.accounts.util;

import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class CommonUtils {

    public static List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values.stream().filter(value -> !value.isBlank()).toList();
    }

}
