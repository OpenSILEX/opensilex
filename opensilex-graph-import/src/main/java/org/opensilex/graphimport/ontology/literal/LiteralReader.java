//******************************************************************************
// OpenSILEX - Licence AGPL V3.0 - https://www.gnu.org/licenses/agpl-3.0.en.html
// Copyright © INRAE 2026
//******************************************************************************
package org.opensilex.graphimport.ontology.literal;

import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.TypeMapper;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.jena.vocabulary.XSD;
import org.opensilex.aiimport.workbook.ExcelValueParser;
import org.opensilex.sparql.deserializer.SPARQLDeserializerNotFoundException;
import org.opensilex.sparql.deserializer.SPARQLDeserializers;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Reads a cell as a literal of a given datatype, the way a person fills a spreadsheet: a decimal
 * comma, yes and no in French or English, an Excel date serial, a date written with slashes.
 * <p>
 * What could mean two things is refused, never guessed: {@code 1.234,5} (which separator is the
 * decimal one?), {@code 03/04/2026} while the column has not settled the order of day and month, a
 * time without a zone. Every value accepted is then checked by the platform's own deserializer, so
 * that the preview accepts what a later write will.
 *
 * @author Arnaud Charleroy
 */
public class LiteralReader {

    /** Digits grouped by spaces, {@code 1 234 567}, with an optional decimal part. */
    private static final Pattern GROUPED = Pattern.compile("^[+-]?\\d{1,3}(?: \\d{3})+(?:[.,]\\d+)?$");
    private static final Pattern NUMBER = Pattern.compile("^[+-]?(?:\\d+(?:[.,]\\d+)?|[.,]\\d+)(?:[eE][+-]?\\d+)?$");

    private static final Set<String> TRUE_WORDS = Set.of("true", "vrai", "oui", "yes", "1");
    private static final Set<String> FALSE_WORDS = Set.of("false", "faux", "non", "no", "0");

    private static final Set<String> INTEGER_TYPES = Set.of(XSD.integer.getURI(), XSD.xint.getURI(),
            XSD.xlong.getURI(), XSD.xshort.getURI(), XSD.xbyte.getURI(), XSD.nonNegativeInteger.getURI(),
            XSD.positiveInteger.getURI(), XSD.negativeInteger.getURI(), XSD.nonPositiveInteger.getURI(),
            XSD.unsignedInt.getURI(), XSD.unsignedLong.getURI(), XSD.unsignedShort.getURI(),
            XSD.unsignedByte.getURI());
    private static final Set<String> DECIMAL_TYPES = Set.of(XSD.decimal.getURI(), XSD.xdouble.getURI(),
            XSD.xfloat.getURI());
    private static final Set<String> TEXT_TYPES = Set.of(XSD.xstring.getURI(), XSD.normalizedString.getURI(),
            XSD.token.getURI(), RDF.langString.getURI(), RDFS.Literal.getURI());

    private final DateReader dates;
    private final Map<String, Function<String, ReadResult<String>>> readersByDatatype = new HashMap<>();

    public LiteralReader(ReadingContext context) {
        this.dates = new DateReader(context);
        INTEGER_TYPES.forEach(type -> readersByDatatype.put(type, this::readInteger));
        DECIMAL_TYPES.forEach(type -> readersByDatatype.put(type, this::readDecimal));
        TEXT_TYPES.forEach(type -> readersByDatatype.put(type, ReadResult::of));
        readersByDatatype.put(XSD.xboolean.getURI(), this::readBoolean);
        readersByDatatype.put(XSD.date.getURI(), this::readDate);
        readersByDatatype.put(XSD.dateTime.getURI(), this::readDateTime);
        readersByDatatype.put(XSD.anyURI.getURI(), this::readUri);
    }

    /**
     * @param raw      the cell as the workbook gave it
     * @param datatype the datatype of the property, as a full URI; a datatype nobody reads here is
     *                 kept as text, then checked by Jena when Jena knows it
     */
    public LiteralReading read(String raw, URI datatype) {
        String text = ExcelValueParser.clean(raw);
        if (ExcelValueParser.isMissing(text)) {
            return LiteralReading.empty(datatype);
        }
        ReadResult<String> result = readersByDatatype.getOrDefault(datatype.toString(), ReadResult::of).apply(text);
        if (result.isRefused()) {
            return LiteralReading.refused(datatype, result.problem());
        }
        if (!validForPlatform(result.value(), datatype)) {
            return LiteralReading.refused(datatype, Problems.invalid(text, datatype));
        }
        return LiteralReading.value(result.value(), datatype);
    }

    private ReadResult<String> readInteger(String text) {
        ReadResult<String> number = readDecimal(text);
        if (number.isRefused()) {
            return number;
        }
        BigDecimal value = new BigDecimal(number.value());
        if (value.stripTrailingZeros().scale() > 0) {
            return ReadResult.refused(Problems.notAnInteger(text));
        }
        return ReadResult.of(value.toBigIntegerExact().toString());
    }

    private ReadResult<String> readDecimal(String text) {
        String candidate = GROUPED.matcher(text).matches() ? text.replace(" ", "") : text;
        if (candidate.indexOf(',') >= 0 && candidate.indexOf('.') >= 0) {
            return ReadResult.refused(Problems.twoSeparators(text));
        }
        if (!NUMBER.matcher(candidate).matches()) {
            return ReadResult.refused(Problems.notANumber(text));
        }
        // Without trailing zeros, so that 1,50 and 1.5 are one value wherever they are compared.
        return ReadResult.of(new BigDecimal(candidate.replace(',', '.')).stripTrailingZeros().toPlainString());
    }

    private ReadResult<String> readBoolean(String text) {
        String word = text.toLowerCase(Locale.ROOT);
        if (TRUE_WORDS.contains(word)) {
            return ReadResult.of("true");
        }
        if (FALSE_WORDS.contains(word)) {
            return ReadResult.of("false");
        }
        return ReadResult.refused(Problems.notABoolean(text));
    }

    private ReadResult<String> readDate(String text) {
        ReadResult<LocalDate> date = dates.date(text);
        return date.isRefused()
                ? ReadResult.refused(date.problem())
                : ReadResult.of(date.value().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    private ReadResult<String> readDateTime(String text) {
        ReadResult<OffsetDateTime> dateTime = dates.dateTime(text);
        return dateTime.isRefused()
                ? ReadResult.refused(dateTime.problem())
                : ReadResult.of(dateTime.value().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    }

    private ReadResult<String> readUri(String text) {
        try {
            URI uri = new URI(text);
            if (uri.isAbsolute()) {
                return ReadResult.of(uri.toString());
            }
        } catch (URISyntaxException e) {
            // reported below
        }
        return ReadResult.refused(Problems.notAUri(text));
    }

    /**
     * Valid for the exact datatype, as Jena knows it, and for the platform's deserializer, which reads
     * several datatypes with one Java type: {@code xsd:positiveInteger} with an {@code Integer}, say,
     * which would let 0 through.
     */
    private static boolean validForPlatform(String lexical, URI datatype) {
        RDFDatatype jenaType = TypeMapper.getInstance().getTypeByName(datatype.toString());
        if (jenaType != null && !jenaType.isValid(lexical)) {
            return false;
        }
        if (!SPARQLDeserializers.existsForDatatype(datatype)) {
            return true;
        }
        try {
            return SPARQLDeserializers.getForDatatype(datatype).validate(lexical);
        } catch (SPARQLDeserializerNotFoundException e) {
            return false;
        }
    }
}
