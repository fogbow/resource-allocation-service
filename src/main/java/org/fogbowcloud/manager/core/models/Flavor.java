package org.fogbowcloud.manager.core.models;

import java.util.Comparator;

public class Flavor implements Comparator<Flavor> {

    private final int MEM_VALUE_RELEVANCE = 1;
    private final int VCPU_VALUE_RELEVANCE = 1;

    private Integer capacity;
    private String name;
    private String id;

    /**
     * Number of cores of the CPU.
     */
    private String cpu;

    /**
     * RAM memory in MB.
     */
    private String memInMB;

    /**
     * Disk in GB.
     */
    private String disk;

    public Flavor(String name, String cpu, String memInMB, String disk) {
        this.setName(name);
        this.setCpu(cpu);
        this.setMem(memInMB);
        this.setDisk(disk);
    }

    public Flavor(String name, String cpu, String memInMB, Integer capacity) {
        this.setName(name);
        this.setCpu(cpu);
        this.setMem(memInMB);
        this.setCapacity(capacity);
    }

    public Flavor(String name, String id, String cpu, String memInMB, String disk) {
        this.setName(name);
        this.setCpu(cpu);
        this.setMem(memInMB);
        this.setDisk(disk);
        this.setId(id);
    }

    public Flavor(String name, String cpu, String memInMB, String disk, Integer capacity) {
        this.setCapacity(capacity);
        this.setName(name);
        this.setCpu(cpu);
        this.setMem(memInMB);
        this.setDisk(disk);
    }

    public String getDisk() {
        return disk;
    }

    public void setDisk(String disk) {
        this.disk = disk;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getMem() {
        return memInMB;
    }

    public void setMem(String mem) {
        this.memInMB = mem;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
    public int compare(Flavor flavorOne, Flavor flavorTwo) {
        try {
            Double oneRelevance = calculateRelevance(flavorOne, flavorTwo);
            Double twoRelevance = calculateRelevance(flavorTwo, flavorOne);
            if (oneRelevance.doubleValue() != twoRelevance.doubleValue()) {
                return oneRelevance.compareTo(twoRelevance);
            }
            Double oneDisk = Double.parseDouble(flavorOne.getDisk());
            Double twoDisk = Double.parseDouble(flavorTwo.getDisk());
            return oneDisk.compareTo(twoDisk);
        } catch (Exception e) {
            return 0;
        }
    }

    public double calculateRelevance(Flavor flavorOne, Flavor flavorTwo) {
        double cpuOne = Double.parseDouble(flavorOne.getCpu());
        double cpuTwo = Double.parseDouble(flavorTwo.getCpu());
        double memOne = Double.parseDouble(flavorOne.getMem());
        double memTwo = Double.parseDouble(flavorTwo.getMem());

        return ((cpuOne / cpuTwo) * 1 / VCPU_VALUE_RELEVANCE)
                + ((memOne / memTwo) * 1 / MEM_VALUE_RELEVANCE);
    }
}
