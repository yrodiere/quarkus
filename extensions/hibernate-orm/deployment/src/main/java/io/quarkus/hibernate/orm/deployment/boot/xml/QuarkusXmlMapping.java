package io.quarkus.hibernate.orm.deployment.boot.xml;

import org.hibernate.boot.jaxb.Origin;
import org.hibernate.boot.jaxb.SourceType;
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping;
import org.hibernate.boot.jaxb.mapping.spi.JaxbEntityMappingsImpl;
import org.hibernate.boot.jaxb.spi.Binding;
import org.hibernate.boot.jaxb.spi.JaxbBindableMappingDescriptor;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * A holder class for a parsed XML mapping file (orm.xml or hbm.xml).
 * <p>
 * FIXME: This was mainly useful to serialize/deserialize through the BytecodeRecorder, but we don't need that anymore.
 */
public class QuarkusXmlMapping {
    // The following two properties are mutually exclusive: exactly one of them is non-null.
    private final JaxbEntityMappingsImpl ormXmlRoot;
    private final JaxbHbmHibernateMapping hbmXmlRoot;

    private final SourceType originType;
    private final String originName;

    public static QuarkusXmlMapping create(Binding<? extends JaxbBindableMappingDescriptor> binding) {
        JaxbBindableMappingDescriptor root = binding.getRoot();
        Origin origin = binding.getOrigin();
        if (root instanceof JaxbEntityMappingsImpl) {
            return new QuarkusXmlMapping((JaxbEntityMappingsImpl) root, null, origin.getType(), origin.getName());
        } else if (root instanceof JaxbHbmHibernateMapping) {
            return new QuarkusXmlMapping(null, (JaxbHbmHibernateMapping) root, origin.getType(), origin.getName());
        } else {
            throw new IllegalArgumentException("Unsupported mapping file root (unrecognized type): " + root);
        }
    }

    @RecordableConstructor
    public QuarkusXmlMapping(JaxbEntityMappingsImpl ormXmlRoot, JaxbHbmHibernateMapping hbmXmlRoot, SourceType originType,
            String originName) {
        this.ormXmlRoot = ormXmlRoot;
        this.hbmXmlRoot = hbmXmlRoot;
        this.originType = originType;
        this.originName = originName;
    }

    @Override
    public String toString() {
        return "RecordableXmlMapping{" +
                "originName='" + originName + '\'' +
                '}';
    }

    public JaxbEntityMappingsImpl getOrmXmlRoot() {
        return ormXmlRoot;
    }

    public JaxbHbmHibernateMapping getHbmXmlRoot() {
        return hbmXmlRoot;
    }

    public SourceType getOriginType() {
        return originType;
    }

    public String getOriginName() {
        return originName;
    }

    public Binding<?> toHibernateOrmBinding() {
        Origin origin = new Origin(originType, originName);
        if (ormXmlRoot != null) {
            return new Binding<>(ormXmlRoot, origin);
        } else {
            return new Binding<>(hbmXmlRoot, origin);
        }
    }
}
