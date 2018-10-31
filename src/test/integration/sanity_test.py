import requests
import time
from compute_test import ComputeTests
from network_test import NetworkTests
from volume_test import VolumeTests
from attachment_test import AttachmentTests
from image_test import ImageTests
from general_configuration_test import GeneralConfigurations
import sys

# Generate token required for requests
def post_token():
  url_tokens = GeneralConfigurations.base_url + GeneralConfigurations.tokens_endpoint
  auth_data = {'username' : GeneralConfigurations.username, 'password' : GeneralConfigurations.password, \
    'projectname' : GeneralConfigurations.projectname, 'domain' : GeneralConfigurations.domain}
  response = requests.post(url_tokens, json = auth_data)
  if response.status_code != GeneralConfigurations.created_status:
    return ''
  return response.text

token = post_token()
if not token:
  sys.exit('Error while trying to generate token')

GeneralConfigurations.json_header["federationTokenValue"] = token
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