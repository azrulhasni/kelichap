/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author azrul
 */

@Converter
public class StringSetOfStringConverter implements AttributeConverter<Set<String>, String> {
    private static final String SPLIT_CHAR = " ";
    
    @Override
    public String convertToDatabaseColumn(Set<String> stringList) {
        return StringUtils.join(stringList,SPLIT_CHAR);
    }

    @Override
    public Set<String> convertToEntityAttribute(String string) {
        Set<String> list = Arrays.stream(string.split(SPLIT_CHAR)).flatMap(s->{
            if (s.isBlank()){
                return Stream.empty();
            }
            return Stream.of(s);
        }).collect(Collectors.toSet());
        return string != null ?list: new HashSet<>();
    }
}
