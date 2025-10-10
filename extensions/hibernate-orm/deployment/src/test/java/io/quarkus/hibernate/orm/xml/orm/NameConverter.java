package io.quarkus.hibernate.orm.xml.orm;

import jakarta.persistence.AttributeConverter;
import org.assertj.core.api.SoftAssertions;

public class NameConverter implements AttributeConverter<String, String> {
    private static String lastWritten = null;
    private static String lastRead = null;

    public static void reset() {
        lastWritten = null;
        lastRead = null;
    }

    public static void checkUsed() {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(lastWritten).as("lastWritten").isNotNull();
            softAssertions.assertThat(lastRead).as("lastRead").isNotNull();
        });
    }

    public static void checkNotUsed() {
        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(lastWritten).as("lastWritten").isNull();
            softAssertions.assertThat(lastRead).as("lastRead").isNull();
        });
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        lastWritten = attribute;
        return attribute;
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        lastRead = dbData;
        return dbData;
    }
}
