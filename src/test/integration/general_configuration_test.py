class GeneralConfigurations:

  #
  #test configurations
  #
  #deploy configurations
  base_url = 'http://localhost:8080/'
  local_member = 'fake-localidentity-member'
  remote_member = 'fake-localidentity-member2'

  #login properties
  username = ''
  password = ''
  projectname = ''
  domain = ''

  #time to wait
  max_tries = 6
  sleep_time_secs = 15

  #quota usage in tests
  #compute confs
  max_computes = 2
  vCPU = 1
  memory = 1024
  disk = 8
  publicKey = ''
  imageId = '9b672abd-67f7-463e-b926-a87adbc80860'
  #network confs
  max_networks = 3
  cidr = "10.15.20.1/28"
  allocationMode = "dynamic"
  #volume confs
  max_volumes = 1
  volume_size = 1
  #attachment confs
  max_attachment = 1

  #
  #internal configurations
  #
  #general attributes
  json_header = {"Content-Type": "application/json"}
  status_endpoint = 'status'
  provider = 'provider'

  #http status
  ok_status = 200
  not_found_status = 404
  created_status = 201

  #order type
  type_compute = 'compute'
  type_network = 'network'
  type_volume = 'volume'
  type_attachment = 'attachment'
  type_image = 'imageId'

  #compute attributes
  computes_endpoint = 'computes/'
  quota_endpoint = 'quota/'
  allocation_endpoint = 'allocationMode/'
  tokens_endpoint = 'tokens/'
  available_quota = 'availableQuota'
  instances_quota = 'instances'
  networksId_key = 'networkIds'

  #network attributes
  networks_endpoint = 'networks/'

  #volume attributes
  volumes_endpoint = 'volumes/'

  #attachment attributes
  attachments_endpoint = 'attachments/'
  source = 'source'
  target = 'target'

  #images attributes
  images_endpoint = 'images/'