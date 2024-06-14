package io.quarkus.hibernate.orm.runtime.customized;

import org.hibernate.type.format.jaxb.JaxbXmlFormatMapper;

public enum FormatMapperKind {
    JACKSON {
        @Override
        public Class<?> getFormatMapperClass() {
            // NOTE: we are not creating a Jackson based XML mapper since that one
            // requires an additional lib (jackson-dataformat-xml-2.15.2) being available
            // as well as an XmlMapper instance instead of an ObjectMapper...
            return QuarkusDefaultJacksonJsonFormatMapper.class;
        }
    },
    JSONB {
        @Override
        public Class<?> getFormatMapperClass() {
            return QuarkusDefaultJsonBJsonFormatMapper.class;
        }
    },
    JAXB {
        @Override
        public Class<?> getFormatMapperClass() {
            return JaxbXmlFormatMapper.class;
        }
    };

    public abstract Class<?> getFormatMapperClass();
}
