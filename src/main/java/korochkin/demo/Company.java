package korochkin.demo;

public class Company implements Comparable<Company> {
    private String name;
    private String inn;
    private String countOfEmps;

    public Company() {
    }

    public Company(String name, String inn, String countOfEmps) {
        this.name = name;
        this.inn = inn;
        this.countOfEmps = countOfEmps;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInn() {
        return inn;
    }

    public void setInn(String inn) {
        this.inn = inn;
    }

    public String getCountOfEmps() {
        return countOfEmps;
    }

    public void setCountOfEmps(String countOfEmps) {
        this.countOfEmps = countOfEmps;
    }

    @Override
    public String toString() {
        return "Company{" +
                "name='" + name + '\'' +
                ", inn='" + inn + '\'' +
                ", countOfEmps='" + countOfEmps + '\'' +
                '}';
    }

    @Override
    public int compareTo(Company o) {
        return Integer.compare(Integer.parseInt(o.getCountOfEmps()), Integer.parseInt(this.getCountOfEmps()));
    }
}
