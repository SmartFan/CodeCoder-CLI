public class Main {

    public static void main(String[] args) {
        try (HttpCoder coder = new HttpCoder()) {
            System.out.println(coder.isLogged());
            if (coder.isLogged())
                coder.submitSource("1A", "n,m,a=map(int,input().split())\n" +
                        "print(((n+a-1)//a)*((m+a-1)//a))\n");
        } catch (Exception e) {
            System.err.println(e.getMessage() + "\n" + e.getCause());
        }
    }
}
