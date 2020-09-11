sudo adduser test --no-create-home --disabled-password --gecos ""
sudo echo "oneuser:oneb" | chpasswd
interface=$(ip l | grep -i down | awk '{print $2}' | sed 's/://')
sudo dhclient ${interface}
sudo ifconfig ${interface} up
