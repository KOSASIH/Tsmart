package com.kosasih.tsmart.config;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.hibernate.cfg.AvailableSettings;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.jboss.marshalling.core.JBossUserMarshaller;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.spring.starter.embedded.InfinispanCacheConfigurer;
import org.infinispan.spring.starter.embedded.InfinispanGlobalConfigurer;
import org.infinispan.transaction.TransactionMode;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.protocols.*;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.protocols.pbcast.NAKACK2;
import org.jgroups.protocols.pbcast.STABLE;
import org.jgroups.stack.IpAddress;
import org.jgroups.stack.ProtocolStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.*;
import tech.jhipster.config.JHipsterProperties;
import tech.jhipster.config.cache.PrefixedKeyGenerator;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private GitProperties gitProperties;
    private BuildProperties buildProperties;

    private final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    private DiscoveryClient discoveryClient;

    private Registration registration;

    @Autowired(required = false)
    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    @Autowired(required = false)
    public void setDiscoveryClient(DiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    /**
     * Inject a {@link org.infinispan.configuration.global.GlobalConfiguration GlobalConfiguration} for Infinispan cache.
     * <p>
     * If a service discovery solution is enabled (JHipster Registry or Consul),
     * then the host list will be populated from the service discovery.
     *
     * <p>
     * If the service discovery is not enabled, host discovery will be based on
     * the default transport settings defined in the 'config-file' packaged within
     * the Jar. The 'config-file' can be overridden using the application property
     * <i>jhipster.cache.infinispan.config-file</i>
     *
     * <p>
     * If no service discovery is defined, you have the choice of 'config-file'
     * based on the underlying platform for hosts discovery. Infinispan
     * supports discovery natively for most of the platforms like Kubernetes/OpenShift,
     * AWS, Azure and Google.
     *
     * @param jHipsterProperties the jhipster properties to configure from.
     * @return the infinispan global configurer.
     */
    @Bean
    public InfinispanGlobalConfigurer globalConfiguration(JHipsterProperties jHipsterProperties) {
        log.info("Defining Infinispan Global Configuration");
        if (this.registration == null) { // if registry is not defined, use native discovery
            log.warn("No discovery service is set up, Infinispan will use default discovery for cluster formation");
            return () ->
                GlobalConfigurationBuilder
                    .defaultClusteredBuilder()
                    .transport()
                    .defaultTransport()
                    .addProperty("configurationFile", jHipsterProperties.getCache().getInfinispan().getConfigFile())
                    .clusterName("infinispan-Tsmart-cluster")
                    .jmx()
                    .enabled(jHipsterProperties.getCache().getInfinispan().isStatsEnabled())
                    .serialization()
                    .marshaller(new JBossUserMarshaller())
                    .build();
        } else if (discoveryClient.getInstances(registration.getServiceId()).size() == 0) {
            return () ->
                GlobalConfigurationBuilder
                    .defaultClusteredBuilder()
                    .transport()
                    .transport(new JGroupsTransport())
                    .clusterName("infinispan-Tsmart-cluster")
                    .jmx()
                    .enabled(jHipsterProperties.getCache().getInfinispan().isStatsEnabled())
                    .serialization()
                    .marshaller(new JBossUserMarshaller())
                    .build();
        } else {
            return () ->
                GlobalConfigurationBuilder
                    .defaultClusteredBuilder()
                    .transport()
                    .transport(new JGroupsTransport(getTransportChannel()))
                    .clusterName("infinispan-Tsmart-cluster")
                    .jmx()
                    .enabled(jHipsterProperties.getCache().getInfinispan().isStatsEnabled())
                    .serialization()
                    .marshaller(new JBossUserMarshaller())
                    .build();
        }
    }

    /**
     * Initialize cache configuration for Hibernate L2 cache and Spring Cache.
     * <p>
     * There are three different modes: local, distributed and replicated, and L2 cache options are pre-configured.
     *
     * <p>
     * It supports both jCache and Spring cache abstractions.
     * <p>
     * Usage:
     *  <ol>
     *      <li>
     *          jCache:
     *          <pre class="code">@CacheResult(cacheName = "dist-app-data") </pre>
     *              - for creating a distributed cache. In a similar way other cache names and options can be used
     *      </li>
     *      <li>
     *          Spring Cache:
     *          <pre class="code">@Cacheable(value = "repl-app-data") </pre>
     *              - for creating a replicated cache. In a similar way other cache names and options can be used
     *      </li>
     *      <li>
     *          Cache manager can also be injected through DI/CDI and data can be manipulated using Infinispan APIs,
     *          <pre class="code">
     *          &#064;Autowired (or) &#064;Inject
     *          private EmbeddedCacheManager cacheManager;
     *
     *          void cacheSample() {
     *              cacheManager.getCache("dist-app-data").put("hi", "there");
     *          }
     *          </pre>
     *      </li>
     *  </ol>
     *
     * @param jHipsterProperties the jhipster properties to configure from.
     * @return the infinispan cache configurer.
     */
    @Bean
    public InfinispanCacheConfigurer cacheConfigurer(JHipsterProperties jHipsterProperties) {
        log.info("Defining {} configuration", "app-data for local, replicated and distributed modes");
        JHipsterProperties.Cache.Infinispan cacheInfo = jHipsterProperties.getCache().getInfinispan();

        return manager -> {
            // initialize application cache
            manager.defineConfiguration(
                "local-app-data",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.LOCAL)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .memory()
                    .maxCount(cacheInfo.getLocal().getMaxEntries())
                    .expiration()
                    .lifespan(cacheInfo.getLocal().getTimeToLiveSeconds(), TimeUnit.SECONDS)
                    .build()
            );
            manager.defineConfiguration(
                "dist-app-data",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.DIST_SYNC)
                    .hash()
                    .numOwners(cacheInfo.getDistributed().getInstanceCount())
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .memory()
                    .maxCount(cacheInfo.getDistributed().getMaxEntries())
                    .expiration()
                    .lifespan(cacheInfo.getDistributed().getTimeToLiveSeconds(), TimeUnit.SECONDS)
                    .build()
            );
            manager.defineConfiguration(
                "repl-app-data",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.REPL_SYNC)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .memory()
                    .maxCount(cacheInfo.getReplicated().getMaxEntries())
                    .expiration()
                    .lifespan(cacheInfo.getReplicated().getTimeToLiveSeconds(), TimeUnit.SECONDS)
                    .build()
            );

            // initialize Hibernate L2 cache configuration templates
            manager.defineConfiguration(
                "entity",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.INVALIDATION_SYNC)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .locking()
                    .concurrencyLevel(1000)
                    .lockAcquisitionTimeout(15000)
                    .template(true)
                    .build()
            );
            manager.defineConfiguration(
                "replicated-entity",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.REPL_SYNC)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .locking()
                    .concurrencyLevel(1000)
                    .lockAcquisitionTimeout(15000)
                    .template(true)
                    .build()
            );
            manager.defineConfiguration(
                "local-query",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.LOCAL)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .locking()
                    .concurrencyLevel(1000)
                    .lockAcquisitionTimeout(15000)
                    .template(true)
                    .build()
            );
            manager.defineConfiguration(
                "replicated-query",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.REPL_ASYNC)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .locking()
                    .concurrencyLevel(1000)
                    .lockAcquisitionTimeout(15000)
                    .template(true)
                    .build()
            );
            manager.defineConfiguration(
                "timestamps",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.REPL_ASYNC)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .locking()
                    .concurrencyLevel(1000)
                    .lockAcquisitionTimeout(15000)
                    .template(true)
                    .build()
            );
            manager.defineConfiguration(
                "pending-puts",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.LOCAL)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .simpleCache(true)
                    .transaction()
                    .transactionMode(TransactionMode.NON_TRANSACTIONAL)
                    .expiration()
                    .maxIdle(60000)
                    .template(true)
                    .build()
            );

            Stream
                .of(
                    com.kosasih.tsmart.repository.UserRepository.USERS_BY_LOGIN_CACHE,
                    com.kosasih.tsmart.repository.UserRepository.USERS_BY_EMAIL_CACHE
                )
                .forEach(cacheName ->
                    manager.defineConfiguration(
                        cacheName,
                        new ConfigurationBuilder()
                            .clustering()
                            .cacheMode(CacheMode.INVALIDATION_SYNC)
                            .statistics()
                            .enabled(cacheInfo.isStatsEnabled())
                            .locking()
                            .concurrencyLevel(1000)
                            .lockAcquisitionTimeout(15000)
                            .build()
                    )
                );
            manager.defineConfiguration(
                "oAuth2Authentication",
                new ConfigurationBuilder()
                    .clustering()
                    .cacheMode(CacheMode.LOCAL)
                    .statistics()
                    .enabled(cacheInfo.isStatsEnabled())
                    .memory()
                    .maxCount(cacheInfo.getLocal().getMaxEntries())
                    .expiration()
                    .lifespan(cacheInfo.getLocal().getTimeToLiveSeconds(), TimeUnit.SECONDS)
                    .build()
            );
        };
    }

    /**
     * Hibernate properties customizer.
     * This component ensures that Hibernate L2 cache region factory uses, as cache manager, the
     * same instance already configured in Spring Context with {@link CacheConfiguration}
     *
     * @param cacheFactoryConfiguration custom infinispan region factory
     * @return hibernate properties with custom cache region factory
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(CacheFactoryConfiguration cacheFactoryConfiguration) {
        return hibernateProperties -> hibernateProperties.put(AvailableSettings.CACHE_REGION_FACTORY, cacheFactoryConfiguration);
    }

    /**
     * TCP channel with the host details populated from the service discovery
     * (JHipster Registry or Consul).
     * <p>
     * MPING multicast is replaced with TCPPING with the host details discovered
     * from registry and sends only unicast messages to the host list.
     */
    private JChannel getTransportChannel() {
        JChannel channel;
        List<InetSocketAddress> initialHosts = new ArrayList<>();
        try {
            for (ServiceInstance instance : discoveryClient.getInstances(registration.getServiceId())) {
                log.debug("Adding Infinispan cluster member {}:{}", instance.getHost(), 7800);
                initialHosts.add(new InetSocketAddress(instance.getHost(), 7800));
            }
            TCP tcp = new TCP();
            tcp.setBindAddress(InetAddress.getLocalHost());
            tcp.setBindPort(7800);

            TCPPING tcpping = new TCPPING();
            initialHosts.add(new InetSocketAddress(InetAddress.getLocalHost(), 7800));
            tcpping.setInitialHosts(initialHosts);
            tcpping.setPortRange(10);

            NAKACK2 nakack = new NAKACK2();
            nakack.setDiscardDeliveredMsgs(false);

            MERGE3 merge = new MERGE3();
            merge.setMinInterval(10000);
            merge.setMaxInterval(30000);

            FD_ALL fd = new FD_ALL();
            fd.setTimeout(60000);
            fd.setInterval(15000);
            fd.setTimeoutCheckInterval(5000);

            ProtocolStack stack = new ProtocolStack();
            // Order shouldn't be changed
            stack
                .addProtocol(tcp)
                .addProtocol(tcpping)
                .addProtocol(merge)
                .addProtocol(new FD_SOCK())
                .addProtocol(fd)
                .addProtocol(new VERIFY_SUSPECT())
                .addProtocol(nakack)
                .addProtocol(new UNICAST3())
                .addProtocol(new STABLE())
                .addProtocol(new GMS())
                .addProtocol(new MFC())
                .addProtocol(new FRAG2());
            channel = new JChannel(stack);
            stack.init();
        } catch (Exception e) {
            throw new BeanInitializationException("Cache (Infinispan protocol stack) configuration failed", e);
        }
        return channel;
    }

    @Autowired(required = false)
    public void setGitProperties(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Autowired(required = false)
    public void setBuildProperties(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Bean
    public KeyGenerator keyGenerator() {
        return new PrefixedKeyGenerator(this.gitProperties, this.buildProperties);
    }
}
