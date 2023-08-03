package com.kosasih.tsmart.config;

import java.util.Properties;
import org.hibernate.service.ServiceRegistry;
import org.infinispan.hibernate.cache.v60.InfinispanRegionFactory;
import org.infinispan.manager.EmbeddedCacheManager;
import org.springframework.stereotype.Component;

/**
 * Factory class for initializing Hibernate 2nd-level cache with Infinispan cache.
 *
 */
@Component
public class CacheFactoryConfiguration extends InfinispanRegionFactory {

    private static final long serialVersionUID = 1L;

    private EmbeddedCacheManager cacheManager;

    public CacheFactoryConfiguration(EmbeddedCacheManager cacheManager) {
        super();
        this.cacheManager = cacheManager;
    }

    @Override
    protected EmbeddedCacheManager createCacheManager(Properties properties, ServiceRegistry serviceRegistry) {
        return cacheManager;
    }
}
