import java.util.Scanner;

class Student {
    String name;
    int age;
}

public class java2 {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // รับค่า input
        String name = sc.nextLine();
        int age = sc.nextInt();

        // สร้าง instance ของ Student
        Student s = new Student();
        s.name = name;
        s.age = age;

        // แสดงผล
        System.out.println("Student: " + s.name + ", Age: " + s.age);
    }
}