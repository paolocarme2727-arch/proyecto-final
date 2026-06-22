package com.example.bank.credits.events;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;

/**
 * Encodes credit-service Kafka events with Avro contracts.
 */
public final class AvroEventCodec {

    private static final Schema CREDIT_DEBT_STATUS_SCHEMA = loadSchema("credit-debt-status.avsc");

    private AvroEventCodec() {
    }

    /**
     * Encodes credit debt status changes.
     *
     * @param event event data
     * @return Avro binary payload
     */
    public static byte[] encodeCreditDebtStatus(CreditDebtStatusEvent event) {
        GenericRecord record = new GenericData.Record(CREDIT_DEBT_STATUS_SCHEMA);
        record.put("customerId", event.customerId());
        record.put("hasOverdueDebt", event.hasOverdueDebt());
        return write(CREDIT_DEBT_STATUS_SCHEMA, record);
    }

    private static Schema loadSchema(String fileName) {
        String resource = "META-INF/avro/" + fileName;
        try (InputStream input = AvroEventCodec.class.getClassLoader().getResourceAsStream(resource)) {
            return new Schema.Parser().parse(Objects.requireNonNull(input, "Contrato Avro no encontrado: " + resource));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo cargar el contrato Avro " + resource, ex);
        }
    }

    private static byte[] write(Schema schema, GenericRecord record) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            GenericDatumWriter<GenericRecord> writer = new GenericDatumWriter<>(schema);
            BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(record, encoder);
            encoder.flush();
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo serializar el evento Avro", ex);
        }
    }
}
