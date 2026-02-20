public class UnreachableConstructions {
    public static int unreachableConstructions(int n) {
        int result = 1;
        int i = 1;
        while (i <= n) {
            result = result * i;
            if (i > 100) {
                continue;
                i = 100500;
            } else {
                break;
                i = 100600;
            }
            result++;
        }
        return result;
    }
}

