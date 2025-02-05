package com.camai.fall_fire_detection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.convert.CassandraCustomConversions;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;
import org.springframework.data.cassandra.core.convert.CassandraConverter;
import org.springframework.data.cassandra.core.convert.MappingCassandraConverter;
import org.springframework.data.cassandra.core.mapping.CassandraMappingContext;
import org.springframework.data.cassandra.core.mapping.SimpleUserTypeResolver;

import com.datastax.oss.driver.api.core.CqlIdentifier;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Configuration
public class ScyllaConfig extends AbstractCassandraConfiguration {

    @Override
    protected String getKeyspaceName() {
        return "fall_fire_detection";
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        CreateKeyspaceSpecification specification = CreateKeyspaceSpecification
                .createKeyspace(getKeyspaceName())
                .ifNotExists()
                .with(KeyspaceOption.DURABLE_WRITES, true)
                .withSimpleReplication(3);
        return Collections.singletonList(specification);
    }

    @Override
    protected String getContactPoints() {
        return "localhost";
    }

    @Override
    protected int getPort() {
        return 9042;
    }

    @Bean
    @Override
    public CassandraCustomConversions customConversions() {
        return new CassandraCustomConversions(Arrays.asList(
            new EventTypeConverters.StringToEventTypeConverter(),
            new EventTypeConverters.EventTypeToStringConverter(),
            new EventTypeConverters.StringToEventStatusConverter(),
            new EventTypeConverters.EventStatusToStringConverter()
        ));
    }

    @Override
    public CassandraConverter cassandraConverter() {
        MappingCassandraConverter converter = 
            (MappingCassandraConverter) super.cassandraConverter();
        converter.setCustomConversions(customConversions()); // Thêm dòng này
        return converter;
    }
}