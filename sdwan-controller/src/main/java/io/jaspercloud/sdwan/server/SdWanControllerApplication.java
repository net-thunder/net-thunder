package io.jaspercloud.sdwan.server;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.derby.tools.ij;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

/**
 * @author jasper
 * @create 2024/7/12
 */
@SpringBootApplication
public class SdWanControllerApplication {

    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption("derby-shell", "derby-shell", false, "derby shell");
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("derby-shell")) {
            String userDir = System.getProperty("user.dir");
            File file = new File(userDir, "derby");
            System.out.println(String.format("connect 'jdbc:derby:%s;create=true';", file.getAbsolutePath()));
            ij.main(new String[]{});
            return;
        }
        SpringApplication.run(SdWanControllerApplication.class, args);
    }
}
