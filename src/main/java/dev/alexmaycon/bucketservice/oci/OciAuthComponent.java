package dev.alexmaycon.bucketservice.oci;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;

@Component
public class OciAuthComponent {

    @Autowired
    private Environment environment;

    public final ConfigFileReader.ConfigFile getConfigFile(String profile) throws IOException {
        String localProfile = (profile != null && Strings.isNotEmpty(profile) ? profile : "DEFAULT");

        File resourceUtils = null;

        // If running as DEV profile
        if (Arrays.asList(environment.getActiveProfiles()).contains("dev")) {
            final String ociConfigFile = environment.getProperty("oci");
            Assert.notNull(ociConfigFile, "Set '--oci=path/to/.oci' command line property when running as DEV profile.");
            resourceUtils = new File(ociConfigFile);
        } else {
            resourceUtils = ResourceUtils.getFile("file:./.oci");
        }

        if (!resourceUtils.exists()) {
            throw new FileNotFoundException("File .oci not found.");
        }

        String configurationFilePath = resourceUtils.getPath();

        return ConfigFileReader.parse(configurationFilePath, localProfile);
    }

    public final AuthenticationDetailsProvider getAuthenticationDetailsProvider(String profile) throws IOException {
        return new ConfigFileAuthenticationDetailsProvider(getConfigFile(profile));
    }

}
