package com.wordiga.global.config;

import com.wordiga.common.dto.AgeGroup;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class AgeGroupConverter implements Converter<String, AgeGroup> {
    @Override
    public AgeGroup convert(String source) {
        return AgeGroup.from(source);
    }
}
