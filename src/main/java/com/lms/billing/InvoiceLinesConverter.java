package com.lms.billing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.List;

/** List&lt;InvoiceLine&gt; ↔ JSON 문자열 (invoice.lines 저장용). */
@Converter
public class InvoiceLinesConverter implements AttributeConverter<List<InvoiceLine>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<InvoiceLine>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<InvoiceLine> attribute) {
        try {
            return MAPPER.writeValueAsString(attribute == null ? List.of() : attribute);
        } catch (Exception e) {
            throw new IllegalStateException("invoice lines 직렬화 실패", e);
        }
    }

    @Override
    public List<InvoiceLine> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readValue(dbData, TYPE);
        } catch (Exception e) {
            throw new IllegalStateException("invoice lines 역직렬화 실패", e);
        }
    }
}
