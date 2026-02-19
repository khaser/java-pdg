public class WhileLoopExample {
    public static int whileLoopExample(int n) {
        int result = 1;
        int i = 1;
        while (i <= n) {
            result = result * i;
            i = i + 1;
        }
        return result;
    }
}

