package io.simplesource.kafka.serialization.avro;

import io.simplesource.kafka.api.AggregateSerdes;
import io.simplesource.kafka.serialization.util.GenericMapper;
import io.simplesource.kafka.model.*;
import io.simplesource.kafka.serialization.util.GenericSerde;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;

import java.util.*;

import static io.simplesource.kafka.serialization.avro.AvroSpecificGenericMapper.specificDomainMapper;
import static io.simplesource.kafka.serialization.avro.AvroSerdes.*;


public final class AvroAggregateSerdes<K, C, E, A> implements AggregateSerdes<K, C, E, A> {

    private final Serde<K> ak;
    private final Serde<CommandRequest<K, C>> crq;
    private final Serde<UUID> crk;
    private final Serde<ValueWithSequence<E>> vws;
    private final Serde<AggregateUpdate<A>> au;
    private final Serde<AggregateUpdateResult<A>> aur;
    private final Serde<CommandResponse> crp;

    public static <A extends GenericRecord, E extends GenericRecord, C extends GenericRecord, K extends GenericRecord> AvroAggregateSerdes<A, E, C, K> of(
            final String schemaRegistryUrl,
            final Schema aggregateSchema
    ) {
        return of(
                schemaRegistryUrl,
                false,
                aggregateSchema);
    }

    public static <A extends GenericRecord, E extends GenericRecord, C extends GenericRecord, K extends GenericRecord> AvroAggregateSerdes<A, E, C, K> of(
            final String schemaRegistryUrl,
            final boolean useMockSchemaRegistry,
            final Schema aggregateSchema
    ) {
        return new AvroAggregateSerdes<>(
                specificDomainMapper(),
                specificDomainMapper(),
                specificDomainMapper(),
                specificDomainMapper(),
                schemaRegistryUrl,
                useMockSchemaRegistry,
                aggregateSchema);
    }

    public AvroAggregateSerdes(
            final GenericMapper<A, GenericRecord> aggregateMapper,
            final GenericMapper<E, GenericRecord> eventMapper,
            final GenericMapper<C, GenericRecord> commandMapper,
            final GenericMapper<K, GenericRecord> keyMapper,
            final String schemaRegistryUrl,
            final boolean useMockSchemaRegistry,
            final Schema aggregateSchema) {

        Serde<GenericRecord> keySerde = AvroGenericUtils.genericAvroSerde(schemaRegistryUrl, useMockSchemaRegistry, true);
        Serde<GenericRecord> valueSerde = AvroGenericUtils.genericAvroSerde(schemaRegistryUrl, useMockSchemaRegistry, false);

        ak = GenericSerde.of(keySerde, keyMapper::toGeneric, keyMapper::fromGeneric);
        crq = GenericSerde.of(valueSerde,
                v -> CommandRequestAvroHelper.toGenericRecord(v.map2(keyMapper::toGeneric, commandMapper::toGeneric)),
                s -> CommandRequestAvroHelper.fromGenericRecord(s).map2(x -> keyMapper.fromGeneric(x), x -> commandMapper.fromGeneric(x)));
        crk = GenericSerde.of(valueSerde,
                CommandResponseKeyAvroHelper::toGenericRecord,
                CommandResponseKeyAvroHelper::fromGenericRecord);
        vws = GenericSerde.of(valueSerde,
                v -> AvroGenericUtils.ValueWithSequenceAvroHelper.toGenericRecord(v.map(eventMapper::toGeneric)),
                s -> AvroGenericUtils.ValueWithSequenceAvroHelper.fromGenericRecord(s).map(eventMapper::fromGeneric));
        au = GenericSerde.of(valueSerde,
                v -> AggregateUpdateAvroHelper.toGenericRecord(v.map(aggregateMapper::toGeneric), aggregateSchema),
                s -> AggregateUpdateAvroHelper.fromGenericRecord(s)
                        .map(aggregateMapper::fromGeneric));
        aur = GenericSerde.of(valueSerde,
                v -> AggregateUpdateResultAvroHelper.toAggregateUpdateResult(v.map(aggregateMapper::toGeneric), aggregateSchema),
                s -> AggregateUpdateResultAvroHelper.fromAggregateUpdateResult(s).map(aggregateMapper::fromGeneric));

        crp = GenericSerde.of(valueSerde,
                v -> CommandResponseAvroHelper.toCommandResponse(v, aggregateSchema),
                CommandResponseAvroHelper::fromCommandResponse);
    }

    @Override
    public Serde<K> aggregateKey() {
        return ak;
    }

    @Override
    public Serde<CommandRequest<K, C>> commandRequest() {
        return crq;
    }

    @Override
    public Serde<UUID> commandResponseKey() {
        return crk;
    }

    @Override
    public Serde<ValueWithSequence<E>> valueWithSequence() {
        return vws;
    }

    @Override
    public Serde<AggregateUpdate<A>> aggregateUpdate() {
        return au;
    }

    @Override
    public Serde<AggregateUpdateResult<A>> updateResult() {
        return aur;
    }

    @Override
    public Serde<CommandResponse> commandResponse() {
        return crp;
    }

}
