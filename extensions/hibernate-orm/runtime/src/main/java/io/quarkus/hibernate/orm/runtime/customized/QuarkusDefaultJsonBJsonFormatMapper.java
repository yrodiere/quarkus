package io.quarkus.hibernate.orm.runtime.customized;

import jakarta.json.bind.Jsonb;

import org.hibernate.type.format.jakartajson.JsonBJsonFormatMapper;

import io.quarkus.arc.Arc;

public class QuarkusDefaultJsonBJsonFormatMapper extends QuarkusDelegatingFormatMapper {
    protected QuarkusDefaultJsonBJsonFormatMapper() {
        super(new JsonBJsonFormatMapper(Arc.container().instance(Jsonb.class).get()));
    }
}
