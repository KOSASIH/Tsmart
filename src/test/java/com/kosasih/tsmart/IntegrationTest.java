package com.kosasih.tsmart;

import com.kosasih.tsmart.TsmartApp;
import com.kosasih.tsmart.config.AsyncSyncConfiguration;
import com.kosasih.tsmart.config.EmbeddedElasticsearch;
import com.kosasih.tsmart.config.EmbeddedKafka;
import com.kosasih.tsmart.config.EmbeddedSQL;
import com.kosasih.tsmart.config.TestSecurityConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { TsmartApp.class, AsyncSyncConfiguration.class, TestSecurityConfiguration.class })
@EmbeddedElasticsearch
@EmbeddedSQL
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@EmbeddedKafka
public @interface IntegrationTest {
}
