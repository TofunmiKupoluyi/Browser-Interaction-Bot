
public class MainExecution {
    public static void main(String[] args) {
        for (int i=0; i< args.length; i++) {System.out.println(args[i]);}
        if (args.length <= 1) {
//            ChromeExecution execution = new ChromeExecution("file:///Users/tofunmi/Documents/capstone_project/Lacuna_New/Lacuna/test_code/index.html", "mytest.txt");
//            ChromeExecution execution = new ChromeExecution("http://unicef.org");
            ChromeExecution execution = new ChromeExecution("http://cnn.com");
            execution.execute();
        } else {
            ChromeExecution execution = new ChromeExecution(args[0], args[1]);
//        ChromeExecution execution = new ChromeExecution("http://google.com");
            execution.execute();
        }
    }

}
