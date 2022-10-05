package dev.alexmaycon.bucketservice.oci;

import com.oracle.bmc.ConfigFileReader;
import com.oracle.bmc.auth.AuthenticationDetailsProvider;
import com.oracle.bmc.auth.ConfigFileAuthenticationDetailsProvider;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Component
public class OciAuthComponent {

    public final ConfigFileReader.ConfigFile getConfigFile(String profile) throws IOException {
        String localProfile = (profile != null && Strings.isNotEmpty(profile) ? profile : "DEFAULT");

        File resourceUtils = new File(".oci");

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
