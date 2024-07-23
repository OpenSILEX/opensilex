package org.opensilex.server.rest.serialization;

import org.apache.commons.lang3.StringUtils;
import org.opensilex.server.rest.serialization.uri.UriFormater;

import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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

        if (rawType.isAssignableFrom(URI.class)) {
            return (ParamConverter<T>) uriConverter;
        } else if (rawType.isAssignableFrom(LocalDate.class)) {
            return (ParamConverter<T>) localDateConverter;
        } else if (rawType.isAssignableFrom(OffsetDateTime.class)) {
            return (ParamConverter<T>) offsetDateTimeConverter;
        }

        return null;
    }

    public static final ParamConverter<LocalDate> localDateConverter = new ParamConverter<>() {
        @Override
        public LocalDate fromString(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(value);
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        public String toString(final LocalDate value) {
            return value.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }
    };

    public static final ParamConverter<OffsetDateTime> offsetDateTimeConverter = new ParamConverter<>() {
        @Override
        public OffsetDateTime fromString(final String value) {
            if (value == null || value.isEmpty()) {
                return null;
            }
            try {
                return OffsetDateTime.parse(value);
            } catch (Exception ex) {
                return null;
            }
        }

        @Override
        public String toString(final OffsetDateTime value) {
            return value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    };

    public static final ParamConverter<URI> uriConverter = new ParamConverter<>() {

        @Override
        public URI fromString(final String value) {
            if (StringUtils.isEmpty(value)){
                return null;
            }
            try {
                return new URI(URLDecoder.decode(value, StandardCharsets.UTF_8));
            } catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex);
            }
        }

        @Override
        public String toString(final URI value) {
            return value.toString();
        }
    };
}
