# Resource Allocation Service

This service provides a common REST interface for manage resources in an environment with different cloud providers (e.g, Amazon Web Service, OpenStack, Microsoft Azure, etc). It also offers a way to interconnect many Fogbow instances in a federation.

## How to use

In this section the installation explanation will be facing a possible contributor. If you are interested in deployment, please take a look at [fogbow-deploy](https://github.com/fogbow/fogbow-deploy).

### Dependencies

- Java 8
- Maven
- [Common module](https://github.com/fogbow/common/), which is a dependency for most fogbow service.
- [Authentication Service](https://github.com/fogbow/authentication-service/), a fogbow service for authentication.

### Installing

First of all, create a directory to organize all fogbow modules/services then clone the required repositories.

```bash
mkdir fogbow && cd fogbow
git clone https://github.com/fogbow/common.git
git clone https://github.com/fogbow/authentication-service.git
git clone https://github.com/fogbow/resource-allocation-service.git

cd common
git checkout develop && mvn install -DskipTests

cd ../authentication-service
git checkout develop && mvn install -DskipTests

cd ../resource-allocation-service
git checkout develop && mvn install -DskipTests
```

### Configuration

This service requires some initial configuration. Most of them will have a template for help you to get started.

First of all, you need to create a directory named _private_ at `src/main/resources`, it will be holding your private settings (managed clouds, username and password for the clouds, etc.).

#### XMPP Configuration

:pushpin: Working in progress

#### private/clouds/

For each cloud that you will use, a directory should be created at `src/main/resources/private/clouds`. For each supported cloud there is a template under `src/main/resources/templates/clouds`.

You can name the directory as you like, but remember that this name will be the identifier for the cloud.

You don't need to use all the clouds, you can choose which one to use. The clouds used will depend on the ras.conf "cloud_names" field.

#### private/private.key, private/public.key

```bash
openssl genrsa -out keypair.pem 2048
openssl rsa -in keypair.pem -pubout -out public.key
openssl pkcs8 -topk8 -inform PEM -outform PEM -nocrypt -in keypair.pem -out private.key
rm keypair.pem
```

After the keys (private.key and public.key) are created, you must put them in `src/main/resources/private/` folder.

#### private/ras.conf

Check out `src/main/resources/templates/ras.conf` for a file template.

**Example:**

```conf
# Required
public_key_file_path=src/main/resources/private/public.key
# Required
private_key_file_path=src/main/resources/private/private.key

# Required
authorization_plugin_class=cloud.fogbow.ras.core.plugins.authorization.DefaultAuthorizationPlugin

# Required
cloud_names=azure,aws

open_orders_sleep_time=
spawning_orders_sleep_time=
fulfilled_orders_sleep_time=
closed_orders_sleep_period=
http_request_timeout=

ssh_common_user=

# Required
provider_id=my-provider-id
# Required
xmpp_password=pass
# Required
xmpp_server_ip=localhost
xmpp_c2c_port=
xmpp_timeout=

# Required
as_port=8081
as_url=http://localhost
```

### Starting the service

1. Start your IDE (IntelliJ, Eclipse, etc);
2. Open the Resource Allocation Service (RAS) project;
3. Add/import common and authentication service as module in the RAS project;
4. Run the AS and RAS application.

### Optional tools

- Postman, for REST requests.

## Contributing

For instructions about how to contribute, check out our [contributor's guide](https://github.com/fogbow/resource-allocation-service/blob/master/CONTRIBUTING.md).
