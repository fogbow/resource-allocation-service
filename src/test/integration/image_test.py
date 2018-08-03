import requests
import time
import random
from general_configuration_test import GeneralConfigurations
from common_methods import CommonMethods

class ImageTests:

  @classmethod
  def test_images(cls):
  	cls.test_get_all_images()
  	cls.test_get_by_id_images()

  @classmethod
  def test_get_all_images(cls):
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_image)
    if response_get_all.status_code != GeneralConfigurations.ok_status:
      print("Test get all images: Failed. Expected status %d, but got %d" % (GeneralConfigurations.ok_status, response_get_all.status_code))
      return
    images_id = response_get_all.json().keys()
    if (len(images_id) <= 0):
      print("Test get all images: Failed. Expected at least 1 image available, but got %d" % len(images_id))
      return
    print('Test get all images: Ok')

  @classmethod
  def test_get_by_id_images(cls):
    response_get_all = CommonMethods.get_all_order(GeneralConfigurations.type_image)
    images_id = list(response_get_all.json().keys())
    random_id = random.choice(images_id)
    response_get = CommonMethods.get_order_by_id(random_id, GeneralConfigurations.type_image)
    if response_get.status_code != GeneralConfigurations.ok_status:
      print("Test get all images: Failed. Expected status %d, but got %d" % (GeneralConfigurations.ok_status, response_get.status_code))
      return
    print('Test get image by id: Ok')