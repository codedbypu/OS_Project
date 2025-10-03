import java.util.Scanner;

class Student {
    String name;
    int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public Student() {
        this.name = "Unknown";
        this.age = 0;
    }

    public void Info() {
        System.out.println("Name: " + name + ", Age: " + age);
    }
public class javaThreeOne {
    public static void main(String[] args) {

        Student b1 = new Student("Alice", 20);

        Student b2 = new Student();

        b1.Info();
        b2.Info();
    }
    }
}
