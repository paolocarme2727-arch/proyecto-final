package com.example.bank.wallets.events;

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
 * Encodes and decodes wallet-service Kafka events with Avro contracts.
 */
public final class AvroEventCodec {

    private static final Schema DEBIT_CARD_REGISTERED_SCHEMA = loadSchema("debit-card-registered.avsc");
    private static final Schema WALLET_PAYMENT_SCHEMA = loadSchema("wallet-payment.avsc");
    private static final Schema WALLET_DEBIT_CARD_LINKED_SCHEMA = loadSchema("wallet-debit-card-linked.avsc");

    private AvroEventCodec() {
    }

    /**
     * Decodes debit card registration events.
     *
     * @param payload Avro binary payload
     * @return event data
     */
    public static DebitCardCatalog.DebitCardRegisteredEvent decodeDebitCardRegistered(byte[] payload) {
        GenericRecord record = read(DEBIT_CARD_REGISTERED_SCHEMA, payload);
        return new DebitCardCatalog.DebitCardRegisteredEvent(
                string(record, "debitCardId"),
                string(record, "accountId"),
                (Boolean) record.get("active"),
                LocalDateTime.parse(string(record, "createdAt")));
    }

    /**
     * Encodes wallet payment events.
     *
     * @param event event data
     * @return Avro binary payload
     */
    public static byte[] encodeWalletPayment(WalletEventPublisher.WalletPaymentEvent event) {
        GenericRecord record = new GenericData.Record(WALLET_PAYMENT_SCHEMA);
        record.put("sourceWalletId", event.sourceWalletId());
        record.put("targetWalletId", event.targetWalletId());
        record.put("sourceDebitCardId", event.sourceDebitCardId());
        record.put("targetDebitCardId", event.targetDebitCardId());
        record.put("amount", event.amount().toPlainString());
        record.put("createdAt", event.createdAt().toString());
        return write(WALLET_PAYMENT_SCHEMA, record);
    }

    /**
     * Encodes wallet debit card link events.
     *
     * @param event event data
     * @return Avro binary payload
     */
    public static byte[] encodeWalletDebitCardLinked(WalletEventPublisher.WalletDebitCardLinkedEvent event) {
        GenericRecord record = new GenericData.Record(WALLET_DEBIT_CARD_LINKED_SCHEMA);
        record.put("walletId", event.walletId());
        record.put("debitCardId", event.debitCardId());
        record.put("createdAt", event.createdAt().toString());
        return write(WALLET_DEBIT_CARD_LINKED_SCHEMA, record);
    }

    /**
     * Decodes wallet payment events.
     *
     * @param payload Avro binary payload
     * @return event data
     */
    public static WalletEventPublisher.WalletPaymentEvent decodeWalletPayment(byte[] payload) {
        GenericRecord record = read(WALLET_PAYMENT_SCHEMA, payload);
        return new WalletEventPublisher.WalletPaymentEvent(
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
