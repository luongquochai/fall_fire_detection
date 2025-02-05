package com.camai.fall_fire_detection.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption;

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
                .withSimpleReplication(3); // Replication factor

        return Collections.singletonList(specification);
    }

    @Override
    protected String getContactPoints() {
        return "localhost"; // Cấu hình contact points của ScyllaDB cluster
    }

    @Override
    protected int getPort() {
        return 9042; // Port mặc định của ScyllaDB
    }
} 