class Student {
    String name;
    int age;

    // Constructor แบบรับค่า
    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Constructor แบบไม่รับค่า (default)
    public Student() {
        this.name = "Unknown";
        this.age = 0;
    }

    public void showInfo() {
        System.out.println("Name: " + name + ", Age: " + age);
    }
    public class waa {
    public static void main(String[] args) {
        // ใช้ constructor แบบรับค่า
        Student s1 = new Student("Alice", 20);

        // ใช้ constructor แบบไม่รับค่า
        Student s2 = new Student();

        s1.showInfo();
        s2.showInfo();
    }}
}
