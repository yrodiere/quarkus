package io.quarkus.hibernate.orm.runtime.customized;

import org.hibernate.boot.jaxb.spi.XmlMappingOptions;

/**
 * The XML mapping options used in Quarkus when XML mapping is enabled.
 */
public class QuarkusXmlMappingOptions implements XmlMappingOptions {

    @Override
    public boolean isPreferJaxb() {
        // We cannot (easily) serialize the DOM4J Documents that are normally used to represent orm.xml mappings.
        // Let's use JAXB instead: it represents mappings as POJOs that can easily be serialized by BytecodeRecorder.
        return true;
    }

}
