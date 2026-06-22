package com.example.bank.accounts.events;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;

/**
 * Encodes and decodes account-service Kafka events with Avro contracts.
 */
public final class AvroEventCodec {

    private static final Schema DEBIT_CARD_REGISTERED_SCHEMA = loadSchema("debit-card-registered.avsc");
    private static final Schema CREDIT_DEBT_STATUS_SCHEMA = loadSchema("credit-debt-status.avsc");
    private static final Schema WALLET_PAYMENT_SCHEMA = loadSchema("wallet-payment.avsc");

    private AvroEventCodec() {
    }

    /**
     * Encodes a debit card registration event.
     *
     * @param event event data
     * @return Avro binary payload
     */
    public static byte[] encodeDebitCardRegistered(DebitCardEventPublisher.DebitCardRegisteredEvent event) {
        GenericRecord record = new GenericData.Record(DEBIT_CARD_REGISTERED_SCHEMA);
        record.put("debitCardId", event.debitCardId());
        record.put("accountId", event.accountId());
        record.put("active", event.active());
        record.put("createdAt", event.createdAt().toString());
        return write(DEBIT_CARD_REGISTERED_SCHEMA, record);
    }

    /**
     * Decodes a debit card registration event.
     *
     * @param payload Avro binary payload
     * @return event data
     */
    public static DebitCardEventPublisher.DebitCardRegisteredEvent decodeDebitCardRegistered(byte[] payload) {
        GenericRecord record = read(DEBIT_CARD_REGISTERED_SCHEMA, payload);
        return new DebitCardEventPublisher.DebitCardRegisteredEvent(
                string(record, "debitCardId"),
                string(record, "accountId"),
                (Boolean) record.get("active"),
                LocalDateTime.parse(string(record, "createdAt")));
    }

    /**
     * Decodes credit debt status changes.
     *
     * @param payload Avro binary payload
     * @return event data
     */
    public static CreditDebtStatusEvent decodeCreditDebtStatus(byte[] payload) {
        GenericRecord record = read(CREDIT_DEBT_STATUS_SCHEMA, payload);
        return new CreditDebtStatusEvent(
                string(record, "customerId"),
                (Boolean) record.get("hasOverdueDebt"));
    }

    /**
     * Decodes wallet payment events.
     *
     * @param payload Avro binary payload
     * @return event data
     */
    public static WalletPaymentEvent decodeWalletPayment(byte[] payload) {
        GenericRecord record = read(WALLET_PAYMENT_SCHEMA, payload);
        return new WalletPaymentEvent(
                string(record, "sourceWalletId"),
                string(record, "targetWalletId"),
                optionalString(record, "sourceDebitCardId"),
                optionalString(record, "targetDebitCardId"),
                new BigDecimal(string(record, "amount")),
                LocalDateTime.parse(string(record, "createdAt")));
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

    private static GenericRecord read(Schema schema, byte[] payload) {
        try {
            GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
            return reader.read(null, DecoderFactory.get().binaryDecoder(payload, null));
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo interpretar el evento Avro", ex);
        }
    }

    private static String string(GenericRecord record, String field) {
        return record.get(field).toString();
    }

    private static String optionalString(GenericRecord record, String field) {
        Object value = record.get(field);
        return value == null ? null : value.toString();
    }
}
