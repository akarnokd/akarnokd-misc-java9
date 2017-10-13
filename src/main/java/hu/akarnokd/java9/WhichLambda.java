package hu.akarnokd.java9;

public class WhichLambda {
    public static void main(String[] args) {
        Runnable r = () -> { System.out.println("Hello World!"); };
        System.out.println("r plain = " + r);
        System.out.println("r class = " + r.getClass());
        System.out.println("r enclosing-method = " + r.getClass().getEnclosingMethod());

        p(() -> { System.out.println("Again!"); });

        p(System.out::println);

        p(new Runnable() {

            @Override
            public void run() {
                System.out.println("And again");
            }
        });
    }

    static void p(Runnable r) {
        System.out.println("r plain = " + r);
        System.out.println("r class = " + r.getClass());
        System.out.println("r enclosing-method = " + r.getClass().getEnclosingMethod());
    }
}
