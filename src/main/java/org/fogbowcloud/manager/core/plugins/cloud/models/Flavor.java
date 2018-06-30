package org.fogbowcloud.manager.core.plugins.cloud.models;

public class Flavor implements Comparable<Flavor> {
    
    /** the lower value, the greater relevance. */
    private final int VCPU_VALUE_RELEVANCE = 1;
    private final int MEM_VALUE_RELEVANCE = 1;

    private String name;
    private String id;

    /** Number of cores of the CPU. */
    private int cpu;

    /** RAM memory in MB. */
    private int ram;

    /** Disk in GB. */
    private int disk;

    public Flavor(String name, String id, int cpu, int ram, int disk) {
        this.setName(name);
        this.setCpu(cpu);
        this.setRam(ram);
        this.setDisk(disk);
        this.setId(id);
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

    public int getRam() {
        return ram;
    }

    public void setRam(int ram) {
        this.ram = ram;
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
        return "Name: " + getName() + ", cpu: " + cpu + ", mem: " + ram + ", disk: " + disk;
    }

    @Override
    public int compareTo(Flavor flavor) {
        double oneRelevance = calculateRelevance(this, flavor);
        double twoRelevance = calculateRelevance(flavor, this);

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
        int memOne = flavorOne.getRam();
        int memTwo = flavorTwo.getRam();

        return ((cpuOne / cpuTwo) / VCPU_VALUE_RELEVANCE) + ((memOne / memTwo) / MEM_VALUE_RELEVANCE);
    }
}
