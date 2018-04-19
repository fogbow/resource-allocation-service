package org.fogbowcloud.manager.core.models;

public class Flavor implements Comparable<Flavor> {

    private final int MEM_VALUE_RELEVANCE = 1;
    private final int VCPU_VALUE_RELEVANCE = 1;

    private int capacity;
    private String name;
    private String id;

    /**
     * Number of cores of the CPU.
     */
    private int cpu;

    /**
     * RAM memory in MB.
     */
    private int memInMB;

    /**
     * Disk in GB.
     */
    private int disk;

    public Flavor(String name, int cpu, int memInMB, int disk, int capacity) {
        this.setName(name);
        this.setCpu(cpu);
        this.setMem(memInMB);

        if (disk != 0) {
            this.setDisk(disk);
        }

        if (capacity != 0) {
            this.setCapacity(capacity);
        }
    }

    public Flavor(String name, String id, int cpu, int memInMB, int disk) {
        this.setName(name);
        this.setCpu(cpu);
        this.setMem(memInMB);
        this.setDisk(disk);
        this.setId(id);
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCpu() {
        return cpu;
    }

    public void setCpu(int cpu) {
        this.cpu = cpu;
    }

    public int getMem() {
        return memInMB;
    }

    public void setMem(int memInMB) {
        this.memInMB = memInMB;
    }

    public int getDisk() {
        return disk;
    }

    public void setDisk(int disk) {
        this.disk = disk;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Flavor flavor = (Flavor) o;

        return id != null ? id.equals(flavor.id) : flavor.id == null;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Name: " + getName() + ", cpu: " + cpu + ", mem: " + memInMB
                + ", disk: " + disk + ", capacity: " + capacity;
    }

    @Override
    public int compareTo(Flavor flavor) {
        double oneRelevance = calculateRelevance(this, flavor);
        double twoRelevance = calculateRelevance(this, flavor);

        if (oneRelevance != twoRelevance) {
            return Double.compare(oneRelevance, twoRelevance);
        }

        int oneDisk = this.getDisk();
        int twoDisk = flavor.getDisk();
        return Integer.compare(oneDisk, twoDisk);
    }

    private double calculateRelevance(Flavor flavorOne, Flavor flavorTwo) {
        int cpuOne = flavorOne.getCpu();
        int cpuTwo = flavorTwo.getCpu();
        int memOne = flavorOne.getMem();
        int memTwo = flavorTwo.getMem();

        return ((cpuOne / cpuTwo) / VCPU_VALUE_RELEVANCE)
                + ((memOne / memTwo) / MEM_VALUE_RELEVANCE);
    }
}
