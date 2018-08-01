import requests
import time
from compute_test import ComputeTests
from network_test import NetworkTests

print('###### Starting tests ######')
print()
#print('### Compute tests ###')
#ComputeTests.test_computes()
#print('### Compute tests are over ###')
#print()
print('### Network tests ###')
NetworkTests.test_networks()
print('### Network tests are over ###')