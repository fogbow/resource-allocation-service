# Resource Allocation Service

This service provides a common REST interface for manage resources in an environment with different cloud providers (e.g, Amazon Web Service, OpenStack, Microsoft Azure, etc). It also offers a way to interconnect many Fogbow instances in a federation.

## How to use

In this section the installation explanation will be facing a possible contributor. If you are interested in deployment, please take a look at [fogbow-deploy](https://github.com/fogbow/fogbow-deploy).

### Dependencies

- Java 8
- Maven
- [Common module](https://github.com/fogbow/common/), which is a dependency for most fogbow service.

### Installing

First of all, create a directory to organize all fogbow modules/services then clone the required repositories.

```bash
mkdir fogbow && cd fogbow

git clone https://github.com/fogbow/common.git
cd common  
git checkout develop && mvn install -DskipTests

git clone https://github.com/fogbow/resource-allocation-service.git
cd resource-allocaton-service
git checkout develop && mvn install -DskipTests
```

### Configuration

This service requires some initial configuration. Most of them will have a template for help you to get started.

First of all, you need to create a directory named *private* at `src/main/resources`, it will be holding your private settings (managed clouds, username and password for the clouds, etc.).

#### prosody.lua file

Insert here information about XMPP configuration.

#### private/clouds/

For each cloud that you will use, a directory should be created at `src/main/resources/private/clouds`. For each supported cloud there is a template under `src/main/resources/templates/clouds`.

You can name the directory as you like, but remember that this name will be the identifier for the cloud.

You don't need to use all the clouds, you can choose which one to use. The clouds used will depend on the ras.conf "cloud_names" field.

#### private/ras.conf

Check out `src/main/resources/templates/ras.conf` for a file template. Here you need to configurate some required fields. Let's take a look at each of them.

- **public_key_file_path:** the path to the public key
- **private_key_file_path:** the path to the private key
- **authorization_plugin_class:** ?
- **cloud_names:** the name of the clouds under private/clouds, separate by comma (,)
- **provider_id:** the XMPP provider id configurated at prosody.lua (Still need to confirmate this info)
- **as_port:** the port that the Authentication Service is running
- **as_url:** the url that the Authentication Service is running

speech_balloon Insert here a guide to public/private key creation.

### Starting the service

1. Start your IDE (IntelliJ, Eclipse, etc);
2. Open the Resource Allocation Service (RAS) project;
3. Add common as a module in the RAS project;
4. Run the RAS application.

### Optional tools

- Postman, for REST requests.

## Contributing

For instructions about how to contribute, check out our [contributor's guide](https://github.com/fogbow/resource-allocation-service/blob/master/CONTRIBUTING.md).

## Help (work in progress)

Insert here information about how to get help when needed. Email, discord or any communication channel.
