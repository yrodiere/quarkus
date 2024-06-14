package io.quarkus.hibernate.orm.runtime.customized;

import org.hibernate.type.format.jackson.JacksonJsonFormatMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.arc.Arc;

public class QuarkusDefaultJacksonJsonFormatMapper extends QuarkusDelegatingFormatMapper {
    public QuarkusDefaultJacksonJsonFormatMapper() {
        super(new JacksonJsonFormatMapper(Arc.container().instance(ObjectMapper.class).get()));
    }
}
