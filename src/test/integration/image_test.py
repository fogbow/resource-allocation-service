import requests
import time
import random
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class ImageTests:

  test_number = 0

  @classmethod
  def test_images(cls):
    data_for_local = {}
    print('-Test %d: get all local images' % cls.which_test_case())
    cls.test_get_all_images(data_for_local)
    print('-Test %d: get by id local images' % cls.which_test_case())
    cls.test_get_by_id_images(data_for_local)
    if GeneralConfigurations.remote_member:
      data_for_remote = {GeneralConfigurations.provider: GeneralConfigurations.remote_member}
      print('-Test %d: get all remote images' % cls.which_test_case())
      cls.test_get_all_images(data_for_remote)
      print('-Test %d: get by id remote images' % cls.which_test_case())
      cls.test_get_by_id_images(data_for_remote)

  @classmethod
  def which_test_case(cls):
    cls.test_number += 1
    return cls.test_number

  @classmethod
  def test_get_all_images(cls, data):
    extra_data = {}
    extra_data.update(data)
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_image)
    if response_get_all.status_code != GeneralConfigurations.ok_status:
      print("  Failed. Expected status %d, but got %d" % (GeneralConfigurations.ok_status, response_get_all.status_code))
      return
    images_id = response_get_all.json().keys()
    if (len(images_id) <= 0):
      print("  Failed. Expected at least 1 imageId available, but got %d" % len(images_id))
      return
    print('  Ok')

  @classmethod
  def test_get_by_id_images(cls, data):
    extra_data = {}
    extra_data.update(data)
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_image)
    images_id = list(response_get_all.json().keys())
    random_id = random.choice(images_id)
    response_get = CommonMethods.get_order_by_id(random_id, GeneralConfigurations.type_image)
    if response_get.status_code != GeneralConfigurations.ok_status:
      print("  Failed. Expected status %d, but got %d" % (GeneralConfigurations.ok_status, response_get.status_code))
      return
    print('  Ok')