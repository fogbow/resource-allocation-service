class GeneralConfigurations:

  #deploy configurations
  base_url = 'http://localhost:8080/'
  local_member = 'fake-localidentity-member'
  remote_member = ''

  #general attributes
  json_header = {"Content-Type": "application/json"}
  status_endpoint = 'status'
  max_tries = 6
  sleep_time_secs = 15
  providingMember = 'providingMember'

  #http status
  ok_status = 200
  not_found_status = 404
  created_status = 201

  #order type
  type_compute = 'compute'
  type_network = 'network'
  type_volume = 'volume'
  type_attachment = 'attachment'

  #compute attributes
  computes_endpoint = 'computes/'
  quota_endpoint = 'quota/'
  allocation_endpoint = 'allocation/'
  available_quota = 'availableQuota'
  instances_quota = 'instances'
  max_computes = 2
  #compute datas
  vCPU = 1
  memory = 1024
  disk = 8
  publicKey = ''
  imageId = '9b672abd-67f7-463e-b926-a87adbc80860'
  networksId_key = 'networksId'

  #network attributes
  networks_endpoint = 'networks/'
  address = "10.15.20.1/28"
  allocation = "dynamic"
  max_networks = 3

  #volume attributes
  volumes_endpoint = 'volumes/'
  volume_size = 1
  max_volumes = 1

  #attachment attributes
  attachments_endpoint = 'attachments/'
  source = 'source'
  target = 'target'
  max_attachment = 1