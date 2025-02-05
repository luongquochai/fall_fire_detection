package com.camai.fall_fire_detection.config;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {
    
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("hazelcast-instance");
        config.getMapConfig("eventCache")
            .setTimeToLiveSeconds(300); // Cache entries expire after 5 minutes
        
        return Hazelcast.newHazelcastInstance(config);
    }
} 