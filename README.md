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

Insert here a guide about how to configurate the required files (resources/private directory and ras.conf)

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
