/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author azrul
 */
@Converter
public class StringListOfLongConverter implements AttributeConverter<List<Long>, String> { //must be a list to maintain order
    private static final String SPLIT_CHAR = " ";
    
    @Override
    public String convertToDatabaseColumn(List<Long> longList) {
       return StringUtils.join(longList,SPLIT_CHAR);
    }

    @Override
    public List<Long> convertToEntityAttribute(String string) {
        List<Long> set = Arrays.stream(string.split(SPLIT_CHAR)).flatMap(s->{
            if (s.isBlank()){
                return Stream.empty();
            }
            return Stream.of(Long.parseLong(s));
        }).collect(Collectors.toList());
        return string != null ? set : new ArrayList<>();
    }
  
}
