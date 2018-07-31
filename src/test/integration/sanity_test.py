import requests
import time
from compute_test import ComputeTests
from network_test import NetworkTests

print('###### Starting tests ######')
print()
#print('### Compute tests ###')
#We can change methods to be static in ComputeTests, this way we don't need to instatiate this object
#compute_test = ComputeTests()
print()
print('### Network tests ###')
NetworkTests.test_networks()