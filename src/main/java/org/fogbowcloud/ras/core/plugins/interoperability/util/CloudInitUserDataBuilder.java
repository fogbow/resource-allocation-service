package org.fogbowcloud.ras.core.plugins.interoperability.util;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import org.apache.commons.codec.binary.Base64;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.Set;

/**
 * Build a <a href="https://help.ubuntu.com/community/CloudInit">CloudInit</a> UserData file.
 * <p>
 * <p>Sample:
 * <p>
 * <pre>
 * <code>
 * // base64 encoded user data
 * String userData = CloudInitUserDataBuilder.start() //
 *                      .addShellScript(shellScript) //
 *                      .addCloudConfig(cloudConfig) //
 *                      .buildBase64UserData();
 *
 * RunInstancesRequest req = new RunInstancesRequest() //
 *                              .withInstanceType("t1.micro") //
 *                              .withImageId("ami-47cefa33") // amazon-linux in eu-west-1 region
 *                              .withMinCount(1).withMaxCount(1) //
 *                              .withSecurityGroupIds("default") //
 *                              .withKeyName("my-key") //
 *                              .withUserData(userData);
 *
 * RunInstancesResult runInstances = ec2.runInstances(runInstancesRequest);
 * </code>
 * </pre>
 * <p>
 * <p>Inspired by ubuntu-on-ec2 cloud-util <a href=
 * "http://bazaar.launchpad.net/~ubuntu-on-ec2/ubuntu-on-ec2/cloud-utils/view/head:/write-mime-multipart"
 * >write-mime-multipart</a> python script.
 *
 * @author <a href="mailto:cyrille@cyrilleleclerc.com">Cyrille Le Clerc</a>
 * @see com.amazonaws.services.ec2.model.RunInstancesRequest#withUserData(String)
 * @see com.amazonaws.services.ec2.AmazonEC2.runInstances(RunInstancesRequest)
 */
public class CloudInitUserDataBuilder {
    private int userDataCounter = 1;

    /**
     * File types supported by CloudInit
     */
    public enum FileType {
        /**
         * This content is "boothook" data. It is stored in a file under /var/lib/cloud and then
         * executed immediately. This is the earliest "hook" available. Note, that there is no
         * mechanism provided for running only once. The boothook must take care of this itself. It
         * is provided with the instance id in the environment variable "INSTANCE_ID". This could be
         * made use of to provide a 'once-per-instance'
         */
        CLOUD_BOOTHOOK("text/cloud-boothook", "cloudinit-cloud-boothook.txt"), //
        /**
         * This content is "cloud-config" data. See the examples for a commented example of
         * supported config formats.
         * <p>
         * <p>Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/cloud-config.txt"
         * >cloud-config.txt</a>
         */
        CLOUD_CONFIG("text/cloud-config", "cloudinit-cloud-config.txt"), //
        /**
         * This content is a "include" file. The file contains a list of urls, one per line. Each of
         * the URLs will be read, and their content will be passed through this same set of rules.
         * Ie, the content read from the URL can be gzipped, mime-multi-part, or plain text
         * <p>
         * <p>Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/include.txt"
         * >include.txt</a>
         */
        INCLUDE_URL("text/x-include-url", "cloudinit-x-include-url.txt"), //
        /**
         * This is a 'part-handler'. It will be written to a file in /var/lib/cloud/data based on
         * its filename. This must be python code that contains a list_types method and a
         * handle_type method. Once the section is read the 'list_types' method will be called. It
         * must return a list of mime-types that this part-handler handlers.
         * <p>
         * <p>Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/part-handler.txt"
         * >part-handler.txt</a>
         */
        PART_HANDLER("text/part-handler", "cloudinit-part-handler.txt"), //
        /**
         * Script will be executed at "rc.localidentity-like" level during first boot. rc.localidentity-like means
         * "very late in the boot sequence"
         * <p>
         * <p>Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/user-script.txt"
         * >user-script.txt</a>
         */
        SHELL_SCRIPT("text/x-shellscript", "cloudinit-userdata-script.txt"), //
        /**
         * Content is placed into a file in /etc/init, and will be consumed by upstart as any other
         * upstart job.
         * <p>
         * <p>Example: <a href=
         * "http://bazaar.launchpad.net/~cloud-init-dev/cloud-init/trunk/view/head:/doc/examples/upstart-rclocal.txt"
         * >upstart-rclocal.txt</a>
         */
        UPSTART_JOB("text/upstart-job", "cloudinit-upstart-job.txt");
        /**
         * Name of the file.
         */
        private final String fileName;
        /**
         * Mime Type of the file.
         */
        private final String mimeType;

        private FileType(String mimeType, String fileName) {
            this.mimeType = Preconditions.checkNotNull(mimeType);
            this.fileName = Preconditions.checkNotNull(fileName);
        }

        /**
         * @return name of the file
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * e.g. "cloud-config" for "text/cloud-config"
         */
        public String getMimeTextSubType() {
            return getMimeType().substring("text/".length());
        }

        /**
         * e.g. "text/cloud-config"
         */
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public String toString() {
            return name() + "[" + mimeType + "]";
        }
    }

    /**
     * Initiates a new instance of the builder with the "UTF-8" charset.
     */
    public static CloudInitUserDataBuilder start() {
        return new CloudInitUserDataBuilder(Charsets.UTF_8);
    }

    /**
     * Initiates a new instance of the builder.
     *
     * @param charset used to generate the mime message.
     */
    public static CloudInitUserDataBuilder start(String charset) {
        return new CloudInitUserDataBuilder(Charset.forName(charset));
    }

    /**
     * File types already added because cloud-init only supports one file of each type.
     */
    private final Set<FileType> alreadyAddedFileTypes = Sets.newHashSet();

    /**
     * Charset used to generate the mime message.
     */
    private final Charset charset;

    /**
     * Mime message under creation
     */
    private final MimeMessage userDataMimeMessage;

    /**
     * Mime message's content under creation
     */
    private final MimeMultipart userDataMultipart;

    private CloudInitUserDataBuilder(Charset charset) {
        super();
        this.userDataMimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        this.userDataMultipart = new MimeMultipart();
        try {
            this.userDataMimeMessage.setContent(this.userDataMultipart);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        this.charset = Preconditions.checkNotNull(charset, "'charset' can NOT be null");
    }

    /**
     * Add a boot-hook file.
     *
     * @param bootHook
     * @return the builder
     * @throws IllegalArgumentException a boot-hook file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#CLOUD_BOOTHOOK
     */
    public CloudInitUserDataBuilder addBootHook(Readable bootHook) {
        return addFile(FileType.CLOUD_BOOTHOOK, bootHook);
    }

    /**
     * Add a cloud-config file.
     *
     * @param cloudConfig
     * @return the builder
     * @throws IllegalArgumentException a cloud-config file was already added to this cloud-init
     *                                  mime message.
     * @see FileType#CLOUD_CONFIG
     */
    public CloudInitUserDataBuilder addCloudConfig(Readable cloudConfig) {
        return addFile(FileType.CLOUD_CONFIG, cloudConfig);
    }

    /**
     * Add a cloud-config file.
     *
     * @param cloudConfig
     * @return the builder
     * @throws IllegalArgumentException a cloud-config file was already added to this cloud-init
     *                                  mime message.
     * @see FileType#CLOUD_CONFIG
     */
    public CloudInitUserDataBuilder addCloudConfig(String cloudConfig) {
        return addCloudConfig(new StringReader(cloudConfig));
    }

    /**
     * Add given file <code>in</code> to the cloud-init mime message.
     *
     * @param fileType
     * @param in       file to add as readable
     * @return the builder
     * @throws IllegalArgumentException the given <code>fileType</code> was already added to this
     *                                  cloud-init mime message.
     */
    public CloudInitUserDataBuilder addFile(FileType fileType, Readable in)
            throws IllegalArgumentException {
        Preconditions.checkNotNull(fileType, "'fileType' can NOT be null");
        Preconditions.checkNotNull(in, "'in' can NOT be null");
        // Preconditions.checkArgument(!alreadyAddedFileTypes.contains(fileType),
        // "%s as already been added", fileType);
        this.alreadyAddedFileTypes.add(fileType);

        try {
            StringWriter sw = new StringWriter();
            CharStreams.copy(in, sw);
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setText(sw.toString(), this.charset.name(), fileType.getMimeTextSubType());
            mimeBodyPart.setFileName((this.userDataCounter++) + fileType.getFileName());
            this.userDataMultipart.addBodyPart(mimeBodyPart);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * Add a include-url file.
     *
     * @param includeUrl
     * @return the builder
     * @throws IllegalArgumentException a include-url file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#INCLUDE_URL
     */
    public CloudInitUserDataBuilder addIncludeUrl(Readable includeUrl) {
        return addFile(FileType.INCLUDE_URL, includeUrl);
    }

    /**
     * Add a include-url file.
     *
     * @param includeUrl
     * @return the builder
     * @throws IllegalArgumentException a include-url file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#INCLUDE_URL
     */
    public CloudInitUserDataBuilder addIncludeUrl(String includeUrl) {
        return addIncludeUrl(new StringReader(includeUrl));
    }

    /**
     * Add a part-handler file.
     *
     * @param partHandler
     * @return the builder
     * @throws IllegalArgumentException a part-handler file was already added to this cloud-init
     *                                  mime message.
     * @see FileType#PART_HANDLER
     */
    public CloudInitUserDataBuilder addPartHandler(Readable partHandler) {
        return addFile(FileType.PART_HANDLER, partHandler);
    }

    /**
     * Add a part-handler file.
     *
     * @param partHandler
     * @return the builder
     * @throws IllegalArgumentException a part-handler file was already added to this cloud-init
     *                                  mime message.
     * @see FileType#PART_HANDLER
     */
    public CloudInitUserDataBuilder addPartHandler(String partHandler) {
        return addPartHandler(new StringReader(partHandler));
    }

    /**
     * Add a shell-script file.
     *
     * @param shellScript
     * @return the builder
     * @throws IllegalArgumentException a shell-script file was already added to this cloud-init
     *                                  mime message.
     * @see FileType#SHELL_SCRIPT
     */
    public CloudInitUserDataBuilder addShellScript(Readable shellScript) {
        return addFile(FileType.SHELL_SCRIPT, shellScript);
    }

    /**
     * Add a shell-script file.
     *
     * @param shellScript
     * @return the builder
     * @throws IllegalArgumentException a shell-script file was already added to this cloud-init
     *                                  mime message.
     * @see FileType#SHELL_SCRIPT
     */
    public CloudInitUserDataBuilder addShellScript(String shellScript) {
        return addShellScript(new StringReader(shellScript));
    }

    /**
     * Add a upstart-job file.
     *
     * @param shellScript
     * @return the builder
     * @throws IllegalArgumentException a upstart-job file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#UPSTART_JOB
     */
    public CloudInitUserDataBuilder addUpstartJob(Readable in) {
        return addFile(FileType.UPSTART_JOB, in);
    }

    /**
     * Add a upstart-job file.
     *
     * @param shellScript
     * @return the builder
     * @throws IllegalArgumentException a upstart-job file was already added to this cloud-init mime
     *                                  message.
     * @see FileType#UPSTART_JOB
     */
    public CloudInitUserDataBuilder addUpstartJob(String upstartJob) {
        return addUpstartJob(new StringReader(upstartJob));
    }

    /**
     * Build the user-data mime message.
     *
     * @return the generate mime message
     */
    public String buildUserData() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            this.userDataMimeMessage.writeTo(baos);
            return new String(baos.toByteArray(), this.charset);

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Build a base64 encoded user-data mime message.
     *
     * @return the base64 encoded encoded mime message
     */
    public String buildBase64UserData() {
        return Base64.encodeBase64String(buildUserData().getBytes());
    }
}
