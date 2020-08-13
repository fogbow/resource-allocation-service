# Resource Allocation Service

This service provides a common REST interface for manage resources in an environment with different cloud providers (e.g, Amazon Web Service, OpenStack, Microsoft Azure, etc). The service offers a way to interconnect many Fogbow instances in a federation.

## How to use (work in progress)

For begin, you need to clone this repository and other fogbow modules. - The [common module](!) is a dependency for every fogbow service.

```bash
mkdir fogbow && cd fogbow
git clone https://github.com/fogbow/resource-allocation-service.git
cd resource-allocaton-service && mvn install -DskipTests

git clone https://github.com/fogbow/common.git
cd common && mvn install -DskipTests
```

- Fogbow [common](https://www.github.com/fogbow/common) module

## Contributing

For instructions about how to contribute, check out our [contributor's guide](https://github.com/fogbow/resource-allocation-service/blob/master/CONTRIBUTING.md).

## Help (work in progress)

Insert here information about how to get help when needed. Email, discord or any communication channel.
