package com.camai.fall_fire_detection.config;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.EvictionConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MaxSizePolicy;
    
@Configuration
public class HazelcastConfig {
    
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setInstanceName("hazelcast-instance-" + System.currentTimeMillis()); // Tạo tên instance động
        
        // Cấu hình network
        NetworkConfig network = config.getNetworkConfig();
        JoinConfig join = network.getJoin();
        join.getMulticastConfig().setEnabled(true);
        
        // Cấu hình cache
        MapConfig eventCacheConfig = new MapConfig("eventCache")
                .setTimeToLiveSeconds(300)
                .setMaxIdleSeconds(200)
                .setEvictionConfig(
                    new EvictionConfig()
                        .setEvictionPolicy(EvictionPolicy.LRU)
                        .setMaxSizePolicy(MaxSizePolicy.PER_NODE)
                        .setSize(10000)
                );
        
        config.addMapConfig(eventCacheConfig);
        
        return Hazelcast.newHazelcastInstance(config);
    }
} 