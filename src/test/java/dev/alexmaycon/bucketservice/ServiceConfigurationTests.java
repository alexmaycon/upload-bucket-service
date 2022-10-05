package dev.alexmaycon.bucketservice;

import dev.alexmaycon.bucketservice.config.ServiceConfiguration;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@EnableConfigurationProperties(value = ServiceConfiguration.class)
@TestPropertySource("classpath:application.properties")
class ServiceConfigurationTests {

	@Autowired
	private ServiceConfiguration serviceConfiguration;

	@Test
	void testePropriedade() {
		Assert.assertNotNull(serviceConfiguration);
		Assert.assertNotNull(serviceConfiguration.getService());
		Assert.assertEquals(serviceConfiguration.getService().getFolders().size(), 1);
		System.out.println(serviceConfiguration.toString());
	}

}
