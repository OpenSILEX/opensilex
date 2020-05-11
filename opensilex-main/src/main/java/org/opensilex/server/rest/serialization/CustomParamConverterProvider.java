package org.opensilex.server.rest.serialization;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Singleton;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Provider;

@Provider
@Singleton
public class CustomParamConverterProvider implements ParamConverterProvider {

    @Override
    @SuppressWarnings("unchecked")
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {

        if (rawType.isAssignableFrom(LocalDate.class)) {
            return (ParamConverter<T>) localDateConverter;
        } else if (rawType.isAssignableFrom(LocalDateTime.class)) {
            return (ParamConverter<T>) localDateTimeConverter;
        }

        return null;
    }

    private static ParamConverter<LocalDate> localDateConverter = new ParamConverter<LocalDate>() {
        @Override
        public LocalDate fromString(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalDate.parse(value);
        }

        @Override
        public String toString(final LocalDate value) {
            return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    };

    private static ParamConverter<LocalDateTime> localDateTimeConverter = new ParamConverter<LocalDateTime>() {
        @Override
        public LocalDateTime fromString(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            return LocalDateTime.parse(value);
        }

        @Override
        public String toString(final LocalDateTime value) {
            return value.format(DateTimeFormatter.ISO_INSTANT);
        }
    };
}
