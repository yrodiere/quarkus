package io.quarkus.hibernate.orm.runtime.customized;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.format.FormatMapper;

class QuarkusDelegatingFormatMapper implements FormatMapper {
    private final FormatMapper delegate;

    protected QuarkusDelegatingFormatMapper(FormatMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> T fromString(CharSequence charSequence, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return delegate.fromString(charSequence, javaType, wrapperOptions);
    }

    @Override
    public <T> String toString(T value, JavaType<T> javaType, WrapperOptions wrapperOptions) {
        return delegate.toString(value, javaType, wrapperOptions);
    }
}
