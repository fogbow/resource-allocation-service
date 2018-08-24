import requests
import time
from compute_test import ComputeTests
from network_test import NetworkTests
from volume_test import VolumeTests
from attachment_test import AttachmentTests
from image_test import ImageTests


print('###### Starting tests ######')
print()
print('=== Starting Compute tests ===')
ComputeTests.test_computes()
print('=== Compute tests are over ===')
print()
print('=== Starting Network tests ===')
NetworkTests.test_networks()
print('=== Network tests are over ===')
print()
print('=== Starting Volume tests ===')
VolumeTests.test_volumes()
print('=== Volume tests are over ===')
print()
print('=== Starting Attachment tests ===')
AttachmentTests.test_attachments()
print('=== Attachment tests are over ===')
print()
print('=== Starting Image tests ===')
ImageTests.test_images()
print('=== Image tests are over ===')
print()
print('###### Tests are over ######')