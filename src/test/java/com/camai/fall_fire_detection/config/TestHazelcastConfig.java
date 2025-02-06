package com.camai.fall_fire_detection.config;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestHazelcastConfig {

    @Bean
    @Primary
    public HazelcastInstance testHazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("test-hazelcast-" + System.currentTimeMillis());
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        return Hazelcast.newHazelcastInstance(config);
    }
} 